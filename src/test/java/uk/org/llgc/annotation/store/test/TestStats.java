package uk.org.llgc.annotation.store.test;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.primefaces.model.chart.PieChartModel;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.users.LocalUser;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.controllers.StatsService;

import com.github.jsonldjava.utils.JsonUtils;

import java.net.URISyntaxException;

import java.util.Map;
import java.util.List;
import java.util.Set;

public class TestStats extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestSearch.class.getName());

    public TestStats() throws IOException {
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

    @Test
    public void listManifest() throws IOException, IDConflictException, MalformedAnnotation {
        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_AnnotationList.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

        // Upload Newspaper annotation list
        AnnotationList tList = new AnnotationList(tAnnotationListJSON);
        _store.addAnnotationList(tList);

        // Upload Manifest
		Map<String, Object> tManifest = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(new Manifest(tManifest));

        List<Manifest> tManifests = _store.getManifests();
        assertEquals("Found " + tManifests.size() + " expected 1", 1, tManifests.size());

        assertEquals("Found different manifest that expected.", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/manifest.json", tManifests.get(0).getURI());
    }

    @Test
    public void testStats() throws IOException, IDConflictException, MalformedAnnotation {
        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_AnnotationList.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
        // Upload Newspaper annotation list
        AnnotationList tList = new AnnotationList(tAnnotationListJSON);
        _store.addAnnotationList(tList);

        // Upload unrelated annotation list to check counts
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        _store.addAnnotation(new Annotation(tAnnotation));

        // Upload Manifest
		Map<String, Object> tManifestJson = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(new Manifest(tManifestJson));
        Manifest tManifest = new Manifest(tManifestJson, tShortId);
        StatsService tStats = new StatsService();
        tStats.init(_annotationUtils);

        List<PageAnnoCount> tPageCounts = tStats.getAnnoCountData(tManifest, null);
        assertEquals("Expected size of 1.", 1, tPageCounts.size());
        assertEquals("Expected 8 annotations for the first page.", 8, tPageCounts.get(0).getCount());

        // Now test how many images are left to do.
        PieChartModel tModel = tStats.getPercentAnnotated(tManifest, null);
        assertEquals("Expected a pie chart of size 2",tModel.getData().size(), 2);
        Set<String> tKeys = tModel.getData().keySet();
        int tDone = 0;
        int tToDo = 0;
        for (String tKey : tKeys) {
            if (tKey.contains("Canvases with annotations")) {
                tDone = tModel.getData().get(tKey).intValue();
            } else {
                tToDo = tModel.getData().get(tKey).intValue();
            }
        }
        
        assertEquals("Expected done to be 1 page.", tDone, 1);
        assertEquals("Expected to be done to be 1 page.", tToDo, 1);
        assertEquals("Expected to be done and done to add up to the number of canvases", tToDo + tDone, tManifest.getCanvases().size());

        PageAnnoCount tPage1 = tPageCounts.get(0);
        assertEquals("Mistmatch with canvas label ",  "[Aberystwyth] - page 1", tPage1.getCanvas().getLabel());
        assertEquals("Mistmatch between annotation count and expected manifest", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/manifest.json", tPage1.getManifest().getURI());
        assertEquals("Mistmatch between expected Manifest label", "[Aberystwyth]", tPage1.getManifest().getLabel());
    }

    @Test
    public void testTopLevel() throws IOException, IDConflictException, MalformedAnnotation {
         List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_AL_MultipleCanvas.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
        
        AnnotationList tList = new AnnotationList(tAnnotationListJSON);
        _store.addAnnotationList(tList);

        // 5 anno canvas, 8 annotations

		Map<String, Object> tManifest = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        String tShortId = _store.indexManifest(new Manifest(tManifest));

        assertEquals("Unexpected number of annos", 8, _store.getTotalAnnotations(null));
        assertEquals("Unexpected number of canvases", 5, _store.getTotalAnnoCanvases(null));
        assertEquals("Unexpected number of Manifests", 1, _store.getTotalManifests(null));
    }


    @Test
    public void testUserStats() throws IOException, URISyntaxException {
        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        tUser1.setShortId("user1");
        tUser1.setName("Glen");
        tUser1.setEmail("glen@glen.com");
        tUser1.setAuthenticationMethod("google");
        _store.saveUser(tUser1);

        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");
        tUser2.setShortId("user2");
        tUser2.setName("Glen");
        tUser2.setEmail("glen@glen.com");
        tUser2.setAuthenticationMethod("github");
        _store.saveUser(tUser2);

        User tUser3 = new User();
        tUser3.setId("http://example.com/user3");
        tUser3.setShortId("user3");
        tUser3.setName("Glen");
        tUser3.setEmail("glen@glen.com");
        tUser3.setAuthenticationMethod(LocalUser.AUTH_METHOD);
        _store.saveUser(tUser3);


        Map<String,Integer> tAuthMethods = _store.getTotalAuthMethods();

        assertEquals("Unexpected number of Github users", new Integer(1), tAuthMethods.get("github"));
        assertEquals("Unexpected number of Google users", new Integer(1), tAuthMethods.get("google"));
        assertEquals("Unexpected number of Local users", new Integer(1), tAuthMethods.get(LocalUser.AUTH_METHOD));
    }

    @Test
    public void testUserListAnnoPages() throws IOException, URISyntaxException, IDConflictException, MalformedAnnotation  {
        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        tUser1.setShortId("user1");
        tUser1.setName("Glen");
        tUser1.setEmail("glen@glen.com");
        tUser1.setAuthenticationMethod("google");
        _store.saveUser(tUser1);

        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");
        tUser2.setShortId("user2");
        tUser2.setName("Glen");
        tUser2.setEmail("glen@glen.com");
        tUser2.setAuthenticationMethod("github");
        _store.saveUser(tUser2);

		Map<String, Object> tManifestJson = (Map<String, Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/stats_manifest.json").getFile())); //annotaiton list
        Manifest tManifest = new Manifest(tManifestJson);
        String tShortId = _store.indexManifest(tManifest);

        List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/stats_2canvas_annolist.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list
        // Canvas1: https://damsssl.llgc.org.uk/iiif/2.0/1132230/canvas/1132230.json
        // Canvas2: https://damsssl.llgc.org.uk/iiif/2.0/1132232/canvas/1132232.json
        Annotation tAnnoUser1 = new Annotation(tAnnotationListJSON.get(0));
        tAnnoUser1.setCreator(tUser1);
        _store.addAnnotation(tAnnoUser1);

        Annotation tAnnoUser2 = new Annotation(tAnnotationListJSON.get(1));
        tAnnoUser2.setCreator(tUser2);
        _store.addAnnotation(tAnnoUser2);

        List<PageAnnoCount> tPages = _store.listAnnoPages(tManifest, tUser1); // TODO need to pass user1 here

        assertEquals("For user1 there should only be 1 page annotated.", 1, tPages.size());
        assertEquals("For user1 annotation pointing to the wrong page", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/canvas/1132232.json", tPages.get(0).getCanvas().getId());
        assertEquals("There should only be 1 annotation For user1", 1, tPages.get(0).getCount());

        tPages = _store.listAnnoPages(tManifest, tUser2); // TODO need to pass user2 here

        assertEquals("For user2 there should only be 1 page annotated.", 1, tPages.size());
        assertEquals("For user2 annotation pointing to the wrong page", "https://damsssl.llgc.org.uk/iiif/2.0/1132230/canvas/1132230.json", tPages.get(0).getCanvas().getId());
        assertEquals("There should only be 1 annotation For user2", 1, tPages.get(0).getCount());
    }
}
