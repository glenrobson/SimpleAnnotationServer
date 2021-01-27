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

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.Manifest;

import java.util.List;
import java.util.Date;

import java.net.URISyntaxException;

import java.io.IOException;

public class TestCollections extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestCollections.class.getName());
    protected User _user = null;

    public TestCollections() throws IOException {
        super();
    }

    @Before
    public void setup() throws IOException {
        super.setup();
        _user = new User();
        try { 
            _user.setId("http://example.com/user/1");
        } catch (URISyntaxException tExcpt) {
        }
        _user.setShortId("short_id");
        _user.setName("Glen");
        _user.setEmail("glen@glen.com");
        _user.setAuthenticationMethod("test");
        _user.setPicture("http://picture.net");
    }

    @After
    public void tearDown() throws IOException {
        super.tearDown();
    }

    @Test
    public void testAddCollections() throws IOException {

        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");
        
        Collection tCollection = new Collection();
        tCollection.setUser(_user);
        tCollection.setLabel("label");
        tCollection.setId("http://example.com/collection/" + _user.getShortId() + "/" + new Date().getTime() + ".json");
        tCollection.add(tManifest);
        Collection tAddedCollection = _store.createCollection(tCollection);

        // This tests the ids are equal
        assertTrue(collectionEquals(tCollection, tAddedCollection));
    }

    protected boolean collectionEquals(final Collection pCollection1, final Collection pCollection2) {
        // Test ids
        assertEquals("Added collection should be equal to created collection", pCollection1, pCollection2);
        assertEquals("Added collection label different", pCollection1.getLabel(), pCollection2.getLabel());
        assertEquals("Added collection user is different", pCollection1.getUser().getId(), pCollection2.getUser().getId());
        assertEquals("Number of manifests don't match", pCollection1.getManifests().size(), pCollection2.getManifests().size());
        for (Manifest tManifest : pCollection1.getManifests()) {
            boolean found = false;
            for (Manifest tManifest2 : pCollection2.getManifests()) {
                if (tManifest2.getURI().equals(tManifest.getURI())) {
                    found = true;
                }
            }
            assertTrue("Unable to find " + tManifest.getURI() + " in second collection", found);
        }
        
        return true;
    }

    @Test
    public void testGetCollections() throws IOException {
        List<Collection> tCollections = _store.getCollections(_user);
        assertEquals("There should be no collections but found some", 0, tCollections.size());

        Collection tCollection = new Collection();
        tCollection.setUser(_user);
        tCollection.setLabel("label");
        tCollection.createId("http://example.com/collection/");
        _store.createCollection(tCollection);

        tCollections = _store.getCollections(_user);

        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertTrue(collectionEquals(tCollection, tCollections.get(0)));
    }


    @Test
    public void testUserCollections() throws IOException {
        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        _store.createCollection(tCollection1);

        User tUser2 = new User();
        try { tUser2.setId("http://example.com/user/2"); } catch (URISyntaxException tExcpt) {}
        Collection tCollection2 = new Collection();
        tCollection2.setUser(tUser2);
        tCollection2.setLabel("label");
        tCollection2.createId("http://example.com/collection/");
        _store.createCollection(tCollection2);

        List<Collection> tCollections = _store.getCollections(_user);
        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertTrue(collectionEquals(tCollection1, tCollections.get(0)));

        tCollections = _store.getCollections(tUser2);
        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertTrue(collectionEquals(tCollection2, tCollections.get(0)));
    }

    @Test
    public void testGetCollectionById() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");
        
        Collection tCollection = new Collection();
        tCollection.setUser(_user);
        tCollection.setLabel("label");
        tCollection.setId("http://example.com/collection/" + _user.getShortId() + "/" + new Date().getTime() + ".json");
        tCollection.add(tManifest);
        String tCollectionId = tCollection.getId();
        _store.createCollection(tCollection);
        Collection tAddedCollection = _store.getCollection(tCollectionId);

        assertTrue(collectionEquals(tCollection, tAddedCollection));
    }

    @Test
    public void testUpdateLabel() throws IOException {
        // Test label update
        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("start");
        tCollection1.createId("http://example.com/collection/");
        _store.createCollection(tCollection1);

        Collection tUpdate = _store.getCollection(tCollection1.getId());
        tUpdate.setLabel("new_label");

        _store.updateCollection(tUpdate);
        Collection tUpdated = _store.getCollection(tUpdate.getId());

        assertTrue(collectionEquals(tUpdate, tUpdated));
    }

    @Test
    public void testAddManifest() throws IOException {
        // Test label update
        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        _store.createCollection(tCollection1);

        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");

        Collection tUpdate = _store.getCollection(tCollection1.getId());
        tUpdate.add(tManifest);

        _store.updateCollection(tUpdate);
        Collection tUpdated = _store.getCollection(tUpdate.getId());

        assertEquals("Collection should have added manifest", 1, tUpdate.getManifests().size());
        assertTrue(collectionEquals(tUpdate, tUpdated));
    }

    @Test
    public void testRemoveManifest() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");

        // Test label update
        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        tCollection1.add(tManifest);
        _store.createCollection(tCollection1);

        Collection tUpdate = _store.getCollection(tCollection1.getId());
        tUpdate.remove(tManifest);

        _store.updateCollection(tUpdate);
        Collection tUpdated = _store.getCollection(tUpdate.getId());

        assertEquals("Collection should be empty", 0, tUpdate.getManifests().size());
        assertTrue(collectionEquals(tUpdate, tUpdated));
    }

    @Test
    public void test2UserRemoveManifest() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");

        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        tCollection1.add(tManifest);
        _store.createCollection(tCollection1);

        User tUser2 = new User();
        try { tUser2.setId("http://example.com/user/2"); } catch (URISyntaxException tExcpt) {}
        Collection tCollection2 = new Collection();
        tCollection2.setUser(tUser2);
        tCollection2.setLabel("label");
        tCollection2.createId("http://example.com/collection/");
        tCollection2.add(tManifest);
        _store.createCollection(tCollection2);

        // Now remove manifest from first collection:
        Collection tUpdate = _store.getCollection(tCollection1.getId());
        tUpdate.remove(tManifest);
        _store.updateCollection(tUpdate);

        List<Collection> tCollections = _store.getCollections(_user);
        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertEquals("Expected empty collection.", 0, tCollections.get(0).getManifests().size());
        assertTrue(collectionEquals(tUpdate, tCollections.get(0)));

        tCollections = _store.getCollections(tUser2);
        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertEquals("User2 collection should be unchanged", 1, tCollections.get(0).getManifests().size());
        assertTrue(collectionEquals(tCollection2, tCollections.get(0)));
    }

    @Test
    public void testDelete() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");

        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        tCollection1.add(tManifest);
        _store.createCollection(tCollection1);

        _store.deleteCollection(tCollection1);
        List<Collection> tCollections = _store.getCollections(_user);
        assertEquals("User should have no collections after delete.", 0, tCollections.size());

        Collection tMissing = _store.getCollection(tCollection1.getId());
        assertNull("Collection should not be returned. Found: " + tMissing, tMissing);
    }

    @Test
    public void test2UserRemoveCollection() throws IOException {
        Manifest tManifest = new Manifest();
        tManifest.setURI("http://example.com/manifest/1");
        tManifest.setLabel("manifest");

        Collection tCollection1 = new Collection();
        tCollection1.setUser(_user);
        tCollection1.setLabel("label");
        tCollection1.createId("http://example.com/collection/");
        tCollection1.add(tManifest);
        _store.createCollection(tCollection1);

        User tUser2 = new User();
        try { tUser2.setId("http://example.com/user/2"); } catch (URISyntaxException tExcpt) {}
        Collection tCollection2 = new Collection();
        tCollection2.setUser(tUser2);
        tCollection2.setLabel("label");
        tCollection2.createId("http://example.com/collection/");
        tCollection2.add(tManifest);
        _store.createCollection(tCollection2);

        // Now remove first collection
        _store.deleteCollection(tCollection1);

        List<Collection> tCollections = _store.getCollections(_user);
        assertEquals("Expected no collections.", 0, tCollections.size());

        tCollections = _store.getCollections(tUser2);
        assertEquals("Expected only one collection.", 1, tCollections.size());
        assertEquals("User2 collection should be unchanged", 1, tCollections.get(0).getManifests().size());
        assertTrue(collectionEquals(tCollection2, tCollections.get(0)));
    }
}
