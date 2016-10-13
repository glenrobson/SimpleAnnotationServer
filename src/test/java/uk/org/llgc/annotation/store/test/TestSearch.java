package uk.org.llgc.annotation.store.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import com.github.jsonldjava.utils.JsonUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.data.SearchQuery;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.query.* ;

import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import java.util.Properties;

import java.text.ParseException;

import java.net.URISyntaxException;
import java.net.URI;

public class TestSearch extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestSearch.class.getName()); 

	public TestSearch() throws IOException {
		super();
	}

	@Before 
   public void setup() throws IOException {
		super.setup();


	}

   @After
   public void tearDown() throws IOException {
		super.tearDown();
	}

	protected List<String> getWithin(final Model pModel, final String pAnnoId) {
		String tQuery = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX dcterms: <http://purl.org/dc/terms/>  select ?within where { <" + pAnnoId + "> oa:hasTarget ?target . ?target dcterms:isPartOf ?within }";

		Query query = QueryFactory.create(tQuery) ;
		ResultSetRewindable results = null;
		List<String> tWithin = new ArrayList<String>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query,pModel)) {
		
			results = ResultSetFactory.copyResults(qexec.execSelect());
			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution() ;

				tWithin.add(soln.getResource("within").toString());
			}
		}	
		if (tWithin.isEmpty()) {
			return null;
		} else {
			return tWithin;
		}
	}

	@Test
	public void testPassedWithin() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 
		Model tModel = _store.addAnnotation(tAnnotationJSON);
		List<String> tWithin = this.getWithin(tModel, "http://example.com/manifest/annotation/within");
		assertNotNull("Missing within for second annotation ", tWithin);
		assertEquals("Second Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.get(0));

		tModel = _store.getAnnotation("http://example.com/manifest/annotation/within");
		tWithin = this.getWithin(tModel, "http://example.com/manifest/annotation/within");
		assertNotNull("Missing within for second annotation ", tWithin);
		assertEquals("Second Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.get(0));
	}	

	@Test
	public void loadManifest() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestAnno1.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 
		Model tModel = _store.addAnnotation(tAnnotationJSON);
		// check no within
	
		List<String> tWithin = this.getWithin(tModel, "http://example.com/manifest/annotation/within");
		assertNull("Annotation contains a within even though I haven't loaded the manifest", tWithin);

		List<String> tLoadedManifests = _store.getManifests();
		assertTrue("Store shouldn't have any manifests registered but answered " + tLoadedManifests, tLoadedManifests.isEmpty());

		Map<String, Object> tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifest.json").getFile()));
		String tShortId = _store.indexManifest(tManifest);


		tLoadedManifests = _store.getManifests();
		assertEquals("Store should have 1 manifests registered but answered " + tLoadedManifests, 1, tLoadedManifests.size());
		assertEquals("Store should have single manifests registered but answered " + tLoadedManifests, "http://example.com/manfiest/test/manifest.json",tLoadedManifests.get(0));
		assertEquals("Short id returned incorrect manifest", "http://example.com/manfiest/test/manifest.json", _store.getManifestId(tShortId));

		tModel = _store.getAnnotation("http://example.com/manifest/annotation/1");
		//RDFDataMgr.write(System.out, tModel, Lang.NQUADS);
		tWithin = this.getWithin(tModel, "http://example.com/manifest/annotation/1");

		assertNotNull("Missing within for first annotation ", tWithin);
		assertEquals("Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.get(0));

		// now get annotation and see if it has a within
		tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestAnno2.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 
		tModel = _store.addAnnotation(tAnnotationJSON);
		//RDFDataMgr.write(System.out, tModel, Lang.NQUADS);
		// this annotation should already have a within.
		tWithin = this.getWithin(tModel, "http://example.com/manifest/annotation/2");
		assertNotNull("Missing within for second annotation ", tWithin);
		assertEquals("Second Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.get(0));
	}

	@Test
	public void testSearching() throws IOException, IDConflictException {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(tAnnotationListJSON);


		SearchQuery tQuery = new SearchQuery("simple");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		Map<String, Object> tResultsJson = _store.search(tQuery); 

		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'simple' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'simple'","http://example.com/annotation/1", tResults.get(0).get("@id"));

		// test html 
		tQuery = new SearchQuery("abold");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tResultsJson = _store.search(tQuery); 

		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'abold' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'abold'","http://example.com/annotation/2", tResults.get(0).get("@id"));

		// Test multiple words:
		tQuery = new SearchQuery("Test content simple");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tResultsJson = _store.search(tQuery); 

		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'Test content simple' but found different", 1, tResults.size());
		assertEquals("Expected different result for 'Test content simple'","http://example.com/annotation/1", tResults.get(0).get("@id"));

	}

	@Test
	public void testPagination() throws IOException, IDConflictException, URISyntaxException, ParseException {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(tAnnotationListJSON);
		
		SearchQuery tQuery = new SearchQuery(new URI("http://example.com/1/search?q=Test"));
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tQuery.setResultsPerPage(10);
		Map<String, Object> tResultsJson = _store.search(tQuery); 

		//System.out.println(JsonUtils.toString(tResultsJson));
		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected a limit of 10 results per page.", 10, tResults.size());

		assertNotNull("Missing next link", tResultsJson.get("next"));
		assertEquals("Unexpected next link","http://example.com/1/search?q=Test&page=1",tResultsJson.get("next"));
		tQuery = new SearchQuery(new URI((String)tResultsJson.get("next")));
		tQuery.setResultsPerPage(10);
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");

		tResultsJson = _store.search(tQuery); 
		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Last page to have 1 result", 1, tResults.size());
		assertEquals("Number of results unexpected", 11, Integer.parseInt((String)((Map<String,Object>)tResultsJson.get("within")).get("total")));
		assertNull("Unexpected next when there isn't another result list",tResultsJson.get("next"));
		assertEquals("Unexpeceted first value", "http://example.com/1/search?q=Test", ((Map<String,Object>)tResultsJson.get("within")).get("first"));
		assertEquals("Unexpeceted last value", "http://example.com/1/search?q=Test&page=1", ((Map<String,Object>)tResultsJson.get("within")).get("last"));
	}

	@Test
	public void getAllAnnotations() throws IOException, IDConflictException, URISyntaxException {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(tAnnotationListJSON);
		
		Map<String, Object> tAllAnnos =  _store.getAllAnnotations();
		assertEquals("Expected 11 results but got " + ((List)tAllAnnos.get("resources")).size(), 11, ((List)tAllAnnos.get("resources")).size());
		Map<String,String> tIds = new HashMap<String,String>();
		for (int i = 1; i < 12; i++) {
			tIds.put("http://example.com/annotation/" + i, "present");
		}

		for (Map<String,Object> tAnno : (List<Map<String,Object>>)tAllAnnos.get("resources")) {
			assertNotNull("Unexpcted id " + tAnno.get("@id"), tIds.get(tAnno.get("@id")));
			tIds.remove(tAnno.get("@id"));
		}

		assertEquals("Unexpected ids " + tIds.keySet(), 0, tIds.keySet().size());
	}
}
