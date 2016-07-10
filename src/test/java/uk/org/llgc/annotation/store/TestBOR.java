package uk.org.llgc.annotation.store;

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

public class TestBOR {
	protected static Logger _logger = LogManager.getLogger(TestBOR.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;
	//@Rule
	protected File _testFolder = null;

	public TestBOR() throws IOException {
		super();
		_testFolder = new File(new File(getClass().getResource("/").toString()),"tmp");
		_annotationUtils = new AnnotationUtils(new File(getClass().getResource("/contexts").getFile()), new BookOfPeaceEncoder());
	}

	@Before 
   public void setup() throws IOException {
		Map<String,String> tProps = new HashMap<String,String>(); 
		tProps.put("store","jena");
		File tDataDir = new File(_testFolder, "data");
		tDataDir.mkdirs();
		tProps.put("data_dir",tDataDir.getPath());
		tProps.put("baseURI","http://dev.llgc.org.uk/annotation/");

		StoreConfig tConfig = new StoreConfig(tProps);
		StoreConfig.initConfig(tConfig);
		_store = StoreConfig.getConfig().getStore();
	}

   @After
   public void tearDown() throws IOException {
		File tDataDir = new File(_testFolder, "data");
		this.delete(tDataDir);
	}

	protected void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				this.delete(c);
			}	
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}	
	}


	@Test
	public void testAnnotation() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/borAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		
		String tQuery = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX cnt: <http://www.w3.org/2011/content#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> select ?uri ?fragment ?unit ?ship ?rank ?hometown ?name ?medal where { <http://example.com/anno/bor/1> oa:hasTarget ?target . ?target oa:hasSource ?uri . ?target oa:hasSelector ?fragmentCont . ?fragmentCont rdf:value ?fragment . <http://example.com/anno/bor/1> oa:hasBody ?body . ?body foaf:primaryTopic ?person . OPTIONAL { ?person <http://data.llgc.org.uk/bor/def#servedInUnit> ?unit } OPTIONAL { ?person <http://data.llgc.org.uk/bor/def#servedOnShip> ?ship } OPTIONAL { ?person <http://rdf.muninn-project.org/ontologies/military#heldRank> ?rank } OPTIONAL { ?person foaf:based_near ?hometown } OPTIONAL { ?person foaf:name ?name } OPTIONAL { ?person <http://data.llgc.org.uk/waw/def#awarded> ?medal }}";

		Query query = QueryFactory.create(tQuery) ;
		ResultSetRewindable results = null;
		try (QueryExecution qexec = QueryExecutionFactory.create(query,tModel)) {
		
			results = ResultSetFactory.copyResults(qexec.execSelect());
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution() ;

				String tURI = soln.getResource("uri").toString() + "#" + soln.getLiteral("fragement");
				assertEquals("Target doesn't match", "http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json#xywh=5626,1853,298,355", tURI);

				assertEquals("Content doesn't match.", "Engr.Capt.", soln.getLiteral("rank").toString());
				assertEquals("Content doesn't match.", "Walter Ken Williams,", soln.getLiteral("name").toString());
				assertEquals("Content doesn't match.", "Cardiff", soln.getLiteral("hometown").toString());
				assertEquals("Content doesn't match.", "M.V.O", soln.getLiteral("medal").toString());
				assertEquals("Content doesn't match.", "R.N.", soln.getLiteral("unit").toString());
				assertEquals("Content doesn't match.", "Bulwark", soln.getLiteral("ship").toString());
			}
		}
		results.reset();

	}

	@Test
	public void testRetrieveAnnotation() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/borAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
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
}	
