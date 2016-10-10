package uk.org.llgc.annotation.store.adapters;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;

import com.github.jsonldjava.core.JsonLdError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public abstract class AbstractRDFStore extends AbstractStoreAdapter {
	protected static Logger _logger = LogManager.getLogger(AbstractRDFStore.class.getName()); 
	public List<Model> getAnnotationsFromPage(final String pPageId) throws IOException {
		String tQueryString = "select ?annoId ?graph where {" 
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> <" + pPageId + "> ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}";

	//	_logger.debug("Query " + tQueryString);
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


	public List<String> getManifests() throws IOException {
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
										"select ?manifest where {"  + 
										" GRAPH ?graph {?manifest <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://iiif.io/api/presentation/2#Manifest> }}";
		

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		List<String> tManifests = new ArrayList<String>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifest") ; // Get a result variable - must be a resource

				_logger.debug("Found manifest " + tManifestURI.getURI());
				tManifests.add(tManifestURI.getURI());
			} 
		} else {
			_logger.debug("no Manifests loaded");
		}
		this.end();

		return tManifests;

	}

	public String getManifestId(final String pShortId) throws IOException {
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" 
								  + "select ?manifest where { "
								  + "GRAPH ?graph { ?manifest rdf:type <http://iiif.io/api/presentation/2#Manifest> . "
								  + "?manifest <http://purl.org/dc/elements/1.1/identifier> '" + pShortId + "' "
								  + "}}";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		String tManifest = "";
		_logger.debug("Results " + results);
		if (results != null) {
			if (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifest") ; // Get a result variable - must be a resource

				tManifest = tManifestURI.getURI();
			} else {
				this.end();
				_logger.debug("Manifest with short id " + pShortId + " not found");
				return null;
			}
		}	
		this.end();

		return tManifest;

	}

	public Map<String,Object> getManifest(final String pShortId) throws IOException {
		String tManifestURI = this.getManifestId(pShortId);
		if (tManifestURI == null || tManifestURI.trim().length() == 0) {
			_logger.debug("Manifest URI not found for short id " + pShortId);
			return null;
		}	
		Model tModel = this.getNamedModel(tManifestURI);

		Map<String,Object> tJson = null;
		try {
			_annoUtils.frameManifest(tModel);
		} catch (JsonLdError tException) {
			throw new IOException("Failed to convert manifest to JsonLd due to "+ tException.toString());
		}
		return tJson;
	}

	public Map<String, Object> search(final SearchQuery pQuery) throws IOException {
		String tQueryString = "PREFIX oa: <http://www.w3.org/ns/oa#> " 
									 + "PREFIX cnt: <http://www.w3.org/2011/content#> "
									 + "select ?anno ?content ?graph where { "
									 + "  GRAPH ?graph { ?anno oa:hasTarget ?target . "
									 + "  ?anno oa:hasBody ?body . "
									 + "  ?body <" + super.FULL_TEXT_PROPERTY + "> ?content ."
									 + "  FILTER regex(str(?content), \".*" + pQuery.getQuery() + ".*\")"
									 + "  }"
									 + "} ORDER BY ?anno";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		Map<String,Object> tAnnotationList = new HashMap<String,Object>();
		tAnnotationList.put("@context", "http://iiif.io/api/presentation/2/context.json");
		tAnnotationList.put("@type", "sc:AnnotationList");

		List<Map<String,Object>> tResources = new ArrayList<Map<String,Object>>();
		tAnnotationList.put("resources", tResources);
		this.begin(ReadWrite.READ);
		List<QuerySolution> tResults = ResultSetFormatter.toList(tExec.execSelect());
		this.end();
		try {
			if (tResults != null) {
				int tStart = pQuery.getIndex();
				int tEnd = tStart + pQuery.getResultsPerPage();
				if (tEnd > tResults.size()) {
					tEnd = tResults.size();
				}
				int tResultNo = tResults.size();
				if (tResultNo > pQuery.getResultsPerPage()) { // if paginating
					Map<String,String> tWithin = new HashMap<String,String>();
					tAnnotationList.put("within",tWithin);
					tWithin.put("@type","sc:Layer");
					tWithin.put("total","" + tResults.size());
					int tNumberOfPages = (int)(tResults.size() / pQuery.getResultsPerPage());
					int tPageNo = pQuery.getPage();
					if (tNumberOfPages != pQuery.getPage()) { // not on last page
						int tPage = tPageNo + 1;
						pQuery.setPage(tPage);
						tAnnotationList.put("next",pQuery.toURI().toString());
					}
					pQuery.setPage(0);
					tWithin.put("first", pQuery.toURI().toString());
					pQuery.setPage(tNumberOfPages);
					tWithin.put("last", pQuery.toURI().toString());
				}
				for (int i = tStart; i < tEnd; i++) {
					QuerySolution soln = tResults.get(i);
					Resource tAnnoURI = soln.getResource("anno") ; // Get a result variable - must be a resource

					Model tAnno = this.getNamedModel(tAnnoURI.getURI());

					Map<String,Object> tJsonAnno = _annoUtils.frameAnnotation(tAnno, true);

					tResources.add(tJsonAnno);
				} 
			}	
		} catch (URISyntaxException tException) {	
			throw new IOException("Failed to work with base URI " + tException.toString());
		} catch (JsonLdError tException) {
			throw new IOException("Failed to generate annotation list due to " + tException.toString());
		}	

		return tAnnotationList;
	}


	public Map<String, Object> getAllAnnotations() throws IOException {
		// get all annotations but filter our manifest annotations
		String tQueryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
									 "select ?anno where { " +
									 "GRAPH ?graph {?anno rdf:type <http://www.w3.org/ns/oa#Annotation> . " +
								    "FILTER NOT EXISTS {?canvas rdf:first ?anno} " +
							 	    "}}";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		int i = 0;
		Map<String,Object> tAnnotationList = new HashMap<String,Object>();
		tAnnotationList.put("@context", "http://iiif.io/api/presentation/2/context.json");
		tAnnotationList.put("@type", "sc:AnnotationList");

		List<Map<String,Object>> tResources = new ArrayList<Map<String,Object>>();
		tAnnotationList.put("resources", tResources);

		try {
			if (results != null) {
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution() ;
					Resource tAnnoURI = soln.getResource("anno") ; // Get a result variable - must be a resource

					Model tAnno = this.getNamedModel(tAnnoURI.getURI());

					Map<String,Object> tJsonAnno = _annoUtils.frameAnnotation(tAnno, false);

					tResources.add(tJsonAnno);
				} 
			}	
		} catch (JsonLdError tException) {
			throw new IOException("Failed to generate annotation list due to " + tException.toString());
		} finally {
			this.end();
		}	

		return tAnnotationList;
	}

	public List<PageAnnoCount> listAnnoPages() {
		String tQueryString = "select ?pageId (count(?annoId) as ?count) where {" 
										+ " GRAPH ?graph { ?on <http://www.w3.org/ns/oa#hasSource> ?pageId ."
										+ " ?annoId <http://www.w3.org/ns/oa#hasTarget> ?on } "
									+ "}group by ?pageId order by ?pageId";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
		int i = 0;
		List<PageAnnoCount> tAnnotations = new ArrayList<PageAnnoCount>();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tPageId = soln.getResource("pageId") ; // Get a result variable - must be a resource
				int tCount = soln.getLiteral("count").getInt();
				_logger.debug("Found " + tPageId + " count " + tCount);

				tAnnotations.add(new PageAnnoCount(tPageId.getURI(), tCount));
			} 
		}	

		return tAnnotations;
	}

	protected QueryExecution getQueryExe(final String pQuery) {
		throw new UnsupportedOperationException("Either getQueryExe must be implemented in a subclass or you should overload listAnnoPages and getAnnotationsFromPage");
	}

	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException {
		String tQueryString =   "PREFIX oa: <http://www.w3.org/ns/oa#> " + 
										"PREFIX sc: <http://iiif.io/api/presentation/2#> " + 
										"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
										"PREFIX dcterms: <http://purl.org/dc/terms/>" + 
										"select ?manifest where {" + 
										"   GRAPH ?graph { " +
										"	 ?manifest sc:hasSequences ?seqence ." + 
										"	 ?seqence ?sequenceCount ?seqenceId ." + 
										"	 ?seqenceId rdf:type sc:Sequence ." + 
										"	 ?seqenceId sc:hasCanvases ?canvasList ." + 
										"	 ?canvasList rdf:rest*/rdf:first <" + pCanvasId + "> " + 
										"   } " +
										"}";

		QueryExecution tExec = this.getQueryExe(tQueryString);
		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
		List<String> tParents = new ArrayList();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tManifestURI = soln.getResource("manifest"); 

				tParents.add(tManifestURI.toString());
			} 
		}	

		if (tParents.isEmpty()) {
			return null;
		} else {
			return tParents;
		}	
	}

	protected abstract String indexManifestOnly(final String pShortId, Map<String,Object> pManifest) throws IOException;

	protected String indexManifestNoCheck(final String pShortId, Map<String,Object> pManifest) throws IOException {
		String tShortId = this.indexManifestOnly(pShortId, pManifest);
		String tManifestURI = (String)pManifest.get("@id");
		// Now update any annotations which don't contain a link to this manifest.
		String tQueryString =   "PREFIX oa: <http://www.w3.org/ns/oa#> " + 
										"PREFIX sc: <http://iiif.io/api/presentation/2#> " + 
										"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
										"PREFIX dcterms: <http://purl.org/dc/terms/>" + 
										"select ?anno where {" + 
										"   GRAPH ?graph2 { " +
										"	 <" + tManifestURI + "> sc:hasSequences ?seqence ." + 
										"	 ?seqence ?sequenceCount ?seqenceId ." + 
										"	 ?seqenceId rdf:type sc:Sequence ." + 
										"	 ?seqenceId sc:hasCanvases ?canvasList ." + 
										"	 ?canvasList rdf:rest*/rdf:first ?canvas " + 
										"   } " +
										"	 GRAPH ?graph {" + 
										"		?source oa:hasSource ?canvas ." + 
										"		?anno oa:hasTarget ?source ." + 
										"		  filter not exists {?source dcterms:isPartOf <" + tManifestURI + "> }" + 
										"  }" + 
										"}";

		QueryExecution tExec = this.getQueryExe(tQueryString);

		this.begin(ReadWrite.READ);
		ResultSet results = tExec.execSelect(); // Requires Java 1.7
		this.end();
		if (results != null) {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution() ;
				Resource tAnno = soln.getResource("anno") ; // Get a result variable - must be a resource
				Model tAnnoModel = this.getNamedModel(tAnno.toString());

				// add within 
				Map<String,Object> tJsonAnno = null;
				try {
					tJsonAnno = _annoUtils.frameAnnotation(tAnnoModel, false);
				} catch (JsonLdError tException) {
					throw new IOException("Failed to convert annotation to json for " + tAnno.toString() + " due to " + tException.toString());
				}
				super.addWithin(tJsonAnno, tManifestURI);

				super.updateAnnotation(tJsonAnno);
			} 

		} else {
			// found no annotations that weren't linked to this manifest
		}

		return tShortId;
	}	
}
