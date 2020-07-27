package uk.org.llgc.annotation.store.data.rdf;

import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;

import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;

import com.github.jsonldjava.core.JsonLdError;


import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;


import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

public class RDFManifest extends Manifest {
    protected Model _manifest = null;

    public RDFManifest(final Model pManifest) {
        super();
        this.setupManifest(pManifest);
    }

    protected void setupManifest(final Model pModel) {
        _manifest = pModel;
        pModel.begin();
        // set uri
        // should maybe use type = Manifest for this:
        Iterator tSubjects = _manifest.listSubjectsWithProperty(DC.identifier);
        Resource tManifestId = (Resource)tSubjects.next();
        super.setURI(tManifestId.getURI());

        // set Manifest Label
        Statement tLabelStatement = _manifest.getProperty(tManifestId, RDFS.label);
        super.setLabel(tLabelStatement.getLiteral().getString());

        // set short id
        Statement tShortIdStatement = _manifest.getProperty(tManifestId, DC.identifier);
        super.setShortId(tShortIdStatement.getLiteral().getString());

        // set canvas

        String tQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                        "select ?manifest ?canvas ?label where {" +
                        "        <" + tManifestId + "> <http://iiif.io/api/presentation/2#hasSequences> ?sequences . " +
                        "        ?sequences <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?sequence ." +
                        "        ?sequence <http://iiif.io/api/presentation/2#hasCanvases> ?canvases ." +
                        "        ?canvases rdf:rest*/rdf:first ?canvas ." +
                        "        ?canvas <http://www.w3.org/2000/01/rdf-schema#label> ?label " +
                        "}"; 

        QueryExecution tExec = QueryExecutionFactory.create(tQuery, _manifest);
		ResultSet results = tExec.execSelect(); 
        _canvases = new ArrayList<Canvas>();
		if (results != null) {
			while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource tCanvasURI = soln.getResource("canvas");
                Literal tLabel = soln.getLiteral("label");
                _canvases.add(new Canvas(tCanvasURI.getURI(), tLabel.getString()));
            }
        }

        pModel.commit();
    }

    public void setModel(final Model pModel) {
        this.setupManifest(pModel);
    }

    public Model getModel() {
        return _manifest;
    }

    public Map<String,Object> getJson() throws IOException {
        Map<String,Object> tJson = null;
		try {
            AnnotationUtils tUtils = new AnnotationUtils();
			tUtils.frameManifest(_manifest);
		} catch (JsonLdError tException) {
			throw new IOException("Failed to convert manifest to JsonLd due to "+ tException.toString());
		}
		return tJson;
    }
}
