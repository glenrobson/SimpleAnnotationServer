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

public class TestModel extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestModel.class.getName());

	public TestModel() throws IOException {
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

	@Test
	public void loadManifest() throws IOException, IDConflictException, MalformedAnnotation {
		List<Manifest> tLoadedManifests = _store.getManifests();
		assertTrue("Store shouldn't have any manifests registered but answered " + tLoadedManifests, tLoadedManifests != null && tLoadedManifests.isEmpty());

		Map<String, Object> tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifest.json").getFile()));
		String tShortId = _store.indexManifest(tManifestJson);

        Manifest tManifest = _store.getManifest(tShortId);

        assertEquals("Indexed manifest title doesn't match the original", "http://example.com/manfiest/test/manifest.json", tManifest.getURI());
        assertEquals("Indexed manifest label doesn't match the original", "Test Manifest", tManifest.getLabel());
        assertEquals("Indexed manifest shortId doesn't match the original", tShortId, tManifest.getShortId());
        assertEquals("Unexpected amount of canvases", 2, tManifest.getCanvases().size());
        assertEquals("Wrong label for first Canvas", "Image 1", tManifest.getCanvases().get(0).getLabel());
    }    

    @Test
	public void testJsonManifest() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifest.json").getFile()));

        String tShortId = "short_id";
        Manifest tManifest = new Manifest(tManifestJson, tShortId);

        assertEquals("Indexed manifest title doesn't match the original", "http://example.com/manfiest/test/manifest.json", tManifest.getURI());
        assertEquals("Indexed manifest label doesn't match the original", "Test Manifest", tManifest.getLabel());
        assertEquals("Indexed manifest shortId doesn't match the original", tShortId, tManifest.getShortId());
        assertEquals("Unexpected amount of canvases", 2, tManifest.getCanvases().size());
        assertEquals("Wrong label for first Canvas", "Image 1", tManifest.getCanvases().get(0).getLabel());
    } 
}
