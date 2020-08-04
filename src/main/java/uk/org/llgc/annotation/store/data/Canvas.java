package uk.org.llgc.annotation.store.data;

import uk.org.llgc.annotation.store.AnnotationUtils;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

public class Canvas {
    protected String _id = "";
    protected String _label = "";
    protected String _shortId = "";

    public Canvas(final String pId, final String pLabel) {
        this.setId(pId);
        this.setLabel(pLabel);
    }
    
    /**
     * Get uri.
     *
     * @return uri as String.
     */
    public String getId() {
        return _id;
    }
    
    /**
     * Set uri.
     *
     * @param uri the value to set.
     */
    public void setId(final String pURI) {
         _id = pURI;
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
    
    protected static Map<String,String> _canvasIdMap = new HashMap<String, String>();
    /**
     * Get shortId.
     *
     * @return shortId as String.
     */
    public String getShortId() {
        if (_shortId == null || _shortId.trim().isEmpty()) {
            if (_canvasIdMap.get(_id) == null) {
                try {
                    _shortId = AnnotationUtils.getHash(_id, "md5");
                    // Cache
                    _canvasIdMap.put(_id, _shortId);
                } catch (IOException tExcpt) {
                    tExcpt.printStackTrace();
                    return null;
                }
            } else {
                // Use cached
                _shortId = _canvasIdMap.get(_id);
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

    public String toString() {
        return "Canvas:\n\tid: " + _id + "\n\tShort id: " + _shortId + "\n\tLabel: " + _label; 
    }
}
