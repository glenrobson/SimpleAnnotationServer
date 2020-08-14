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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Mirador214;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.Annotation;

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

import java.net.URISyntaxException;

public class TestMirador214 extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestMirador214.class.getName());

	public TestMirador214() throws IOException {
		super(new Mirador214());
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
	public void testMirador214() throws IOException, IDConflictException, InterruptedException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/mirador-2.1.4.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		String tAnnoId = (String)tAnnotationJSON.get("@id");
		_logger.debug("ID " + tAnnoId);
		Annotation tAnnotation = _store.addAnnotation(new Annotation(tAnnotationJSON));

		// Require on to be an array
		assertTrue("On needs to be an array and it isn't.", tAnnotation.toJson().get("on") instanceof List);

		//System.out.println(JsonUtils.toPrettyString(tAnnotation));
		assertNotNull("Default rect present ", (((Map<String,String>)((Map<String, Object>)tAnnotation.getTargets().get(0).toJson().get("selector")).get("default")).get("value")));
	}
}
