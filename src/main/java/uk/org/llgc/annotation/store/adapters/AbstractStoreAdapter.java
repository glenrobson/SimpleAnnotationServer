package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import java.nio.charset.Charset;

import com.github.jsonldjava.utils.JsonUtils;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public abstract class AbstractStoreAdapter implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(AbstractStoreAdapter.class.getName()); 
	protected SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected AnnotationUtils _annoUtils = null;
	public static final String FULL_TEXT_PROPERTY = "http://dev.llgc.org.uk/sas/full_text";

	public void init(final AnnotationUtils pAnnoUtils) {
		_annoUtils = pAnnoUtils;
	}

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException, IDConflictException {
		List<Model> tModel = new ArrayList<Model>();
		for (Map<String,Object> tAnno : pJson) {
			tModel.add(this.addAnnotation(tAnno));
		}
		return tModel;
	}

	public Model addAnnotation(final Map<String,Object> pJson) throws IOException, IDConflictException {
		if (this.getNamedModel((String)pJson.get("@id")) != null) {
			_logger.debug("Found existing annotation with id " + pJson.get("@id").toString());
			pJson.put("@id",(String)pJson.get("@id") + "1");
			if (((String)pJson.get("@id")).length() > 400) {
				throw new IDConflictException("Tried multiple times to make this id unique but have failed " + (String)pJson.get("@id"));
			}
			return this.addAnnotation(pJson);
		} else {
			this.expandTarget(pJson);
			_logger.debug("No conflicting id " + pJson.get("@id"));
			if (this.isMissingWithin(pJson)) {
				// missing within so check to see if the canvas maps to a manifest
				String tCanvasId = "";
				if (pJson.get("on") instanceof Map) {
					tCanvasId = (String)((Map<String,Object>)pJson.get("on")).get("full");
				} else {
					String tURL = (String)pJson.get("on");
					tCanvasId = tURL.split("#")[0];
				}
				List<String> tManifestURI = getManifestForCanvas(tCanvasId);
				if (tManifestURI != null && !tManifestURI.isEmpty()) {
					this.addWithin(pJson, tManifestURI);
				}
			}

			this.addMetadata(pJson);
			return addAnnotationSafe(pJson);
		}
	}

	public void expandTarget(final Map<String,Object> pJson) {
		String tURI = null;
		Map<String,Object> tSpecificResource = null;
		if (pJson.get("on") instanceof String) {
			tURI = (String)pJson.get("on");
			tSpecificResource = new HashMap<String,Object>();
			pJson.put("on", tSpecificResource);
		} else if (pJson.get("on") instanceof Map) {
			tSpecificResource = (Map<String,Object>)pJson.get("on");

			if (tSpecificResource.get("@id") == null || ((String)tSpecificResource.get("@id")).indexOf("#") == -1) {
				return; // No id to split or no fragement
			}
			if (tSpecificResource.get("selector") != null) {
				return; // already have a selector
			}
			tURI = (String)tSpecificResource.get("@id");
			tSpecificResource.remove("@id");
		} else {
			return; // could be a list so not processing
		}
		int tIndexOfHash = tURI.indexOf("#");
		tSpecificResource.put("@type","oa:SpecificResource");
		Map<String,Object> tFragement = new HashMap<String,Object>();
		tSpecificResource.put("selector", tFragement);
		tSpecificResource.put("full", tURI.substring(0, tIndexOfHash));

		tFragement.put("@type", "oa:FragmentSelector");
		tFragement.put("value", tURI.substring(tIndexOfHash + 1));
	}
	
	protected boolean isMissingWithin(final Map<String,Object> pAnno) {
		if (pAnno.get("on") != null) {
			if (pAnno.get("on") instanceof String) {
				return true;
			}
			if (pAnno.get("on") instanceof Map) {
				return ((Map<String,Object>)pAnno.get("on")).get("within") == null;
			}
		}
		return true;
	}

	protected void addWithin(final Map<String,Object> pAnno, final String pManifestURI) {
		List<String> tParents = new ArrayList<String>();
		tParents.add(pManifestURI);
		this.addWithin(pAnno, tParents);
	}
	protected void addWithin(final Map<String,Object> pAnno, final List<String> pManifestURI) {
		if (pAnno.get("on") instanceof String) {
			String[] tOnStr = ((String)pAnno.get("on")).split("#");

			Map<String,Object> tOnObj = new HashMap<String,Object>();
			tOnObj.put("@type", "oa:SpecificResource");
			tOnObj.put("full", tOnStr[0]);

			Map<String,Object> tSelector = new HashMap<String,Object>();
			tOnObj.put("selector", tSelector);
			tSelector.put("@type", "oa:FragmentSelector");
			tSelector.put("value", tOnStr[1]);

			pAnno.put("on", tOnObj);
		}
		Object tWithin = null;
		if (pManifestURI.size() == 1) {
			tWithin = pManifestURI.get(0);
		} else {
			tWithin = pManifestURI;
		}
		((Map<String, Object>)pAnno.get("on")).put("within", tWithin);
	}

	public void addMetadata(final Map<String,Object> pJson) {
		// Add create date if it doesn't already have one
		if (pJson.get("dcterms:created") == null && pJson.get("created") == null && pJson.get("http://purl.org/dc/terms/created") == null) {
			pJson.put(DCTerms.created.getURI(), _dateFormatter.format(new Date()));
		}
		if (pJson.get("resource") != null) {
			String tRepalceStr = "<[ /]*[a-zA-Z0-9 ]*[ /]*>";
			if (pJson.get("resource") instanceof List) {
				for (Map<String,Object> tResource : (List<Map<String,Object>>)pJson.get("resource")) {
					if (tResource.get("chars") != null) {
						// add a field which contains the text with all of the html markup removed
						String tCleaned = ((String)tResource.get("chars")).replaceAll(tRepalceStr,"");
						tResource.put(FULL_TEXT_PROPERTY,tCleaned);
					}	
				}
			} else {
				if (((Map<String,Object>)pJson.get("resource")).get("chars") != null) {
					String tCleaned = ((String)((Map<String,Object>)pJson.get("resource")).get("chars")).replaceAll(tRepalceStr,"");
					((Map<String,Object>)pJson.get("resource")).put(FULL_TEXT_PROPERTY,tCleaned);
				} else {
					_logger.debug("Not adding full text as no chars in resource");
				}
			}
		} else {
			_logger.debug("Not adding full text as no resource");
		}
	}

	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException {
		_logger.debug("processing " + JsonUtils.toPrettyString(pJson));
		// add modified date and retrieve created date
		String tAnnoId = (String)pJson.get("@id");
		_logger.debug("ID " + tAnnoId);
		Model tStoredAnno = this.getNamedModel(tAnnoId);
		Resource tAnnoRes = tStoredAnno.getResource(tAnnoId); 
		Statement tCreatedSt = tAnnoRes.getProperty(DCTerms.created);
		if (tCreatedSt != null) {
			String tCreatedDate = tCreatedSt.getString();
			pJson.put(DCTerms.created.getURI(), tCreatedDate);
		}	
		pJson.put(DCTerms.modified.getURI(), _dateFormatter.format(new Date()));
		_logger.debug("Modified annotation " + JsonUtils.toPrettyString(pJson));
		deleteAnnotation(tAnnoId);

		if (this.isMissingWithin(pJson)) {
			// missing within so check to see if the canvas maps to a manifest
			String tCanvasId = "";
			if (pJson.get("on") instanceof Map) {
				tCanvasId = (String)((Map<String,Object>)pJson.get("on")).get("full");
			} else {
				String tURL = (String)pJson.get("on");
				tCanvasId = tURL.split("#")[0];
			}
			List<String> tManifestURI = getManifestForCanvas(tCanvasId);
			if (tManifestURI != null && !tManifestURI.isEmpty()) {
				this.addWithin(pJson, tManifestURI);
			}
		}
		this.addMetadata(pJson);

		return addAnnotationSafe(pJson);
	}

	public String indexManifest(Map<String,Object> pManifest) throws IOException {
		String tShortId = this.createShortId((String)pManifest.get("@id"));
		return this.indexManifest(tShortId, pManifest);
	}

	protected String indexManifest(final String pShortId, Map<String,Object> pManifest) throws IOException {	
		String tManifestId = (String)pManifest.get("@id");

		Map<String,Object> tExisting = this.getManifest(pShortId);
		if (tExisting != null) {
			if (((String)tExisting.get("@id")).equals((String)pManifest.get("@id"))) {
				return (String)tExisting.get("short_id"); // manifest already indexed
			} else {	
				// there already exists a document with this id but its a different manifest so try and make id unique
				return indexManifest(pShortId + "1", pManifest);
			}	
		}
		pManifest.put("short_id",pShortId);//may need to make this a uri...

		Map<String,Object> tShortIdContext = new HashMap<String,Object>();
		tShortIdContext.put("@id","http://purl.org/dc/elements/1.1/identifier");
		Map<String,Object> tExtraContext = new HashMap<String,Object>();
		tExtraContext.put("short_id", tShortIdContext);
		if (pManifest.get("@context") instanceof List) {
			List<Map<String,Object>> tListContext = (List<Map<String,Object>>)pManifest.get("@context");
			tListContext.add(tExtraContext);
		} else {
			String tContext = (String)pManifest.get("@context");
			List<Object> tListContext = new ArrayList<Object>();
			tListContext.add(tContext);
			tListContext.add(tExtraContext);

			pManifest.put("@context", tListContext);
		}
		
		return this.indexManifestNoCheck(pShortId, pManifest);
	}

	protected String createShortId(final String pLongId) {
		if (pLongId.endsWith("manifest.json")) {
			String[] tURI = pLongId.split("/");
			return tURI[tURI.length - 2];
		} else {
			return pLongId.replaceAll("\\D+","");
		}
	}

	public abstract List<String> getManifestForCanvas(final String pCanvasId) throws IOException;
	public abstract Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException;
	public abstract Map<String, Object> search(final SearchQuery pQuery) throws IOException;
	protected abstract String indexManifestNoCheck(final String pShortID, final Map<String,Object> pManifest) throws IOException;
	public abstract List<String> getManifests() throws IOException;
	public abstract String getManifestId(final String pShortId) throws IOException;
	public abstract Map<String,Object> getManifest(final String pShortId) throws IOException;

	public Model getAnnotation(final String pId) throws IOException {
		return getNamedModel(pId);
	}

	protected abstract Model getNamedModel(final String pName) throws IOException;

	protected void begin(final ReadWrite pWrite) {
	}
	protected void end() {
	}

	protected Model convertAnnoToModel(final Map<String,Object> pJson) throws IOException {
		return _annoUtils.convertAnnoToModel(pJson);
	}

}
