package uk.org.llgc.annotation.store.data;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.users.User;
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

    public String getAnnoListURL(final Canvas pCanvas, final String pBaseURI, final User pUser) {
        StringBuffer tAnnoListURL = new StringBuffer(pBaseURI);
        if (!pBaseURI.endsWith("/")) {
            tAnnoListURL.append("/");
        }

        tAnnoListURL.append("annotations/");
        tAnnoListURL.append(pUser.getShortId());
        tAnnoListURL.append("/");
        tAnnoListURL.append(pCanvas.getShortId());
        tAnnoListURL.append(".json");

        return tAnnoListURL.toString();
    }

    public void addAnnotationLists(final String pBaseURI, final User pUser) {
        Map<String,Object> tSequence = null;
        if (_json.get("sequences") instanceof List ) {
            if (!((List)_json.get("sequences")).isEmpty()) {
                tSequence = ((List<Map<String, Object>>)_json.get("sequences")).get(0);
            }
        } else {
            tSequence = (Map<String, Object>)_json.get("sequences");
        }

        if (tSequence != null) {
            StringBuffer tAnnoListURL = new StringBuffer(pBaseURI);
            if (!pBaseURI.endsWith("/")) {
                tAnnoListURL.append("/");
            }

            tAnnoListURL.append("annotations/");
            tAnnoListURL.append(pUser.getShortId());
            tAnnoListURL.append("/##CANVAS_SHORT##.json");

            for (Map<String, Object> tCanvasJson : (List<Map<String, Object>>)tSequence.get("canvases")) {
                /*  "otherContent": [ {
                        "@id": "http://localhost:8888/annotation/list/41eef9939cd722b17ef4c177c2afa12d.json",
                        "@type": "sc:AnnotationList",
                        "label": "My fantastic annotations"
                    }]*/
                Canvas tCanvas = new Canvas((String)tCanvasJson.get("@id"), (String)tCanvasJson.get("label"));

                Map<String, Object> tOtherContent = new HashMap<String, Object>();
                tOtherContent.put("@id", tAnnoListURL.toString().replace("##CANVAS_SHORT##", tCanvas.getShortId()));
                tOtherContent.put("@type", "sc:AnnotationList");
                tOtherContent.put("label", "Annotations for canvas " + tCanvas.getLabel());

                this.addKey(tCanvasJson, "otherContent", tOtherContent, true);
            }
        }
    }

    /**
     * Add key but if it already exists add it to a list
     * @param alwaysList if true then always set data as a list.
     */
    protected void addKey(final Map<String,Object> pParent, final String pKey, final Map<String,Object> pData, final boolean alwaysList) {
        List tValueList = new ArrayList();

        // If service already exists then add to it. 
        if (pParent.containsKey(pKey)) {
            if (pParent.get(pKey) instanceof Map) {
                tValueList.add(pParent.get(pKey));
                pParent.put(pKey, tValueList);
            }

            ((List)pParent.get(pKey)).add(pData);
        } else {
            if (alwaysList) {
                tValueList.add(pData);
                pParent.put(pKey, tValueList);
            } else {
                pParent.put(pKey, pData);
            }
        }
    }

    public URL getSearchURL(final String pBaseURI, final User pUser) {
        StringBuffer tSearchURL = new StringBuffer(pBaseURI);
        if (!pBaseURI.endsWith("/")) {
            tSearchURL.append("/");
        }

        tSearchURL.append("search-api/");
        tSearchURL.append(pUser.getShortId());
        tSearchURL.append("/");
        tSearchURL.append(this.getShortId());
        tSearchURL.append("/search");
        try {
            return new URL(tSearchURL.toString());
        } catch (MalformedURLException tExcpt) {
            System.out.println("Unable to create URL from " + tSearchURL.toString());
            tExcpt.printStackTrace();
        }
        return null;
    }

    public void addSearchService(final String pBaseURI, final User pUser) {
        /* "service": {
            "@context": "http://iiif.io/api/search/0/context.json"
            "@id": "http://localhost:8888/search-api/5bbada360fbe7c8f72a8153896686398/search",
            "profile": "http://iiif.io/api/search/0/search",
        },*/

        Map<String,Object> tService = new HashMap<String,Object>();
        tService.put("@context", "http://iiif.io/api/search/0/context.json");
        tService.put("@id", this.getSearchURL(pBaseURI, pUser).toString());
        tService.put("profile", "http://iiif.io/api/search/0/search");

        this.addKey(_json, "service", tService, false);
    }

    public String getType() {
        try {
            return (String)getJson().get("@type");
        } catch (IOException tExcpt) {
            //tExcpt.printStackTrace();
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
         if (_json != null) {
             _json.put("@id", pURI);
         }
	}
	
	/**
	 * Get shortId.
	 *
	 * @return shortId as String.
	 */
	public String getShortId() {
        if (_shortId == null || _shortId.isEmpty()) {
            // Its no longer safe to use this NLW shortcut 
            // as it fails the workbench:
            // https://glenrobson.github.io/workbench/manifests/projectmanifest.json"
            /*if (_URI.endsWith("manifest.json")) {
                String[] tURI = _URI.split("/");
                _shortId = tURI[tURI.length - 2];
            } else {*/
            try {
                _shortId = AnnotationUtils.getHash(_URI, "md5");
            } catch (IOException tExcpt) {
                tExcpt.printStackTrace();
            }
            //}
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

    public String getLogo() {
        String tLogo = null;
        
        if (_json != null && _json.containsKey("logo")) {
            if (_json.get("logo") instanceof Map && ((Map)_json.get("logo")).containsKey("@id")) {
                tLogo = (String)((Map<String,Object>)_json.get("logo")).get("@id");
            } else if (_json.get("logo") instanceof String) {
                tLogo = (String)_json.get("logo");
            }
        }

        return tLogo;
    }

    public String getDescription(){
        String tDesc = null;
        if (_json != null && _json.containsKey("description")) {
            tDesc = (String)_json.get("description");
        }

        return tDesc;
    }
    
    public String getAttribution() {
        String tAtt = null;
        if (_json != null && _json.containsKey("attribution")) {
            tAtt = (String)_json.get("attribution");
        }

        return tAtt;
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
