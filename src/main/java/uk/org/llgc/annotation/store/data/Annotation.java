package uk.org.llgc.annotation.store.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

import com.hp.hpl.jena.vocabulary.DCTerms;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

public class Annotation {
    public static final String FULL_TEXT_PROPERTY = "http://dev.llgc.org.uk/sas/full_text";
    protected static Logger _logger = LogManager.getLogger(Annotation.class.getName());
	protected SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected Map<String,Object> _annotation = null;

    public Annotation(final Map<String,Object> pAnno) {
        _annotation = pAnno;
        this.expandTarget();
        addMetadata();
    }

    public String getId() {
        return (String)_annotation.get("@id");
    }

    public void setId(final String pId) {
        _annotation.put("@id", pId);
    }

    public void setCreated(final String pDate) {
        _annotation.put(DCTerms.created.getURI(), pDate);
    }

    public void setCreated(final Date pDate) {
        _annotation.put(DCTerms.created.getURI(), _dateFormatter.format(pDate));
    }

    public void updateModified() {
		_annotation.put(DCTerms.modified.getURI(), _dateFormatter.format(new Date()));
    }

    /**
     * Check if the load annotaiton is valid
     */
    public String checkValid() throws MalformedAnnotation {
        List<Map<String, Object>> tOnList = this.getOn();
        StringBuffer tOutput = new StringBuffer();
        boolean tMalformed = false;
        for (Map<String, Object> tOn : tOnList) {
            // Look to see if selector value is an array. It should be a string.
            if (tOn.get("selector") != null
                    && tOn.get("selector") instanceof Map
                    && ((Map<String,Object>)tOn.get("selector")).get("value") != null
                    && ((Map<String,Object>)tOn.get("selector")).get("value") instanceof List) {
                tOutput.append("Annotation " + this.getId() + " has an array in the on/selector/value and this should be a string");
                tMalformed = true;
            }
        }
        if (tMalformed) {
            throw new MalformedAnnotation(tOutput.toString());
        } else {
            // in case of non critical errors:
            return tOutput.toString();
        }
    }

    public List<Map<String,Object>> getMissingWithin() {
        List<Map<String,Object>> tOnList = this.getOn();
        List<Map<String,Object>> tMissingOnList = new ArrayList<Map<String,Object>>();
        for (Map<String, Object> tOn : tOnList) {
            if (tOn.get("within") == null) {
                tMissingOnList.add(tOn);
			}
		}
		return tMissingOnList;
	}

    public List<Map<String,Object>> getOn() {
        if (_annotation.get("on") instanceof List) {
            return (List<Map<String,Object>>)_annotation.get("on");
        } else if (_annotation.get("on") instanceof Map) {
            return map2list((Map<String,Object>)_annotation.get("on"));
        }

        return null; // invalid annotation
    }

    protected void addMetadata() {
		// Add create date if it doesn't already have one
		if (_annotation.get("dcterms:created") == null && _annotation.get("created") == null && _annotation.get("http://purl.org/dc/terms/created") == null) {
			_annotation.put(DCTerms.created.getURI(), _dateFormatter.format(new Date()));
		}
		if (_annotation.get("resource") != null) {
			String tRepalceStr = "<[ /]*[a-zA-Z0-9 ]*[ /]*>";
			if (_annotation.get("resource") instanceof List) {
				for (Map<String,Object> tResource : (List<Map<String,Object>>)_annotation.get("resource")) {
					if (tResource.get("chars") != null) {
						// add a field which contains the text with all of the html markup removed
						String tCleaned = ((String)tResource.get("chars")).replaceAll(tRepalceStr,"");
						tResource.put(FULL_TEXT_PROPERTY,tCleaned);
					}
				}
			} else {
				if (((Map<String,Object>)_annotation.get("resource")).get("chars") != null) {
					String tCleaned = ((String)((Map<String,Object>)_annotation.get("resource")).get("chars")).replaceAll(tRepalceStr,"");
					((Map<String,Object>)_annotation.get("resource")).put(FULL_TEXT_PROPERTY,tCleaned);
				} else {
					_logger.debug("Not adding full text as no chars in resource");
				}
			}
		} else {
			_logger.debug("Not adding full text as no resource");
		}
	}

    protected void expandTarget() {
		String tURI = null;
		Map<String,Object> tSpecificResource = null;
		if (_annotation.get("on") instanceof String) {
			tURI = (String)_annotation.get("on");
			tSpecificResource = this.expandOn(tURI);
            _annotation.put("on", tSpecificResource);
        } else if (_annotation.get("on") instanceof List) {
            List <Object> tOnList = new ArrayList<Object>();
            for (Object tOn : (List<Object>)_annotation.get("on")) {
                if (tOn instanceof String) {
                    tOnList.add(expandOn((String)tOn));
                } else {
                    tOnList.add(tOn);
                }
            }
            _annotation.put("on", tOnList);
		} else if (_annotation.get("on") instanceof Map) {
			tSpecificResource = (Map<String,Object>)_annotation.get("on");

			if (tSpecificResource.get("@id") == null || ((String)tSpecificResource.get("@id")).indexOf("#") == -1) {
				return; // No id to split or no fragement
			}
			if (tSpecificResource.get("selector") != null) {
				return; // already have a selector
			}
			tURI = (String)tSpecificResource.get("@id");

            Map<String,Object> tNewSpecificResource = this.expandOn(tURI);
            if (tSpecificResource.get("within") != null) {
                tNewSpecificResource.put("within", tSpecificResource.get("within"));
            }
            _annotation.put("on", tNewSpecificResource);
		}
        // Unrecognised annotation
	}

    protected Map<String, Object> expandOn(final String tOn) {
        if (tOn.contains("#")) {
            String[] tOnStr = tOn.split("#");

            Map<String,Object> tSpecificResource = new HashMap<String,Object>();
            tSpecificResource.put("@type", "oa:SpecificResource");
            tSpecificResource.put("full", tOnStr[0]);

            Map<String,Object> tSelector = new HashMap<String,Object>();
            tSpecificResource.put("selector", tSelector);
            tSelector.put("@type", "oa:FragmentSelector");
            tSelector.put("value", tOnStr[1]);

            return tSpecificResource;
        } else {
            return null; // throw execption?
        }
    }

    private List<Map<String,Object>> map2list(final Map<String,Object> tMap) {
        List<Map<String, Object>> tNewList = new ArrayList<Map<String,Object>>();
        tNewList.add(tMap);
        return tNewList;
    }

    public Map<String,Object> toJson() {
        return _annotation;
    }

    public String toString() {
        try {
            return JsonUtils.toPrettyString(_annotation);
        } catch (IOException tExcpt) {
            return "Failed to pretty print json due to " + tExcpt.getMessage();
        }
    }
}
