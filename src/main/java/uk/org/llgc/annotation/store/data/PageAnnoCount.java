package uk.org.llgc.annotation.store.data;

import uk.org.llgc.annotation.store.data.Manifest;

public class PageAnnoCount {
	protected int _count = 0;
	protected String _pageId = "";
    protected String _label = "";
    protected Manifest _manifest = null;

	public PageAnnoCount(final String pPageId, final int pCount, final String pLabel, final Manifest pManifest) {
		this.setPageId(pPageId);
		this.setCount(pCount);
        this.setLabel(pLabel);
        this.setManifest(pManifest);
	}

    public Manifest getManifest() {
        return _manifest;
    }

    public void setManifest(final Manifest pManifest) {
        _manifest = pManifest;
    }

    public String getLabel() {
        return _label;
    }

    public void setLabel(final String pLabel) {
        _label = pLabel;
    }
	
	/**
	 * Get count.
	 *
	 * @return count as int.
	 */
	public int getCount() {
	    return _count;
	}
	
	/**
	 * Set count.
	 *
	 * @param count the value to set.
	 */
	public void setCount(final int pCount) {
	     _count = pCount;
	}
	
	/**
	 * Get pageId.
	 *
	 * @return pageId as String.
	 */
	public String getPageId() {
	    return _pageId;
	}
	
	/**
	 * Set pageId.
	 *
	 * @param pageId the value to set.
	 */
	public void setPageId(final String pPageId) {
	     _pageId = pPageId;
	}

	public String toString() {
		return "Pageid=>" + _pageId + "\tCount=>" + _count;
	}
}
