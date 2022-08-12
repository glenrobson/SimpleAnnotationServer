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
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.PageAnnoCount;

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
		String tShortId = _store.indexManifest(new Manifest(tManifestJson));

        Manifest tManifest = _store.getManifest((String)tManifestJson.get("@id"));

        assertNotNull("Manifest not found", tManifest);
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

    @Test
	public void testManifestLabels() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/manifests/list_label.json").getFile()));

        String tShortId = "short_id";
        Manifest tManifest = new Manifest(tManifestJson, tShortId);

        assertEquals("Manifest wasn't able to parse label", "Carex blepharicarpa Franch.", tManifest.getLabel());
    } 
    @Test
	public void testWorkshopManifest() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/manifests/workshop.json").getFile()));

        String tShortId = "short_id";
        Manifest tManifest = new Manifest(tManifestJson, tShortId);

        assertEquals("Manifest wasn't able to parse label", "Glen's fantastic October Manifest", tManifest.getLabel());
    } 


    @Test
	public void test30Manifest() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/manifests/3.0manifest.json").getFile()));

        String tShortId = "short_id";
        try {
            Manifest tManifest = new Manifest(tManifestJson, tShortId);
            assertFalse("Manifest should have thrown an IOException when supplied with a 3.0 mainfest",true);
        } catch (IOException tExcpt) {
            assertEquals("Exception should mention 3.0 error.", "SAS Currently only works with IIIF version 2.0 manifests",  tExcpt.getMessage());
        }
    } 


    @Test
    public void testSkeletonManifests() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));

        _store.addAnnotation(new Annotation(tAnnotation));
        List<Manifest> tManifests = _store.getSkeletonManifests(super.createAdminUser());
        assertEquals("Unexpected amount of manifests in store.", 1, tManifests.size());
        assertEquals("ID doesn't match", "http://example.com/manfiest/test/manifest.json", tManifests.get(0).getURI());

        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manfiest/test/manifest.json");
        List<PageAnnoCount> tAnnoList = _store.listAnnoPages(tManifest, null);
        assertEquals("Unexpected amount of annotations for this manifest.", 1, tAnnoList.size());
        assertEquals("Unexpected canvas ID", "http://example.com/manfiest/test/canvas/1.json", tAnnoList.get(0).getCanvas().getId());

        assertEquals("Incorrect short id", "857085d28ae8df537449a85b5272d516", tAnnoList.get(0).getCanvas().getShortId());
        Canvas tRetrievedCanvas = _store.resolveCanvas(tAnnoList.get(0).getCanvas().getShortId());
        assertNotNull("Failed to retrieve canvas", tRetrievedCanvas);
    }

    @Test
    public void testStoreCanvas() throws IOException, IDConflictException, MalformedAnnotation {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        _store.addAnnotation(new Annotation(tAnnotation));

        Canvas tCanvas = new Canvas("http://example.com/manfiest/test/canvas/1.json","");
        String tShortId = "857085d28ae8df537449a85b5272d516";
        assertEquals("Unexpected canvas short id", tShortId, tCanvas.getShortId());

        _store.storeCanvas(tCanvas);

        Canvas tRetrievedCanvas = _store.resolveCanvas(tShortId);
        assertNotNull("Failed to retrieve canvas", tRetrievedCanvas);

        assertEquals("Unexpected Canvas Id", "http://example.com/manfiest/test/canvas/1.json", tRetrievedCanvas.getId());
        assertEquals("Unexpected Canvas label", "", tRetrievedCanvas.getLabel());
        assertEquals("Unexpected Short Id", tShortId, tRetrievedCanvas.getShortId());

    }

    @Test
    public void testShortId() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("https://api-pre.library.tamu.edu/fcrepo/rest/mwbManifests/CofeEarHis/Full_Manifest");
        String tShortId = tManifest.getShortId();
        assertNotNull("Short id shouldn't be null",tShortId);
        assertNotEquals("Short id shouldn't be empty",tShortId, "");
    }

}
