package uk.org.llgc.annotation.store.data;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URL;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.AnnotationUtils;
       
public class Manifest {
	protected String _URI = "";
	protected String _shortId = "";
	protected String _label = "";
    protected Map<String, Object> _json = null;
    protected List<Canvas> _canvases = new ArrayList<Canvas>();


	public Manifest() {
	}

    /*
     * Set manifest before it has a short id
     */
     public Manifest(final Map<String, Object> pJson) throws IOException {
        this(pJson, "");
    }

    public Manifest(final Map<String, Object> pJson, final String pShortId) throws IOException {
        if (pJson.get("@type") == null) {
            if (pJson.get("type") != null) {
                throw new IOException("SAS Currently only works with IIIF version 2.0 manifests");
            } else {
                throw new IOException("Failed to process manifest as it has no @type property.");
            }
        } else {
            if (!pJson.get("@type").equals("sc:Manifest")) {
                throw new IOException("Can't create manifest as type was incorrect. Expected sc:Manifest but got: " + this.getType());
            }
        }
        this.setJson(pJson);
        this.setShortId(pShortId);
        this.setURI((String)pJson.get("@id"));
    }

    public Map<String, Object> toJson() {
        return _json;
    }

    public void setJson(final Map<String, Object> pJson) {
        _json = pJson;
        this.setURI((String)_json.get("@id"));
        if (_json.get("label") != null) {
            String tLabel = "";
            if (_json.get("label") instanceof String) {
                tLabel = (String)_json.get("label");
            } else if (_json.get("label") instanceof Map) {
                Map<String,Object> tLabelMap = (Map<String,Object>)_json.get("label");
                if (tLabelMap.get("@value") != null && tLabelMap.get("@value") instanceof String) {
                    tLabel = (String)tLabelMap.get("@value");
                }
            } else {
                // Label is an array of one or more language strings or an array of strings
                List<Object> tLabels = (List)_json.get("label");
                if (!tLabels.isEmpty()) {
                    if (tLabels.get(0) instanceof String) {
                        tLabel = (String)tLabels.get(0);
                    } else if (tLabels.get(0) instanceof Map){
                        // Lang map just select first
                        Map<String,Object> tLabelMap = (Map<String,Object>)tLabels.get(0);
                        if (tLabelMap.get("@value") != null && tLabelMap.get("@value") instanceof String) {
                            tLabel = (String)tLabelMap.get("@value");
                        }
                        
                    }
                }
            }

            this.setLabel(tLabel);
        }
        if (this.getLabel().isEmpty()) {
            this.setLabel("Missing Manifest label");
        }

        Map<String,Object> tSequence = null;
        if (_json.get("sequences") instanceof List ) {
            if (!((List)_json.get("sequences")).isEmpty()) {
                tSequence = ((List<Map<String, Object>>)_json.get("sequences")).get(0);
            }
        } else {
            tSequence = (Map<String, Object>)_json.get("sequences");
        }

        _canvases = new ArrayList<Canvas>();
        if (tSequence != null) {
            for (Map<String, Object> tCanvas : (List<Map<String, Object>>)tSequence.get("canvases")) {
                _canvases.add(new Canvas((String)tCanvas.get("@id"), (String)tCanvas.get("label")));
            }
        }
    }

    public String getType() {
        try {
            return (String)getJson().get("@type");
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
            return null;
        }
    }

    public Canvas getCanvas(final String pId) {
        for (Canvas tCanvas : _canvases) {
            if (tCanvas.getId().equals(pId)) {
                return tCanvas;
            }
        }
        return null;
    }

    /**
     *
     */
    public Map<String,Object> getJson() throws IOException {
        if (_json == null) {
            this.setJson((Map<String,Object>)JsonUtils.fromInputStream(new URL(_URI).openStream()));
        }
        return _json;
    }    
	
	/**
	 * Get URI.
	 *
	 * @return URI as String.
	 */
	public String getURI() {
	    return _URI;
	}
	
	/**
	 * Set URI.
	 *
	 * @param URI the value to set.
	 */
	public void setURI(final String pURI) {
	     _URI = pURI;
	}
	
	/**
	 * Get shortId.
	 *
	 * @return shortId as String.
	 */
	public String getShortId() {
        if (_shortId == null || _shortId.isEmpty()) {
            if (_URI.endsWith("manifest.json")) {
                String[] tURI = _URI.split("/");
                _shortId = tURI[tURI.length - 2];
            } else {
                try {
                    _shortId = AnnotationUtils.getHash(_URI, "md5");
                } catch (IOException tExcpt) {
                    tExcpt.printStackTrace();
                }
            }
        }
	    return _shortId;
	}
	
	/**
	 * Set shortId.
	 *
	 * @param shortId the value to set.
	 */
	public void setShortId(final String pShortId) {
	     _shortId = pShortId;
	}
	
	/**
	 * Get label.
	 *
	 * @return label as String.
	 */
	public String getLabel() {
	    return _label;
	}
	
	/**
	 * Set label.
	 *
	 * @param label the value to set.
	 */
	public void setLabel(final String pLabel) {
	     _label = pLabel;
	}

    
    public List<Canvas> getCanvases() {
        return _canvases;
    }

    public boolean equals(Object pOther) {
        if (pOther instanceof Manifest) {
            return _URI.equals(((Manifest)pOther).getURI());
        } else {
            return false;
        }
    }

    public String toString() {
        return "Id: " + _URI + "\nShortId: " + _shortId + "\nLabel: " + _label + "\nCanvases: " + _canvases.size();
    }
}
