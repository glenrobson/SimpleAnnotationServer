package uk.org.llgc.annotation.store.data.stats;

public class TopLevel {
    protected int _totalAnnotations = 0;
    protected int _totalManifests = 0;
    protected int _totalAnnoCanvases = 0;

    public TopLevel() {
    }
    
    /**
     * Get totalAnnotations.
     *
     * @return totalAnnotations as int.
     */
    public int getTotalAnnotations() {
        return _totalAnnotations;
    }
    
    /**
     * Set totalAnnotations.
     *
     * @param totalAnnotations the value to set.
     */
    public void setTotalAnnotations(final int pTotalAnnotations) {
         _totalAnnotations = pTotalAnnotations;
    }
    
    /**
     * Get totalManifests.
     *
     * @return totalManifests as int.
     */
    public int getTotalManifests() {
        return _totalManifests;
    }
    
    /**
     * Set totalManifests.
     *
     * @param totalManifests the value to set.
     */
    public void setTotalManifests(final int pTotalManifests) {
         _totalManifests = pTotalManifests;
    }
    
    /**
     * Get totalAnnoCanvases.
     *
     * @return totalAnnoCanvases as int.
     */
    public int getTotalAnnoCanvases() {
        return _totalAnnoCanvases;
    }
    
    /**
     * Set totalAnnoCanvases.
     *
     * @param totalAnnoCanvases the value to set.
     */
    public void setTotalAnnoCanvases(final int pTotalAnnoCanvases) {
         _totalAnnoCanvases = pTotalAnnoCanvases;
    }
}
