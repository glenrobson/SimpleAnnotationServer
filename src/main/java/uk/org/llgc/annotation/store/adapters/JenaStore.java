package uk.org.llgc.annotation.store.adapters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import uk.org.llgc.annotation.store.data.PageAnnoCount;

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

	public JenaStore(final String pDataDir) {
		_dataset = TDBFactory.createDataset(pDataDir);
	}

	public Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		String tJson = JsonUtils.toString(pJson);

		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pJson.get("@id"), tJsonLDModel);
		_dataset.commit();

		return tJsonLDModel;
	}

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		_dataset.begin(ReadWrite.WRITE); // should probably move this to deleted state
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
		if (tLocaltransaction) {
			_dataset.end();
		}	
		if (tAnnotation.isEmpty()) {
			return null; // annotation wasn't found
		} else {
			return tAnnotation;
		}	
	}

	protected void begin(final ReadWrite pWrite) {
		_dataset.begin(pWrite);
	}
	protected void end() {
		_dataset.end();
	}

	protected String indexManifestOnly(final String pShortId, Map<String,Object> pManifest) throws IOException {
		_dataset.begin(ReadWrite.WRITE) ;
		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(JsonUtils.toString(pManifest).getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		
		//RDFDataMgr.write(System.out, tJsonLDModel, Lang.NQUADS);
		_dataset.addNamedModel((String)pManifest.get("@id"), tJsonLDModel);

		_dataset.commit();

		return pShortId;
	}
}
