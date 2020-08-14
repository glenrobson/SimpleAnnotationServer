package uk.org.llgc.annotation.store.data;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

public class IIIFSearchResults extends AnnotationList {
    protected AnnoListNav _nav = null;
    protected int _startIndex = -1;
    protected String _next = "";

    public IIIFSearchResults() {
        super();
    }

    public Map<String, Object> toJson() throws IOException {
        Map<String,Object> tJson = new HashMap<String,Object>();
        tJson.put("@context", "http://iiif.io/api/presentation/2/context.json");
        tJson.put("@id", _id);
        tJson.put("@type", "sc:AnnotationList");
        if (_nav != null) {
            tJson.put("within", _nav.toJson());
        }
        if (_next != null && !_next.isEmpty()) {
            tJson.put("next", _next);
        }
        if (_startIndex != -1) {
            tJson.put("startIndex", _startIndex);
        }

        List<Map<String,Object>> tAnnos = new ArrayList<Map<String,Object>>();
        tJson.put("resources", tAnnos);

        for (Annotation tAnno: _annotations) {
            // TODO need to collapse the annotations into #xywh
            tAnnos.add(tAnno.jsonTargetRect());
        }

        return tJson;
    }
    /**
     * Get nav.
     *
     * @return nav as AnnoListNav.
     */
    public AnnoListNav getNav() {
        return _nav;
    }
    
    /**
     * Set nav.
     *
     * @param nav the value to set.
     */
    public void setNav(final AnnoListNav pNav) {
         _nav = pNav;
    }
    
    /**
     * Get startIndex.
     *
     * @return startIndex as protected.
     */
    public int getStartIndex() {
        return _startIndex;
    }
    
    /**
     * Set startIndex.
     *
     * @param startIndex the value to set.
     */
    public void setStartIndex(final int pStartIndex) {
         _startIndex = pStartIndex;
    }
    
    /**
     * Get next.
     *
     * @return next as String.
     */
    public String getNext() {
        return _next;
    }
    
    /**
     * Set next.
     *
     * @param next the value to set.
     */
    public void setNext(final String pNext) {
         _next = pNext;
    }

}
