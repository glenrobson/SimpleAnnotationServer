package uk.org.llgc.annotation.store.data;

import java.util.Map;
import java.util.HashMap;

public class Body {
    public static final String FULL_TEXT_PROPERTY = Annotation.FULL_TEXT_PROPERTY;

    protected Map<String, Object> _json = null;

    public Body(final Object pObj) {
        if (pObj instanceof String) {
            init((String)pObj);
        } else {
            init((Map<String, Object>)pObj);
        }

        if (_json.get("chars") != null) {
			String tRepalceStr = "<[ /]*[a-zA-Z0-9 ]*[ /]*>";
            _json.put(FULL_TEXT_PROPERTY, ((String)_json.get("chars")).replaceAll(tRepalceStr, ""));
        }
    }

    protected void init(final String pBody) {
        _json = new HashMap<String, Object>();
        _json.put("@id", pBody);
    }

    protected void init(final Map<String, Object> tBodyJson) {
        _json = tBodyJson;
    }

    public String getType() {
        return (String)_json.get("@type");
    }

    public String getChars() {
        return (String)_json.get("chars");
    }

    public String getIndexableContent() {
        String tContent = "";
        if (_json.get("chars") != null) {
            tContent = (String)_json.get("chars");
        }
        return tContent;
    }

    public Map<String, Object> toJson() {
        return _json;
    }
}
