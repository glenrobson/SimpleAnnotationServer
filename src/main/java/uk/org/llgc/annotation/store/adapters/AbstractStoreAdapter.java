package uk.org.llgc.annotation.store.adapters;

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

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;

import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractStoreAdapter implements StoreAdapter {

	public List<Model> getAnnotationsFromPage(final String pPageId) throws IOException {
		String tQueryString = "select ?annoId ?graph where {" 
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> <" + pPageId + "> ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<Model> tAnnotations = new ArrayList<Model>();
		while (results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			Resource tAnnoId = soln.getResource("annoId") ; // Get a result variable - must be a resource

			tAnnotations.add(this.getNamedModel(tAnnoId.getURI()));
		} 
		this.end();

		return tAnnotations;
	}


	public Model addAnnotation(final Map<String,Object> pJson) throws IOException, IDConflictException {
		if (this.getNamedModel((String)pJson.get("@id")) != null) {
			throw new IDConflictException("There is already an annotation with id " + pJson.get("@id"));
		} else {
			return addAnnotationSafe(pJson);
		}
	}
	
	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException {
		return addAnnotationSafe(pJson);
	}

	public abstract Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException;

	public Model getAnnotation(final String pId) throws IOException {
		return getNamedModel(pId);
	}

	protected abstract Model getNamedModel(final String pName) throws IOException;
	protected abstract QueryExecution getQueryExe(final String pQuery);

	protected void begin(final ReadWrite pWrite) {
	}
	protected void end() {
	}

	public List<PageAnnoCount> listAnnoPages() {
		String tQueryString = "select ?pageId (count(?annoId) as ?count) where {" 
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}group by ?pageId order by ?pageId";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<PageAnnoCount> tAnnotations = new ArrayList<PageAnnoCount>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tPageId = soln.getResource("pageId") ; // Get a result variable - must be a resource
				int tCount = soln.getLiteral("count").getInt();
				System.out.println("Found " + tPageId + " count " + tCount);

				tAnnotations.add(new PageAnnoCount(tPageId.getURI(), tCount));
			} 
		}	
		this.end();

		return tAnnotations;
	}
}
