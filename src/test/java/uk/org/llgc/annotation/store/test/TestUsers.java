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
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

import com.github.jsonldjava.utils.JsonUtils;

import java.net.URISyntaxException;
import java.net.URI;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.text.ParseException;

public class TestUsers extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestUsers.class.getName());

    public TestUsers() throws IOException {
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
    public void testStoreUser() throws IOException, URISyntaxException {
        User tUser = new User();
        tUser.setId("id_example");
        tUser.setShortId("short_id");
        tUser.setName("Glen");
        tUser.setEmail("glen@glen.com");
        tUser.setAuthenticationMethod("test");
        tUser.setPicture("http://picture.net");

        User tSavedUser = _store.saveUser(tUser);

        assertEquals("Saved user and original not the same", tSavedUser, tUser);
        assertFalse("User shouldn't be admin", tSavedUser.isAdmin());
    }

    @Test
    public void testGetUser() throws IOException, URISyntaxException {
        User tUser = new User();
        tUser.setId("id_example");
        tUser.setShortId("short_id");
        tUser.setName("Glen");
        tUser.setEmail("glen@glen.com");
        tUser.setAuthenticationMethod("test");
        tUser.setAdmin(true);
        tUser.setPicture("http://picture.net");
        _store.saveUser(tUser);

        User tCutdownUser = new User();
        tCutdownUser.setId("id_example");

        User tRetrieved = _store.getUser(tUser);

        assertEquals("Saved user and original not the same", tRetrieved, tUser);
        assertTrue("User should be admin", tRetrieved.isAdmin());
    }

    @Test 
    public void testAnnoCreate() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException {
        User tUser = new User();
        tUser.setId("http://example.com/user1");
        tUser.setShortId("user1");
        tUser.setName("Glen");
        tUser.setEmail("glen@glen.com");
        tUser.setAuthenticationMethod("test");
        tUser.setAdmin(true);
        tUser.setPicture("http://picture.net");
        _store.saveUser(tUser);

         Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
         Annotation tAnno = new Annotation(tAnnotation);
         tAnno.setCreator(tUser);

         assertNotNull("Creator missing in JSON", tAnno.toJson().get("dcterms:creator"));

         Annotation tStoredAnno = _store.addAnnotation(tAnno);

         assertNotNull("User should not be missing", tStoredAnno.getCreator());
         assertEquals("User not set on annotation", tUser.getId(), tStoredAnno.getCreator().getId());

    }

    @Test 
    public void testUpdatePermission() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Map<String, Object> tAnnotation2 = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Annotation tAnno1 = new Annotation(tAnnotation);
        Annotation tAnno2 = new Annotation(tAnnotation2);

        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");

        AuthorisationController tAuth = new AuthorisationController();

        // First test if the same owner of the anno is allowed to edit
        tAnno1.setCreator(tUser1);
        tAnno2.setCreator(tUser1);

        tAuth.setUser(tUser1);
        assertTrue("User who created the anno should be able to edit", tAuth.allowUpdate(tAnno1, tAnno2));
        
        // Now test a bad actor editing someone elses annotation
        tAnno1.setCreator(tUser1);// this is the anno from the store
        tAnno2.setCreator(tUser2);

        tAuth.setUser(tUser2);
        assertFalse("Shouldn't have let a different user edit the annotation", tAuth.allowUpdate(tAnno1, tAnno2));

        // Now check admin override

        tUser2.setAdmin(true);
        assertTrue("Admin should be able to edit annotation", tAuth.allowUpdate(tAnno1, tAnno2));
    }

    @Test 
    public void testDeletePermission() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Annotation tAnno1 = new Annotation(tAnnotation);

        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");

        AuthorisationController tAuth = new AuthorisationController();

        // First test if the same owner of the anno is allowed to delete it
        tAnno1.setCreator(tUser1);

        tAuth.setUser(tUser1);
        assertTrue("User who created the anno should be able to delete it", tAuth.allowDelete(tAnno1));
        
        // Now test a bad actor editing someone elses annotation
        tAnno1.setCreator(tUser1);// this is the anno from the store

        tAuth.setUser(tUser2);
        assertFalse("Shouldn't have let a different user delete the annotation", tAuth.allowDelete(tAnno1));

        // Now check admin override

        tUser2.setAdmin(true);
        assertTrue("Admin should be able to delete annotation", tAuth.allowDelete(tAnno1));
    }

    @Test 
    public void testListAnnos() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException, URISyntaxException {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Map<String, Object> tAnnotation2 = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Annotation tAnno1 = new Annotation(tAnnotation);
        Annotation tAnno2 = new Annotation(tAnnotation2);
        tAnno1.setId("http://example.com/user1/anno1");
        tAnno2.setId("http://example.com/user2/anno1");

        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        tAnno1.setCreator(tUser1);

        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");
        tAnno2.setCreator(tUser2);

        User tAdminUser = new User();
        tAdminUser.setId("http://example.com/admin");
        tAdminUser.setAdmin(true);

        _store.addAnnotation(tAnno1);
        _store.addAnnotation(tAnno2);

        Canvas tCanvas = tAnno1.getTargets().get(0).getCanvas(); // Both annotations target the same canvas

        AnnotationList tUser1Annos = _store.getAnnotationsFromPage(tUser1, tCanvas);
        AnnotationList tUser2Annos = _store.getAnnotationsFromPage(tUser2, tCanvas);
        AnnotationList tAdminAnnos = _store.getAnnotationsFromPage(tAdminUser, tCanvas);

        assertEquals("Expected 1 annotation for user 1",1, tUser1Annos.size()); 
        assertEquals("Unexpected ID for anntoation","http://example.com/user1/anno1", tUser1Annos.get(0).getId()); 

        assertEquals("Expected 1 annotation for user 2",1, tUser2Annos.size()); 
        assertEquals("Unexpected ID for anntoation","http://example.com/user2/anno1", tUser2Annos.get(0).getId()); 

        assertEquals("Expected 2 annotation for admin",2, tAdminAnnos.size()); 
        List<String> tAnnoIds = new ArrayList<String>();
        for (Annotation tAnno : tAdminAnnos.getAnnotations()) {
            tAnnoIds.add(tAnno.getId());
        }
        assertTrue("Hoped to find user1's annos in list but it wasn't found.",tAnnoIds.contains("http://example.com/user1/anno1"));
        assertTrue("Hoped to find user2's annos in list but it wasn't found.",tAnnoIds.contains("http://example.com/user2/anno1"));
    }

    @Test 
    public void testSkeleton() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Map<String, Object> tAnnotation2 = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Annotation tAnno1 = new Annotation(tAnnotation);
        tAnno1.setId("http://example.com/user1/anno1");

        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        tAnno1.setCreator(tUser1);

        Annotation tAnno2 = new Annotation(tAnnotation2);
        tAnno2.setId("http://example.com/user2/anno1");
        Manifest tSecondManifest = new Manifest();
        tSecondManifest.setURI("http://example.com/manifest2.json");
        tAnno2.getTargets().get(0).setManifest(tSecondManifest);

        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");
        tAnno2.setCreator(tUser2);

        User tAdminUser = new User();
        tAdminUser.setId("http://example.com/admin");
        tAdminUser.setAdmin(true);

        _store.addAnnotation(tAnno1);
        _store.addAnnotation(tAnno2);

        List<Manifest> tManifests = _store.getSkeletonManifests(tUser1);
        assertEquals("Expecting only 1 manifest for first user", 1, tManifests.size());
        assertEquals("Expecting a different skeleton manifest for user 1", "http://example.com/manfiest/test/manifest.json", tManifests.get(0).getURI());

        tManifests = _store.getSkeletonManifests(tUser2);
        assertEquals("Expecting only 1 manifest for second user", 1, tManifests.size());
        assertEquals("Expecting a different skeleton manifest for second user", "http://example.com/manifest2.json", tManifests.get(0).getURI());

        tManifests = _store.getSkeletonManifests(tAdminUser);
        assertEquals("Expected 2 annotation for admin",2, tManifests.size()); 
        List<String> tManifestsIds = new ArrayList<String>();
        for (Manifest tManifest : tManifests) {
            tManifestsIds.add(tManifest.getURI());
        }
        assertTrue("Hoped to find user1's annos in list but it wasn't found.",tManifestsIds.contains("http://example.com/manfiest/test/manifest.json"));
        assertTrue("Hoped to find user2's annos in list but it wasn't found.",tManifestsIds.contains("http://example.com/manifest2.json"));
    }

    @Test 
    public void testUpdateUser() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException, URISyntaxException {
        User tUser = new User();
        tUser.setId("http://example.com/user1");
        tUser.setShortId("user1");
        tUser.setName("Glen");
        tUser.setEmail("glen@glen.com");
        tUser.setAuthenticationMethod("test");
        tUser.setAdmin(true);
        tUser.setPicture("http://picture.net");
        _store.saveUser(tUser);

        User tUpdatedUser = new User();
        tUpdatedUser.setId("http://example.com/user1"); // this is the same
        tUpdatedUser.setShortId("user2");
        tUpdatedUser.setName("Bob");
        tUpdatedUser.setEmail("bob@glen.com");
        tUpdatedUser.setAuthenticationMethod("test");
        tUpdatedUser.setAdmin(false);
        tUpdatedUser.setPicture("http://picture.net/new_pic");
        _store.saveUser(tUpdatedUser);

        assertEquals("Updated user not the same as changed user", tUpdatedUser, _store.getUser(tUser));
    }

    @Test 
    public void testDates() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException {
        User tUser = new User();
        tUser.setId("http://example.com/user1");
        tUser.setShortId("user1");
        tUser.setName("Glen");
        tUser.setEmail("glen@glen.com");
        tUser.setAuthenticationMethod("test");
        tUser.setAdmin(true);
        tUser.setPicture("http://picture.net");
        assertEquals("On creation created date and last modified should be the same",tUser.getCreated(), tUser.getLastModified());
        User tStoreUser = _store.saveUser(tUser);

        User tSearchUser = new User();
        tSearchUser.setId("http://example.com/user1");
        User tUpdatedUser = _store.getUser(tSearchUser);
        assertEquals("Modification date should not be updated by get.", tUser.getLastModified(), tUpdatedUser.getLastModified());

        tUpdatedUser.setId("http://example.com/user1"); // this is the same
        tUpdatedUser.setShortId("user2");
        tUpdatedUser.setName("Bob");
        tUpdatedUser.setEmail("bob@glen.com");
        tUpdatedUser.setAuthenticationMethod("test");
        tUpdatedUser.setAdmin(false);
        tUpdatedUser.setPicture("http://picture.net/new_pic");
        tUpdatedUser = _store.saveUser(tUpdatedUser);

        tSearchUser = new User();
        tSearchUser.setId("http://example.com/user1");
        User tStoredUpdatedUser = _store.getUser(tSearchUser);

        assertEquals("Created date should be the same after update " + tUser.getCreated().getTime() + " " + tStoredUpdatedUser.getCreated().getTime(), tUser.getCreated(), tStoredUpdatedUser.getCreated());
        assertEquals("Last mod date should be updated", tUpdatedUser.getLastModified(), tStoredUpdatedUser.getLastModified());
    }

    @Test 
    public void testSearch() throws IOException, IDConflictException, MalformedAnnotation, URISyntaxException, ParseException {
        Map<String, Object> tAnnotation = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Map<String, Object> tAnnotation2 = (Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(getClass().getResource("/jsonld/testManifestWithin.json").getFile()));
        Annotation tAnno1 = new Annotation(tAnnotation);
        Annotation tAnno2 = new Annotation(tAnnotation2);
        tAnno1.setId("http://example.com/user1/anno1");
        tAnno2.setId("http://example.com/user2/anno1");

        User tUser1 = new User();
        tUser1.setId("http://example.com/user1");
        tAnno1.setCreator(tUser1);

        User tUser2 = new User();
        tUser2.setId("http://example.com/user2");
        tAnno2.setCreator(tUser2);

        User tAdminUser = new User();
        tAdminUser.setId("http://example.com/admin");
        tAdminUser.setAdmin(true);

        _store.addAnnotation(tAnno1);
        _store.addAnnotation(tAnno2);

        Manifest tManifest = tAnno1.getTargets().get(0).getManifest(); // Both annotations target the same canvas

        SearchQuery tQuery = new SearchQuery(new URI("http://example.com/search"));
        tQuery.setResultsPerPage(100);
        tQuery.setScope(tManifest.getURI());
        tQuery.addUser(tUser1);
		AnnotationList tResults1 = _store.search(tQuery);

        tQuery = new SearchQuery(new URI("http://example.com/search"));
        tQuery.setResultsPerPage(100);
        tQuery.setScope(tManifest.getURI());
        tQuery.addUser(tUser2);
		AnnotationList tResults2 = _store.search(tQuery);

        tQuery = new SearchQuery(new URI("http://example.com/search"));
        tQuery.setResultsPerPage(100);
        tQuery.setScope(tManifest.getURI());
        tQuery.addUser(tAdminUser);
		AnnotationList tAdminResults = _store.search(tQuery);

        assertEquals("Expected 1 annotation for user 1",1, tResults1.size()); 
        assertEquals("Unexpected ID for anntoation","http://example.com/user1/anno1", tResults1.get(0).getId()); 

        assertEquals("Expected 1 annotation for user 2",1, tResults2.size()); 
        assertEquals("Unexpected ID for anntoation","http://example.com/user2/anno1", tResults2.get(0).getId()); 

        assertEquals("Expected 2 annotation for admin",2, tAdminResults.size()); 
        List<String> tAnnoIds = new ArrayList<String>();
        for (Annotation tAnno : tAdminResults.getAnnotations()) {
            tAnnoIds.add(tAnno.getId());
        }
        assertTrue("Hoped to find user1's annos in list but it wasn't found.",tAnnoIds.contains("http://example.com/user1/anno1"));
        assertTrue("Hoped to find user2's annos in list but it wasn't found.",tAnnoIds.contains("http://example.com/user2/anno1"));
    }


    @Test
    public void testGetAllUser() throws IOException, URISyntaxException {
        assertTrue("Need to implement testGetAllUsers.",false);
    }
}
