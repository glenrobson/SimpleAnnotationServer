package uk.org.llgc.annotation.store;

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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import java.nio.charset.Charset;

public class StoreAdapter {
	protected Dataset _dataset = null;

	public StoreAdapter(final Dataset pData) {
		_dataset = pData;
	}

	public Model addAnnotation(final Map<String,Object> pJson) throws IOException {
		String tJson = JsonUtils.toString(pJson);

		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pJson.get("@id"), tJsonLDModel);
		_dataset.commit();

		return tJsonLDModel;
	}

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException {
		List<Model> tStoredAnnotations = new ArrayList<Model>();

		_dataset.begin(ReadWrite.WRITE) ;
		for (Map<String, Object> tAnno : pJson) {
			String tJson = JsonUtils.toString(tAnno);

			Model tJsonLDModel = ModelFactory.createDefaultModel();
			RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

			_dataset.addNamedModel((String)tAnno.get("@id"), tJsonLDModel);

			tStoredAnnotations.add(tJsonLDModel);

		}
		_dataset.commit();

		return tStoredAnnotations;
	}

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		_dataset.begin(ReadWrite.WRITE); // should probably move this to deleted state
		_dataset.removeNamedModel(pAnnoId);
		_dataset.commit();
	}

	public List<Model> getAnnotationsFromPage(final String pPageId) {
		String tQueryString = "select ?annoId ?graph where {" 
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> <" + pPageId + "> ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}";

		Query tQuery = QueryFactory.create(tQueryString);
		QueryExecution tExec = QueryExecutionFactory.create(tQuery, _dataset);

		_dataset.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<Model> tAnnotations = new ArrayList<Model>();
		while (results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			Resource tAnnoId = soln.getResource("annoId") ; // Get a result variable - must be a resource

			tAnnotations.add(_dataset.getNamedModel(tAnnoId.getURI()));
		} 
		_dataset.end();

		return tAnnotations;
	}
}
