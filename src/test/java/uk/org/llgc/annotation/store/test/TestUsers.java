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
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.contollers.StatsService;
import uk.org.llgc.annotation.store.adapters.AbstractStoreAdapter;

import com.github.jsonldjava.utils.JsonUtils;

import java.util.Map;
import java.util.List;
import java.util.Set;

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
    public void testStoreUser() throws IOException {
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
    public void testGetUser() throws IOException {
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

}
