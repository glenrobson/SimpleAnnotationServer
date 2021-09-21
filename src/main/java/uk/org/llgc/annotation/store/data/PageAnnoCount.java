package uk.org.llgc.annotation.store.data;

import uk.org.llgc.annotation.store.data.Manifest;

public class PageAnnoCount {
	protected int _count = 0;
    protected Canvas _canvas = null;
    protected Manifest _manifest = null;

	public PageAnnoCount(final Canvas pCanvas, final int pCount, final Manifest pManifest) {
		this.setCanvas(pCanvas);
		this.setCount(pCount);
        this.setManifest(pManifest);
	}

    public Manifest getManifest() {
        return _manifest;
    }

    public void setManifest(final Manifest pManifest) {
        _manifest = pManifest;
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
     * Get canvas.
     *
     * @return canvas as Canvas.
     */
    public Canvas getCanvas() {
        return _canvas;
    }
    
    /**
     * Set canvas.
     *
     * @param canvas the value to set.
     */
    public void setCanvas(final Canvas pCanvas) {
         _canvas = pCanvas;
    }

    public boolean equals(final Object pOtherObj) {
        if (pOtherObj instanceof PageAnnoCount) {
            PageAnnoCount tOther = (PageAnnoCount)pOtherObj;
            return _canvas.getId().equals(tOther.getCanvas().getId());
        } else {
            return false;
        }
    }

	public String toString() {
		return "Canvas=>" + _canvas.toString() + "\tCount=>" + _count;
	}
}
