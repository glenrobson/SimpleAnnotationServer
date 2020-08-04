package uk.org.llgc.annotation.store.data;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.AnnotationUtils;

public class AnnotationList {
    protected List<Model> _annotations = null;
    protected String _id = "";

    public AnnotationList(final List<Model> pAnnotations) {
        _annotations = pAnnotations;
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

    public Map<String, Object> toJson() throws IOException {
        Map<String,Object> tJson = new HashMap<String,Object>();
        tJson.put("@context", "http://iiif.io/api/presentation/2/context.json");
        tJson.put("@id", _id);
        tJson.put("@type", "sc:AnnotationList");
        List<Map<String,Object>> tAnnos = new ArrayList<Map<String,Object>>();
        tJson.put("resources", tAnnos);

        System.out.println("Number of annos: " + _annotations.size());
        AnnotationUtils tUtils = new AnnotationUtils();
        for (Model tModel : _annotations) {
            tAnnos.add(tUtils.frameAnnotation(tModel, false));
        }

        return tJson;
    }
}
