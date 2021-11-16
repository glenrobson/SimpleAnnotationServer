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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.encoders.BookOfPeaceEncoder;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.query.* ;

public class TestSetup extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestSetup.class.getName());
    public TestSetup() throws IOException {
        super();
    }

    @Before
    public void setup() throws IOException {
        //super.setup();
    }

    @After
    public void tearDown() throws IOException {
        //super.tearDown();
        removeEnv("SAS_baseURI");
        removeEnv("SAS_encoder");
        removeEnv("SAS_store");
        removeEnv("SAS_data_dir");
        removeEnv("SAS_repo_url");
        removeEnv("SAS_solr_connection");
        removeEnv("SAS_solr_collection");
        removeEnv("SAS_admin");
    }

    @Test
    public void testEnviormentOveride() throws IOException, IDConflictException {
        Properties tProps = new Properties();
        tProps.setProperty("baseURI","properties_baseuri");
        tProps.setProperty("encoder", "properties_encoder");
        tProps.setProperty("store", "properties_store");
        tProps.setProperty("data_dir", "properties_data_dir");
        tProps.setProperty("repo_url", "properties_repo_url");
        tProps.setProperty("solr_connection", "properties_solr_connection");
        tProps.setProperty("solr_collection", "properties_solr_collection");
        tProps.setProperty("admin", "prop_admin");

        setEnv("SAS_baseURI","env_baseuri");
        setEnv("SAS_encoder","env_encoder");
        setEnv("SAS_store","env_store");
        setEnv("SAS_data_dir","env_data_dir");
        setEnv("SAS_repo_url","env_repo_url");
        setEnv("SAS_solr_connection","env_solr_connection");
        setEnv("SAS_solr_collection","env_solr_collection");
        setEnv("SAS_admin","env_admin");
        StoreConfig tConfig = new StoreConfig(tProps);

        Map<String, String> tNewProps = tConfig.getProps();
        assertEquals("baseuri hasn't been picked up from the Enviroment.","env_baseuri", tNewProps.get("baseURI"));
        assertEquals("encoder hasn't been picked up from the Enviroment.","env_encoder", tNewProps.get("encoder"));
        assertEquals("store hasn't been picked up from the Enviroment.","env_store", tNewProps.get("store"));
        assertEquals("data_dir hasn't been picked up from the Enviroment.","env_data_dir", tNewProps.get("data_dir"));
        assertEquals("repo_url hasn't been picked up from the Enviroment.","env_repo_url", tNewProps.get("repo_url"));
        assertEquals("solr_connection hasn't been picked up from the Enviroment.","env_solr_connection", tNewProps.get("solr_connection"));
        assertEquals("solr_collection hasn't been picked up from the Enviroment.","env_solr_collection", tNewProps.get("solr_collection"));
        assertEquals("admin email hasn't been picked up from the Enviroment.","env_admin", tNewProps.get("admin"));
    }
    private void setEnv(String key, String value) {
        System.setProperty(key, value);
    }
    private void removeEnv(String key) {
        System.clearProperty(key);
    }
}
