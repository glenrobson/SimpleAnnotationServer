package uk.org.llgc.annotation.store.data;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URL;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import uk.org.llgc.annotation.store.data.Canvas;
       
public class Manifest {
	protected String _URI = "";
	protected String _shortId = "";
	protected String _label = "";
    protected Map<String, Object> _json = null;
    protected List<Canvas> _canvases = new ArrayList<Canvas>();


	public Manifest() {
	}

    public Manifest(final Map<String, Object> pJson, final String pShortId) {
        this.setJson(pJson);
        this.setShortId(pShortId);
        this.setURI((String)pJson.get("@id"));
    }

    public void setJson(final Map<String, Object> pJson) {
        _json = pJson;
        this.setURI((String)_json.get("@id"));
        this.setLabel((String)_json.get("label")); // will fail if there is a multilingual string


        Map<String,Object> tSequence = null;
        if (_json.get("sequences") instanceof List) {
            tSequence = ((List<Map<String, Object>>)_json.get("sequences")).get(0);
        } else {
            tSequence = (Map<String, Object>)_json.get("sequences");
        }

        _canvases = new ArrayList<Canvas>();
        for (Map<String, Object> tCanvas : (List<Map<String, Object>>)tSequence.get("canvases")) {
            _canvases.add(new Canvas((String)tCanvas.get("@id"), (String)tCanvas.get("label")));
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

    public String toString() {
        return "Id: " + _URI + "\nShortId: " + _shortId + "\nLabel: " + _label + "\nCanvases: " + _canvases.size();
    }
}
