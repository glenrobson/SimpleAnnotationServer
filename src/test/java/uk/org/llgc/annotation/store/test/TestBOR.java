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
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.encoders.BookOfPeaceEncoder;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.query.* ;

public class TestBOR extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestBOR.class.getName()); 

	public TestBOR() throws IOException {
		super(new BookOfPeaceEncoder());
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
	public void testAnnotation() throws IOException, IDConflictException {
		_logger.debug("Reading annotation");
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/borAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		_logger.debug("Adding annotation");
		Model tModel = _store.addAnnotation(tAnnotationJSON);
		_logger.debug("Annotation Saved");
		
		String tAnnoId = super.getAnnoId(tModel);
		_annoIds.add(tAnnoId);
		_logger.debug("ID " + tAnnoId);
		String tQuery = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX cnt: <http://www.w3.org/2011/content#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> select ?uri ?fragment ?unit ?ship ?rank ?hometown ?name ?medal where { <" + tAnnoId + "> oa:hasTarget ?target . ?target oa:hasSource ?uri . ?target oa:hasSelector ?fragmentCont . ?fragmentCont rdf:value ?fragment . <" + tAnnoId + "> oa:hasBody ?body . ?body foaf:primaryTopic ?person . OPTIONAL { ?person <http://data.llgc.org.uk/bor/def#servedInUnit> ?unit } OPTIONAL { ?person <http://data.llgc.org.uk/bor/def#servedOnShip> ?ship } OPTIONAL { ?person <http://rdf.muninn-project.org/ontologies/military#heldRank> ?rank } OPTIONAL { ?person foaf:based_near ?hometown } OPTIONAL { ?person foaf:name ?name } OPTIONAL { ?person <http://data.llgc.org.uk/waw/def#awarded> ?medal }}";

		Query query = QueryFactory.create(tQuery) ;
		ResultSetRewindable results = null;
		boolean tHasResults = false;
		try (QueryExecution qexec = QueryExecutionFactory.create(query,tModel)) {
		
			results = ResultSetFactory.copyResults(qexec.execSelect());
			for ( ; results.hasNext() ; ) {
				tHasResults = true;
				QuerySolution soln = results.nextSolution() ;
				_logger.debug(soln.toString());

				String tURI = soln.getResource("uri").toString() + "#" + soln.getLiteral("fragment").getString();
				assertEquals("Target doesn't match", "http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json#xywh=5626,1853,298,355", tURI);

				assertEquals("Content doesn't match.", "Engr.Capt.", soln.getLiteral("rank").getString());
				assertEquals("Content doesn't match.", "Walter Ken Williams,", soln.getLiteral("name").getString());
				assertEquals("Content doesn't match.", "Cardiff", soln.getLiteral("hometown").getString());
				assertEquals("Content doesn't match.", "M.V.O", soln.getLiteral("medal").getString());
				assertEquals("Content doesn't match.", "R.N.", soln.getLiteral("unit").getString());
				assertEquals("Content doesn't match.", "Bulwark", soln.getLiteral("ship").getString());
			}
		}
		results.reset();
		assertTrue("Failed to find annotation to test with.",tHasResults);
	}

	@Test
	public void testRetrieveAnnotation() throws IOException, IDConflictException {
		_logger.debug("Reading annotation");
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/borAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		_logger.debug("Adding annotation");
		Model tModel = _store.addAnnotation(tAnnotationJSON);
		_annoIds.add(super.getAnnoId(tModel));
		_logger.debug("Annotation Saved");
		Map<String,Object> tAnno =  _annotationUtils.createAnnotationList(tModel);

		_logger.debug(JsonUtils.toPrettyString(tAnno));
		String tContent = (String)((List<Map<String,Object>>)tAnno.get("resource")).get(0).get("chars");

		assertTrue("Missing rank", tContent.contains("<span property=\"ns:rank\" class=\"rank\">Engr.Capt.</span>"));
		assertTrue("Missing name", tContent.contains("<span property=\"ns:name\" class=\"name\">Walter Ken Williams,</span>"));
		assertTrue("Missing place", tContent.contains("<span property=\"ns:place\" class=\"place\">Cardiff</span>"));
		assertTrue("Missing medal", tContent.contains("<span property=\"ns:medal\" class=\"medal\">M.V.O</span>"));
		assertTrue("Missing unit", tContent.contains("<span property=\"ns:unit\" class=\"unit\">R.N.</span>"));
		assertTrue("Missing ship", tContent.contains("<span property=\"ns:ship\" class=\"ship\">Bulwark</span>"));
	}

	@Test
	public void testAberAnno() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/aberAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		Map<String, Object> tAnnotation = _annotationUtils.createAnnotationList(tModel);

		assertNotNull("Missing Id ", tAnnotation.get("@id"));

		List<Map<String,Object>> tResources = (List<Map<String,Object>>)tAnnotation.get("resource");
		assertEquals("Not correct number of bodies", 2, tResources.size());
		for (Map<String,Object> tResource : tResources) {
			if (tResource.get("@type").equals("dctypes:Text")) {
				assertEquals("Body text no correct", "<p>18</p>", tResource.get("chars"));
			} else {
				assertEquals("tag text no correct", "age", tResource.get("chars"));
				// tag
			}
		}
		assertTrue("motivation missing commenting", ((List<String>)tAnnotation.get("motivation")).contains("oa:commenting"));
		assertTrue("motivation missing tagging ", ((List<String>)tAnnotation.get("motivation")).contains("oa:tagging"));
	}	

}	
