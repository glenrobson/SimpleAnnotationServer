package uk.org.llgc.annotation.store.data;

public class PageAnnoCount {
	protected int _count = 0;
	protected String _pageId = "";

	public PageAnnoCount(final String pPageId, final int pCount) {
		this.setPageId(pPageId);
		this.setCount(pCount);
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
