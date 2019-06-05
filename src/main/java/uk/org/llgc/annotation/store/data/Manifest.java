package uk.org.llgc.annotation.store.data;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URL;

import java.util.Map;
import java.util.List;
       
public class Manifest {
	protected String _URI = "";
	protected String _shortId = "";
	protected String _label = "";
    protected Map<String, Object> _json = null;

	public Manifest() {
	}

    public Manifest(final Map<String, Object> pJson, final String pShortId) {
        this.setJson(pJson);
        this.setShortId(pShortId);
    }

    public void setJson(final Map<String, Object> pJson) {
        _json = pJson;
        this.setURI((String)_json.get("@id"));
        this.setLabel((String)_json.get("label")); // will fail if there is a multilingual string
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
        if (_label == null || _label.trim().length() == 0) {
            try {
                this.getJson();
            } catch (IOException tExcpt) {
                System.err.println("Failed to get label as manifest could not be retrieved");
                tExcpt.printStackTrace();
            }
        }
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

    public int getPageCount() {
        int count = -1;
        if (_json != null) {
            Map<String,Object> sequence = null;
            if (_json.get("sequence") instanceof List) {
                sequence = ((List<Map<String, Object>>)sequence).get(0);
            } else {
                sequence = (Map<String, Object>)sequence;
            }
            count = ((List)sequence.get("canvases")).size();
        }

        return count;
    }
}
