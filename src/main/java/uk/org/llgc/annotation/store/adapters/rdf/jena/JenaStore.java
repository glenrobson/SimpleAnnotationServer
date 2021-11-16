package uk.org.llgc.annotation.store.adapters.rdf.jena;

import org.apache.jena.query.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.JenaTransactionException;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.adapters.rdf.AbstractRDFStore;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import java.nio.charset.Charset;

public class JenaStore extends AbstractRDFStore implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(JenaStore.class.getName());

	protected Dataset _dataset = null;

	public JenaStore(final AnnotationUtils pUtils, final String pDataDir) {
        super(pUtils);
		_dataset = TDBFactory.createDataset(pDataDir);
	}

	protected Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		String tJson = JsonUtils.toString(pJson);

        _logger.debug("Converting: " + JsonUtils.toPrettyString(pJson));
		Model tJsonLDModel = ModelFactory.createDefaultModel();

		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);


		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pJson.get("@id"), tJsonLDModel);
		_dataset.commit();

		return tJsonLDModel;
	}

    protected void storeModel(final String pGraphName, final Model pModel) throws IOException {
		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel(pGraphName, pModel);
		_dataset.commit();
    }

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		_dataset.begin(ReadWrite.WRITE) ; // should probably move this to deleted state
		_dataset.removeNamedModel(pAnnoId);
		_dataset.commit();
	}

	protected QueryExecution getQueryExe(final String pQuery) {
		return QueryExecutionFactory.create(pQuery, _dataset);
	}
	protected Model getNamedModel(final String pContext) throws IOException {
		boolean tLocaltransaction = !_dataset.isInTransaction();
		if (tLocaltransaction) {
			_dataset.begin(ReadWrite.READ);
		}
		Model tAnnotation = _dataset.getNamedModel(pContext);
		if (tAnnotation.isEmpty()) {
			tAnnotation = null; // annotation wasn't found
		}
		if (tLocaltransaction) {
			_dataset.end();
		}
        return tAnnotation;
	}

	protected void begin(final ReadWrite pWrite) {
        try {
            _dataset.begin(pWrite);
        } catch (JenaTransactionException tExcpt) {
            System.err.println("In transaction so going to try and close it before re-openning");
            tExcpt.printStackTrace();
            this.end();
            _dataset.begin(pWrite);
        }
	}
	protected void end() {
		_dataset.end();
	}

	protected String indexManifestOnly(final String pShortId, Map<String,Object> pManifest) throws IOException {
		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(JsonUtils.toString(pManifest).getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		//RDFDataMgr.write(System.out, tJsonLDModel, Lang.NQUADS);
		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pManifest.get("@id"), tJsonLDModel);

		_dataset.commit();

		return pShortId;
	}
}
