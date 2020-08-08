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
import uk.org.llgc.annotation.store.adapters.AbstractStoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Target;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.query.* ;

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
        _logger.debug("Called super()");
	}

	@Before
   public void setup() throws IOException {
		super.setup();
        _logger.debug("Called setup()");
	}

   @After
   public void tearDown() throws IOException {
		super.tearDown();
	}

	protected List<Manifest> getWithin(final Annotation pAnno) {
        List<Manifest> tWithin = new ArrayList<Manifest>();
        for (Target tTarget : pAnno.getTargets()) {
            tWithin.add(tTarget.getManifest());
        }

        return tWithin;
	}

	@Test
	public void testPassedWithin() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		Annotation tAnnotation = _store.addAnnotation(new Annotation(tAnnotationJSON));
        
		List<Manifest> tWithin = this.getWithin(tAnnotation);
        Manifest testManifest = new Manifest();
        testManifest.setURI("http://example.com/manfiest/test/manifest.json");
		assertTrue("Missing within for within annotation ", tWithin.contains(testManifest));

		tAnnotation = _store.getAnnotation("http://example.com/manifest/annotation/within");
		tWithin = this.getWithin(tAnnotation);
		assertTrue("After retreival from store Within has disappeared.", tWithin.contains(testManifest));
	}

	@Test
	public void loadManifest() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestAnno1.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

		// check no within
		assertNull("Annotation contains a within even though I haven't loaded the manifest", tAnno.getTargets().get(0).getManifest());

		List<Manifest> tLoadedManifests = _store.getManifests();
		assertTrue("Store shouldn't have any manifests registered but answered " + tLoadedManifests, tLoadedManifests != null && tLoadedManifests.isEmpty());

		Map<String, Object> tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifest.json").getFile()));
		String tShortId = _store.indexManifest(new Manifest(tManifest));

		tLoadedManifests = _store.getManifests();
		assertEquals("Store should have 1 manifests registered but answered " + tLoadedManifests, 1, tLoadedManifests.size());
		assertEquals("Store should have single manifests registered but answered " + tLoadedManifests, "http://example.com/manfiest/test/manifest.json",tLoadedManifests.get(0).getURI());
		assertEquals("Short id returned incorrect manifest", "http://example.com/manfiest/test/manifest.json", _store.getManifestId(tShortId));

		tAnno = _store.getAnnotation("http://example.com/manifest/annotation/1");
        Manifest tWithin = tAnno.getTargets().get(0).getManifest();
		assertNotNull("Missing within for loaded manifest", tWithin);
		assertEquals("Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.getURI());

		// now get annotation and see if it has a within
		tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testManifestAnno2.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

		tWithin = tAnno.getTargets().get(0).getManifest();
		assertNotNull("Missing within for second annotation ", tWithin);
		assertEquals("Second Annotation should have a within but its missing or not correct", "http://example.com/manfiest/test/manifest.json", tWithin.getURI());
	}

	@Test
	public void testSearching() throws IOException, IDConflictException, MalformedAnnotation {
        // Add two copies of the same annotation list but pointing to different Manifests
        // this checks if the scoping to manifest search is working.
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch-distraction.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(new AnnotationList(tAnnotationListJSON));
		 tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
		_store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

		SearchQuery tQuery = new SearchQuery("simple");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'simple' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'simple'","http://example.com/annotation/1", tResults.get(0).get("@id"));

		// test html
		tQuery = new SearchQuery("abold");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tResultsJson = _store.search(tQuery).toJson();

		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'abold' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'abold'","http://example.com/annotation/2", tResults.get(0).get("@id"));

		// Test multiple words:
		tQuery = new SearchQuery("Test content simple");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tResultsJson = _store.search(tQuery).toJson();

		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'Test content simple' but found different", 1, tResults.size());
		assertEquals("Expected different result for 'Test content simple'","http://example.com/annotation/1", tResults.get(0).get("@id"));
	}

    @Test
	public void testUTF8() throws IOException, IDConflictException, MalformedAnnotation {
        Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testUTF8Annotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));        

        // Load annotation
        Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

        // Test UTF-8 search
		SearchQuery tQuery = new SearchQuery("Καλημέρα κόσμε");
		tQuery.setScope("http://example.com/manfiest/utf8.json");
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'Καλημέρα κόσμε' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'simple'","http://example.com/uft8", tResults.get(0).get("@id"));
    }

     @Test
	public void testAnnoWithTag() throws IOException, IDConflictException, MalformedAnnotation {
        Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/mirador-2.1.4.json").getFile()), StoreConfig.getConfig().getBaseURI(null));        

        // Load annotation
        Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

        // Test UTF-8 search
		SearchQuery tQuery = new SearchQuery("tag");
		tQuery.setScope("http://dms-data.stanford.edu/data/manifests/BnF/jr903ng8662/manifest.json");
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 1 result for 'tag' but found different", 1, tResults.size());
		assertEquals("Expected single result for 'simple'","http://localhost:8888/annotation/1488244504042", tResults.get(0).get("@id"));
    }

    @Test
	public void testMirador() throws IOException, IDConflictException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

		SearchQuery tQuery = new SearchQuery("simple");
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

        // Mirador requires the number of results:
		assertNotNull("Missing within in result set", tResultsJson.get("within"));
        Map<String, Object> tWithin = (Map<String,Object>)tResultsJson.get("within");
		assertNotNull("Missing result count in result set", tWithin.get("total"));
		assertEquals("Start index should be 0", 0, tResultsJson.get("startIndex"));
		assertEquals("Number of results should be 1", 1, ((List<Map<String,Object>>)tResultsJson.get("resources")).size());
        Map<String, Object> tAnno = ((List<Map<String,Object>>)tResultsJson.get("resources")).get(0);
        assertTrue("Mirador requires resource to be an object. Found class " + tAnno.get("resource").getClass().getName(), tAnno.get("resource") instanceof Map);
		assertNotNull("Mirador requires a label describing a search match, using annotation.label", tAnno.get("label"));
    }

    @Test(expected = MalformedAnnotation.class)
    public void testInvalidAnnoId() throws IOException, IDConflictException, MalformedAnnotation {
        // Add two copies of the same annotation list but pointing to different Manifests
        // this checks if the scoping to manifest search is working.
        Map<String,Object> tAnnoListRaw = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/populateAnno.json").getFile()));
        String tOriginalAnnoId = (String)((List<Map<String,Object>>)tAnnoListRaw.get("resources")).get(0).get("@id");

        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/populateAnno.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
        try {
            AnnotationList tList = _store.addAnnotationList(new AnnotationList(tAnnotationListJSON)); // this should throw Exception as Anno ID isn't a valid URI

            String tId = tList.getId();
            assertEquals("Annotation ID changed on loading... ", tOriginalAnnoId, tId);
        } catch (Exception tExcpt) {
            throw tExcpt;
        }
    }

    @Test(expected = IOException.class)
	public void loadAnnoListAsManifest() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testAnnotationList1.json").getFile()));
		String tShortId = _store.indexManifest(new Manifest(tManifest));

        assertNull("Should fail to load annotation list as a manifest", tShortId);
    }


	@Test
	public void testPagination() throws IOException, IDConflictException, URISyntaxException, ParseException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

		SearchQuery tQuery = new SearchQuery(new URI("http://example.com/1/search?q=Test"));
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");
		tQuery.setResultsPerPage(10);
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

		//System.out.println(JsonUtils.toPrettyString(tResultsJson));
		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected a limit of 10 results per page.", 10, tResults.size());

		assertNotNull("Missing next link", tResultsJson.get("next"));
		assertEquals("Unexpected next link","http://example.com/1/search?q=Test&page=1",tResultsJson.get("next"));
		tQuery = new SearchQuery(new URI((String)tResultsJson.get("next")));
		tQuery.setResultsPerPage(10);
		tQuery.setScope("http://example.com/manfiest/test/manifest.json");

		tResultsJson = _store.search(tQuery).toJson();
		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Last page to have 1 result", 1, tResults.size());
		assertEquals("Number of results unexpected", 11, (int)((Map<String,Object>)tResultsJson.get("within")).get("total"));
		assertNull("Unexpected next when there isn't another result list",tResultsJson.get("next"));
		assertEquals("Unexpeceted first value", "http://example.com/1/search?q=Test", ((Map<String,Object>)tResultsJson.get("within")).get("first"));
		assertEquals("Unexpeceted last value", "http://example.com/1/search?q=Test&page=1", ((Map<String,Object>)tResultsJson.get("within")).get("last"));
	}

	@Test
	public void getAllAnnotations() throws IOException, IDConflictException, URISyntaxException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		_store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

		AnnotationList tAllAnnos =  _store.getAllAnnotations();
		assertEquals("Expected 11 results but got " + tAllAnnos.size(), 11, tAllAnnos.size());
		Map<String,String> tIds = new HashMap<String,String>();
		for (int i = 1; i < 12; i++) {
			tIds.put("http://example.com/annotation/" + i, "present");
		}

		for (Annotation tAnno : tAllAnnos.getAnnotations()) {
			assertNotNull("Unexpcted id " + tAnno.getId(), tIds.get(tAnno.getId()));
			tIds.remove(tAnno.getId());
		}

		assertEquals("Unexpected ids " + tIds.keySet(), 0, tIds.keySet().size());
	}

    @Test
    public void testEndToEnd() throws IOException, IDConflictException, URISyntaxException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/examples/anno_list.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

        // Upload Newspaper annotation list
        _store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

        // Upload Manifest
		Map<String, Object> tManifest = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/examples/Cambrian_1804-01-28.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(new Manifest(tManifest));

        SearchQuery tQuery = new SearchQuery("chimney");
		tQuery.setScope("http://dams.llgc.org.uk/iiif/newspaper/issue/3320640/manifest.json");
		Map<String, Object> tResultsJson = _store.search(tQuery).toJson();

		List<Map<String,Object>> tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		assertEquals("Expected 3 result for 'chimney' but found different", 1, tResults.size());

        // Now test a default search

        tQuery = new SearchQuery("");
		tQuery.setScope("http://dams.llgc.org.uk/iiif/newspaper/issue/3320640/manifest.json");
        tQuery.setResultsPerPage(1000);
		tResultsJson = _store.search(tQuery).toJson();
		tResults = (List<Map<String,Object>>)tResultsJson.get("resources");

		//System.out.println(JsonUtils.toPrettyString(tResultsJson));
		assertEquals("Expected 735 result for any empty search but found something different.", 735, tResults.size());
    }

    @Test
    public void testUploadOfInvalidManifest() throws IOException, IDConflictException, URISyntaxException, MalformedAnnotation {
		Map<String, Object> tManifest = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/invalidManifest.json").getFile())); //annotaiton list

        String tShortId = null;
        try {
            tShortId = _store.indexManifest(new Manifest(tManifest));
            assertNull("Invalid manifest shouldn't have been loaded but it was...", tShortId);
        } catch (IOException tException) {
            _logger.debug("Caught broken manifest");
        }

        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationListSearch.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
        // Load annotation after failed
		AnnotationList tLoaded = null;
        try {
            tLoaded = _store.addAnnotationList(new AnnotationList(tAnnotationListJSON));
        } catch (org.apache.jena.sparql.JenaTransactionException tException) {
             tException.printStackTrace();
        }
        assertNotNull("Failed to load annotation list after failed upload of manifest.", tLoaded);
    }

}
