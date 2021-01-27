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

        // Mirador DualStrategy test:
        /*
            if (annotation.on && jQuery.isArray(annotation.on) && annotation.on.length > 0 && typeof annotation.on[0] === 'object' &&
          annotation.on[0].selector && typeof annotation.on[0].selector === 'object' &&
          annotation.on[0].selector['@type'] === 'oa:Choice' &&
          annotation.on[0].selector.default && typeof annotation.on[0].selector.default === 'object' &&
          annotation.on[0].selector.default.value && typeof annotation.on[0].selector.default.value === 'string' &&
          annotation.on[0].selector.item && typeof annotation.on[0].selector.item === 'object' &&
          annotation.on[0].selector.item.value && typeof annotation.on[0].selector.item.value === 'string'
        ) {
        return annotation.on[0].selector.default.value.indexOf('xywh=') === 0 && annotation.on[0].selector.item.value.indexOf('<svg') === 0;

        */

		// Require on to be an array
		assertTrue("On needs to be an array and it isn't.", tAnnotation.toJson().get("on") instanceof List);
		assertTrue("First object in on must be a map", ((List)tAnnotation.toJson().get("on")).get(0) instanceof Map);
        Map<String, Object> tSelector = (Map<String,Object>)((List<Map<String,Object>>)tAnnotation.toJson().get("on")).get(0).get("selector");

		assertEquals("Selector should be a choice", "oa:Choice", (String)tSelector.get("@type"));
		//System.out.println(JsonUtils.toPrettyString(tAnnotation));
		assertTrue("Default present and is Map", tSelector.get("default") instanceof Map);
		assertTrue("Default value should be String", ((Map)tSelector.get("default")).get("value") instanceof String);
		assertTrue("Selector item should be Map", tSelector.get("item") instanceof Map);
		assertTrue("Selector item value should be string", ((Map)tSelector.get("item")).get("value") instanceof String);
		assertTrue("Default should be xywh", ((String)((Map<String,Object>)tSelector.get("default")).get("value")).indexOf("xywh") != -1);
		assertTrue("Item should be svg", ((String)((Map<String,Object>)tSelector.get("item")).get("value")).indexOf("<svg") != -1);
	}
}
