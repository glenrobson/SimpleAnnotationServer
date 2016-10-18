package uk.org.llgc.annotation.store.data;

public class Manifest {
	protected String _URI = "";
	protected String _shortId = "";
	protected String _label = "";

	public Manifest() {
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
}
