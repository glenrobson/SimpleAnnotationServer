package uk.org.llgc.annotation.store.data;

import java.util.Map;
import java.util.HashMap;

public class Target {
    protected Map<String, Object> _json = null;

    public Target(final Object pObj) {
        if (pObj instanceof String) {
            init((String)pObj);
        } else {
            init((Map<String, Object>)pObj);
        }
    }

    protected void init(final String pTarget) {
        _json = new HashMap<String, Object>();
        if (pTarget.indexOf("#") != -1) {
            // Instance of FragementSelector
            String[] tOnStr = pTarget.split("#");

            _json = new HashMap<String,Object>();
            _json.put("@type", "oa:SpecificResource");
            _json.put("full", tOnStr[0]);

            Map<String,Object> tSelector = new HashMap<String,Object>();
            _json.put("selector", tSelector);
            tSelector.put("@type", "oa:FragmentSelector");
            tSelector.put("value", tOnStr[1]);
        } else {
            // Plain URI
            _json.put("@id", pTarget);
        }
    }

    /** 
     * Returns null if no region
     */ 
    public String getRegion() {
        String tRegion = null;
        if (_json.get("@type") != null && _json.get("@type").equals("oa:SpecificResource") && _json.get("selector") != null) {
            Map<String,Object> tSelector = (Map<String,Object>)_json.get("selector");
            if (tSelector.get("@type") != null && tSelector.get("@type").equals("oa:FragmentSelector") && tSelector.get("value") != null) {
                tRegion = (String)tSelector.get("value");
            }
            if (tSelector.get("@type") != null && tSelector.get("@type").equals("oa:Choice") && tSelector.get("default") != null && ((Map<String,String>)tSelector.get("default")).get("@type").equals("oa:FragmentSelector")) {
                tRegion = ((Map<String,String>)tSelector.get("default")).get("value");
            }
        }
        System.out.println("Returning " + tRegion);
        return tRegion;
    }

    protected void init(final Map<String, Object> pTarget) {
        _json = pTarget;
    }

    public Canvas getCanvas() {
        String tKey = "";
        if (_json.get("full") != null) {
            tKey = "full";
        } else if(_json.get("source") != null) {
            tKey = "source";
        } else {
            return null; // No linked canvas
        }
        String tId = "";
        String tLabel = "";
        if (_json.get(tKey) instanceof Map) {
            Map<String,Object> tCanvasMap = (Map<String,Object>)_json.get(tKey);
            tId = (String)tCanvasMap.get("@id");
            if (tCanvasMap.get("label") != null) {
                tLabel = (String)tCanvasMap.get("label");
            }
        } else {
            tId = (String)_json.get(tKey);
        }
        return new Canvas(tId, tLabel);
    }

    public Manifest getManifest() {
        Manifest tManifest = null;
        if (_json.get("within") != null) {
            tManifest = new Manifest();
            if (_json.get("within") instanceof Map) {
                tManifest.setURI((String)((Map<String,Object>)_json.get("within")).get("@id"));
                if (((Map<String,Object>)_json.get("within")).get("label") != null) {
                    tManifest.setLabel((String)((Map<String,Object>)_json.get("within")).get("label"));
                }
            } else {
                tManifest.setURI((String)_json.get("within"));
            }
        }
        return tManifest;
    }

    public void setManifest(final Manifest pManifest) {
        if (!pManifest.getLabel().isEmpty()) {
            // Create map
            Map<String, String> tWithin = new HashMap<String, String>();
            tWithin.put("@id", pManifest.getURI());
            tWithin.put("@type", "sc:Manifest");
            tWithin.put("label", pManifest.getLabel());

            _json.put("within", tWithin);
        } else {
            _json.put("within", pManifest.getURI());
        }
    }

    public Map<String, Object> toJson() {
        return _json;
    }
}
