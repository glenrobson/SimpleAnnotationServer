package uk.org.llgc.annotation.store.data;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.AnnotationUtils;

public class AnnotationList {
    protected List<Annotation> _annotations = null;
    protected String _id = "";
    protected AnnoListNav _nav = null;
    protected int _startIndex = -1;
    protected String _next = "";

    public AnnotationList() {
        _annotations = new ArrayList<Annotation>();
    }

    public AnnotationList(final List<Map<String, Object>> pAnnos) {
        _annotations = new ArrayList<Annotation>();
        for (Map<String,Object> tAnno : pAnnos) {
            _annotations.add(new Annotation(tAnno));
        }
    }

    public AnnotationList(final List<Model> pAnnotations, final boolean pCompact) throws IOException {
        _annotations = new ArrayList<Annotation>();
        AnnotationUtils tUtils = new AnnotationUtils();
        for (Model tModel : pAnnotations) {
            _annotations.add(new Annotation(tUtils.frameAnnotation(tModel, pCompact)));
        }
    }
    
    /**
     * Get id.
     *
     * @return id as String.
     */
    public String getId() {
        return _id;
    }
    
    /**
     * Set id.
     *
     * @param id the value to set.
     */
    public void setId(final String pId) {
         _id = pId;
    }

    public void add(final Annotation pAnno) {
        _annotations.add(pAnno);
    }
    
    public Annotation get(final String pAnnoId) {
        for (Annotation tAnno : _annotations) {
            if (tAnno.getId().equals(pAnnoId)) {
                return tAnno;
            }
        }
        return null;
    }

    public int size() {
        return _annotations.size();
    }

    public List<Annotation> getAnnotations() {
        return _annotations;
    }
    
    public void setAnnotations(final List<Annotation> pAnnos) {
        _annotations = pAnnos;
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
            tAnnos.add(tAnno.toJson());
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
