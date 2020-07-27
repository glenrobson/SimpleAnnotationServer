package uk.org.llgc.annotation.store.data;

public class Canvas {
    protected String _id = "";
    protected String _label = "";

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
}
