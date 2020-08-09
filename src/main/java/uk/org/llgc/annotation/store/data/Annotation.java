package uk.org.llgc.annotation.store.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

import org.apache.jena.vocabulary.DCTerms;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URI;

public class Annotation {
    public static final String FULL_TEXT_PROPERTY = "http://dev.llgc.org.uk/sas/full_text";
    protected static Logger _logger = LogManager.getLogger(Annotation.class.getName());
	protected SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected Map<String,Object> _annotation = null;
    protected List<Body> _bodies = null;
    protected List<Target> _targets = null;

    public Annotation(final Map<String,Object> pAnno) {
        this.setJson(pAnno);
    }

    public void setJson(final Map<String, Object> pJson) {
        _annotation = pJson;
        this.init();
    }

    protected void init() {
        this.standaiseAnno();
        this.expandTarget();
        addMetadata();

        _bodies = new ArrayList<Body>();
        if (_annotation.get("resource") != null) {
            if (_annotation.get("resource") instanceof List) {
                for (Object tBody : (List<Object>)_annotation.get("resource")) {
                    _bodies.add(new Body(tBody));
                }
            } else {
                _bodies.add(new Body(_annotation.get("resource")));
            }
        }

        _targets = new ArrayList<Target>();
        if (_annotation.get("on") != null) {
            if (_annotation.get("on") instanceof List) {
                for (Object tTarget : (List<Object>)_annotation.get("on")) {
                    _targets.add(new Target(tTarget));
                }
            } else {
                _targets.add(new Target(_annotation.get("on")));
            }
        }
    }

    protected void standaiseAnno() {
        // Compact created
        if (_annotation.get("http://purl.org/dc/terms/created") != null) {
            _annotation.put("dcterms:created", _annotation.get("http://purl.org/dc/terms/created"));
            _annotation.remove("http://purl.org/dc/terms/created");
        }
        if (_annotation.get("http://purl.org/dc/terms/modified") != null) {
            _annotation.put("dcterms:modified", _annotation.get("http://purl.org/dc/terms/modified"));
            _annotation.remove("http://purl.org/dc/terms/modified");
        }
        if (_annotation.get("resource") != null && _annotation.get("resource") instanceof Map) {
            // turn into Array?
            List tList = new ArrayList();
            tList.add(_annotation.get("resource"));
            
            _annotation.put("resource", tList);
        }
    }

    public String getId() {
        return (String)_annotation.get("@id");
    }

    public void setId(final String pId) {
        _annotation.put("@id", pId);
    }

    // In search api this is the snippet
    public void setLabel(final String pLabel) {
        _annotation.put("label", pLabel);
    }

    public String getType() {
        return (String)_annotation.get("@type");
    }

    public void setType(final String pType) {
        _annotation.put("@type", pType);
    }

    public List<String> getMotivations() {
        List<String> tMotivations = new ArrayList<String>();
        if (_annotation.get("motivation") instanceof String) {
            tMotivations.add((String)_annotation.get("motivation"));
        } else {
            tMotivations = (List<String>)_annotation.get("motivation");
        }
        return tMotivations;
    }

    public void setMotivations(final List<String> pMotivations) {
        _annotation.put("motivation", pMotivations);
    }

    public void setCreated(final String pDate) {
        if (_annotation.get("dcterms:" + DCTerms.created.getLocalName()) == null) {
            _annotation.put("dcterms:" + DCTerms.created.getLocalName(), pDate);
        }
    }

    public void setCreated(final Date pDate) {
        _annotation.put("dcterms:" + DCTerms.created.getLocalName(), _dateFormatter.format(pDate));
    }

    public Date getCreated() {
        if (_annotation.get("dcterms:" + DCTerms.created.getLocalName()) == null) {
            return null;
        } else {
            String tDate = (String)_annotation.get("dcterms:" + DCTerms.created.getLocalName());
            try {
                return _dateFormatter.parse(tDate);
            } catch (ParseException tExcpt) {
                // This shouldn't happen as date is created above...
                tExcpt.printStackTrace();
                return null;
            }
        }
    }

    public Date getModified() {
        if (_annotation.get("dcterms:" + DCTerms.modified.getLocalName()) == null) {
            return null;
        } else {
            String tDate = (String)_annotation.get("dcterms:" + DCTerms.modified.getLocalName());
            try {
                return _dateFormatter.parse(tDate);
            } catch (ParseException tExcpt) {
                // This shouldn't happen as date is created above...
                tExcpt.printStackTrace();
                return null;
            }
        }
    }

    public void updateModified() {
		_annotation.put("dcterms:" + DCTerms.modified.getLocalName(), _dateFormatter.format(new Date()));
    }

    public void addWithin(final Manifest pManifest, final Canvas pCanvas) {
        for (Target tTarget : this.getTargets()) {
            if (tTarget.getCanvas().equals(pCanvas)) {
                tTarget.setManifest(pManifest);
            }
        }
    }

    public List<Body> getBodies() {
        return _bodies; 
    }

    public List<Target> getTargets() {
        return _targets; 
    }

    /**
     * Check if the load annotaiton is valid
     */
    public String checkValid() throws MalformedAnnotation {
        try {
            URI tURI = new URI(this.getId()); // Check if this is a valid URI otherwise it will fail to load correctly.
            if (!tURI.isAbsolute()) {
                // No scheme so invalid
                throw new MalformedAnnotation("URI: '" + tURI + "' doesn't contain a scheme");
            }
        } catch (URISyntaxException tExcpt) {
            throw new MalformedAnnotation("URI: " + this.getId() + " is invalid due to " + tExcpt.getMessage());
        }

        if (_annotation.get("on") == null) {
            throw new MalformedAnnotation("Missing on");
        }
        StringBuffer tOutput = new StringBuffer();
        boolean tMalformed = false;
        for (Target tTarget : this.getTargets()) {
            // Look to see if selector value is an array. It should be a string.
            Map<String, Object> tOn = tTarget.toJson();
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

    public List<Target> getMissingWithin() {
        List<Target> tMissing = new ArrayList<Target>();

        for (Target tTarget : this.getTargets()) {
            if (tTarget.getManifest() == null) {
                tMissing.add(tTarget);
            }
        }
		return tMissing;
    }

    protected void addMetadata() {
		// Add create date if it doesn't already have one
		if (this.getCreated() == null) {
			this.setCreated(new Date());
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
