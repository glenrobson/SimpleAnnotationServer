package uk.org.llgc.annotation.store.encoders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.jdom2.JDOMException;

import java.io.StringReader;
import java.io.IOException;

import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;

public class BookOfPeaceEncoder implements Encoder {
	protected static Logger _logger = LogManager.getLogger(BookOfPeaceEncoder.class.getName()); 

	protected Map<String,String> _props = null;
	protected Map<String,Object> _context = null;
	public BookOfPeaceEncoder() {
		StringBuffer tContextString = new StringBuffer("{");
		tContextString.append("    \"heldRank\": {");
		tContextString.append("      \"@id\":\"http://rdf.muninn-project.org/ontologies/military#heldRank\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"foaf\":\"http://xmlns.com/foaf/0.1/\",");
		tContextString.append("    \"bor\":\"http://data.llgc.org.uk/bor/def#\",");
		tContextString.append("    \"label\": {");
		tContextString.append("      \"@id\":\"http://www.w3.org/2000/01/rdf-schema#label\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"name\": {");
		tContextString.append("      \"@id\":\"http://xmlns.com/foaf/0.1/name\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"servedInUnit\":{");
		tContextString.append("      \"@id\":\"http://data.llgc.org.uk/bor/def#servedInUnit\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"servedOnShip\":{");
		tContextString.append("      \"@id\":\"http://data.llgc.org.uk/bor/def#servedOnShip\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"oa\":\"http://www.w3.org/ns/oa#\",");
		tContextString.append("    \"awarded\":{");
		tContextString.append("      \"@id\":\"http://data.llgc.org.uk/waw/def#awarded\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    },");
		tContextString.append("    \"based_near\":{");
		tContextString.append("      \"@id\":\"http://xmlns.com/foaf/0.1/based_near\",");
		tContextString.append("      \"@language\":\"en\"");
		tContextString.append("    }");
		tContextString.append("  }");
		try {
			_context = (Map<String,Object>)JsonUtils.fromString(tContextString.toString());
		} catch (IOException tError) {
			_logger.error("Couldn't parse context due to " + tError);
			tError.printStackTrace();
		}
	}

	public void init(final Map<String,String> pProps) {
		_props = pProps;
	}

	/*
	 * {
	 "@context","see above"
	 "@type":"oa:SemanticTag",
	 "foaf:primaryTopic":{
	 "@type":"foaf:Person",
	 "heldRank":"Cmdr",
	 "name":"John Jones",
	 "servedInUnit":"R.N.",
	 "servedOnShip":"HMS Invincible",
	 "awarded":"D.S.C",
	 "based_near":"place"
	 }
	 }
	 */

	/**
	 * Encode RDFa as RDF
	 */
	public void encode(final Map<String, Object> pModel) {
		_logger.debug("ENCODE");
		Object tBodyThing = pModel.get("resource");
		Map<String, Object> tBody = null;
		if (tBodyThing instanceof List) {
			tBody = (Map<String,Object>)((List)tBodyThing).get(0);
		} else {
			tBody = (Map<String,Object>)tBodyThing;
		}	
		String tChars = "<root>" + ((String)tBody.get("chars")).replaceAll("\\\\\"","\"").replaceAll("\\&[a-zA-Z0-9]*;","") + "</root>";
		// Need to parse into XML 
		SAXBuilder tBuilder = new SAXBuilder();
		try {
			_logger.debug("Chars" + tChars);
			Document tContent = tBuilder.build(new StringReader(tChars));
			Map<String,Object> tPerson = new HashMap<String, Object>();
			this.saveField(tPerson, "heldRank", this.getField(tContent, "//span[@property='ns:rank']"));
			this.saveField(tPerson, "name", this.getField(tContent, "//span[@property='ns:name']"));
			this.saveField(tPerson, "based_near", this.getField(tContent, "//span[@property='ns:place']"));
			this.saveField(tPerson, "servedInUnit", this.getField(tContent, "//span[@property='ns:unit']"));
			this.saveField(tPerson, "servedOnShip", this.getField(tContent, "//span[@property='ns:ship']"));
			this.saveField(tPerson, "awarded", this.getField(tContent, "//span[@property='ns:medal']"));
			if (!tPerson.isEmpty()) {
				tPerson.put("@type","foaf:Person");
				tBody.remove("@type");
				tBody.remove("format");
				tBody.remove("@id");
				tBody.remove("chars");
				tBody.put("@context", _context);
				tBody.put("@type","oa:SemanticTag");
				tBody.put("foaf:primaryTopic", tPerson);
			}	
			String tHeadingValue = this.getField(tContent, "//span[@property='ns:heading']");
			if (tHeadingValue != null) {
				Map<String,Object> tHeading = new HashMap<String,Object>();
				if (tBody.get("foaf:primaryTopic") != null) {
					// Annotation already has a person
					Map<String,Object> tPersonLink = (Map<String,Object>)tBody.get("foaf:primaryTopic");
					List tTopics = new ArrayList(); // turn value into an array
					tTopics.add(tPersonLink);
					tTopics.add(tHeading);
					tBody.put("foaf:primaryTopic", tTopics);
				} else {
					tBody.remove("@type");
					tBody.remove("format");
					tBody.remove("@id");
					tBody.remove("chars");
					tBody.put("@context", _context);
					tBody.put("@type","oa:SemanticTag");
					tBody.put("foaf:primaryTopic", tHeading);
				}
				tHeading.put("@type","bor:Section");
				tHeading.put("label", tHeadingValue);
			}
		} catch (JDOMException tExcpt) {
			_logger.error("Couldn't parseInput:");
			_logger.error(tChars);
			tExcpt.printStackTrace();
		} catch (IOException tExcpt) {	
			_logger.error("Couldn't parseInput:");
			_logger.error(tChars);
			tExcpt.printStackTrace();
		}
	}

	protected void saveField(final Map<String,Object> pBody, final String pKey, final String pValue) {
		if (pValue != null) {
			pBody.put(pKey, pValue);
		}
	}

	protected String getField(final Document pDoc, final String pPath) throws JDOMException {
		List<Element> tList = (List<Element>)XPath.selectNodes(pDoc, pPath);
		StringBuffer tBuffer = new StringBuffer();
		for (Element tEl : tList) {
			if (tEl.getText().trim().length() > 0) {
				tBuffer.append(tEl.getText());
				tBuffer.append(" ");
			}
		}
		String tValue = tBuffer.toString().trim();
		if (tValue.length() > 0) {
			return tValue;
		} else {
			return null;
		}
	}

	/**
	 * Decode RDF into RDFa/HTML for mirador
	 */
	public void decode(final Map<String, Object> pModel) {
		/*try {
	 _logger.debug(com.github.jsonldjava.utils.JsonUtils.toPrettyString(pModel));	
	 } catch (Exception tExcpt) { 
	 	_logger.error("Couldn't print json " + tExcpt);
	 }*/

		Object tBodyThing = pModel.get("resource");
		Map<String, Object> tBody = null;
		if (tBodyThing instanceof List) {
			tBody = (Map<String,Object>)((List)tBodyThing).get(0);
		} else {
			tBody = (Map<String,Object>)tBodyThing;
		}

		if (tBody.get("foaf:primaryTopic") != null) {
			List<Map<String,Object>> tTopics = null;
			if (tBody.get("foaf:primaryTopic") instanceof List) {
				tTopics = (List<Map<String,Object>>)tBody.get("foaf:primaryTopic");
			} else {
				Map<String, Object> tTopic = (Map<String,Object>)tBody.get("foaf:primaryTopic");
				tTopics = new ArrayList<Map<String,Object>>();
				tTopics.add(tTopic);
			}

			StringBuffer tHTML = new StringBuffer("<p>");
			for (Map<String,Object> tPerson : tTopics) {
				if (tPerson.get("label") != null) {
					tHTML.append("<span property=\"ns:heading\" class=\"heading\">");
					tHTML.append(((Map<String, Object>)tPerson.get("label")).get("@value"));
					tHTML.append("</span> ");
				}

				if (tPerson.get("http://rdf.muninn-project.org/ontologies/military#heldRank") != null) {
					tHTML.append("<span property=\"ns:rank\" class=\"rank\">");
					tHTML.append(this.getContent(tPerson.get("http://rdf.muninn-project.org/ontologies/military#heldRank")));
					tHTML.append("</span> ");
				}

				if (tPerson.get("foaf:name") != null) {
					tHTML.append("<span property=\"ns:name\" class=\"name\">");
					tHTML.append(this.getContent(tPerson.get("foaf:name")));
					tHTML.append("</span> ");
				}

				if (tPerson.get("foaf:based_near") != null) {
					tHTML.append("<span property=\"ns:place\" class=\"place\">");
					tHTML.append(this.getContent(tPerson.get("foaf:based_near")));
					tHTML.append("</span> ");
				}

				if (tPerson.get("http://data.llgc.org.uk/bor/def#servedInUnit") != null) {
					tHTML.append("<span property=\"ns:unit\" class=\"unit\">");
					tHTML.append(this.getContent(tPerson.get("http://data.llgc.org.uk/bor/def#servedInUnit")));
					tHTML.append("</span> ");
				}

				if (tPerson.get("http://data.llgc.org.uk/bor/def#servedOnShip") != null) {
					tHTML.append("<span property=\"ns:ship\" class=\"ship\">");
					tHTML.append(this.getContent(tPerson.get("http://data.llgc.org.uk/bor/def#servedOnShip")));
					tHTML.append("</span> ");
				}

				if (tPerson.get("http://data.llgc.org.uk/waw/def#awarded") != null) {
					tHTML.append("<span property=\"ns:medal\" class=\"medal\">");
					tHTML.append(this.getContent(tPerson.get("http://data.llgc.org.uk/waw/def#awarded")));
					tHTML.append("</span> ");
				}

			}	
			tBody.remove("foaf:primaryTopic");
			tBody.put("@type","dctypes:Text");
			tBody.put("format","text/html");
			tHTML.append("</p>");
			tBody.put("chars",tHTML.toString());
		}	
	}	

	protected String getContent(Object pContent) {
		if (pContent == null) {
			return "";
		} else if (pContent instanceof Map) {
			return this.getContent((Map<String,Object>)pContent);
		} else {
			return this.getContent((List<Map<String,Object>>)pContent);
		}
	}

	protected String getContent(Map<String, Object> pContent) {
		return (String)pContent.get("@value");
	}	

	protected String getContent(List<Map<String, Object>> pContent) {
		StringBuffer tContentStr = new StringBuffer();
		// join list of ranks together 
		for (Map<String,Object> tContentItem: pContent) {
			tContentStr.append((String)tContentItem.get("@value") + " ");
		}

		return tContentStr.toString();
	}
}
