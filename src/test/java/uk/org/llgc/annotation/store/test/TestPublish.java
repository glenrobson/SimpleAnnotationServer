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
import java.util.Date;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Body;

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

public class TestPublish extends TestUtils {
	protected static Logger _logger = LogManager.getLogger(TestPublish.class.getName());

	public TestPublish() throws IOException {
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
	public void testPublish() throws IOException, IDConflictException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationList1.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		AnnotationList tAnnoList = _store.addAnnotationList(new AnnotationList(tAnnotationListJSON));

        for (Annotation tAnno : tAnnoList.getAnnotations()) {
            assertEquals("Unexpected canvas", "http://example.com/image1", tAnno.getTargets().get(0).getCanvas().getId());
            String tRegion = "";
            if (tAnno.getId().equals("http://example.com/annotation/1")) {
                tRegion = "xywh=0,132,102,10";
            } else {
                tRegion = "xywh=1873,132,102,10";
            }
            assertEquals("Unexpected region", tRegion, tAnno.getTargets().get(0).getRegion());
        }
	}

	@Test
	public void testCreate() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tBeforeJson = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

        Annotation tBefore = new Annotation(tBeforeJson);
		Annotation tAfter = _store.addAnnotation(tBefore);
        Map<String, Object> tAfterJson = tAfter.toJson();

        assertNotNull("Saving annotation should have created an ID", tAfterJson.get("@id"));
        assertEquals("Type different", tBeforeJson.get("@type"), tAfterJson.get("@type"));
        assertEquals("Motivation different lenghts", ((List)tBeforeJson.get("motivation")).size(), ((List)tAfterJson.get("motivation")).size());
        assertEquals("Motivation different ", ((List)tBeforeJson.get("motivation")).get(0), ((List)tAfterJson.get("motivation")).get(0));

        assertTrue("Resource should be a Map and it was " + tAfterJson.get("resource").getClass().getName(), tAfterJson.get("resource") instanceof List);
        assertEquals("Body type wrong", "dctypes:Text",((Map<String,Object>)((List)tAfterJson.get("resource")).get(0)).get("@type"));
        assertEquals("Body format wrong", "text/html",((Map<String,Object>)((List)tAfterJson.get("resource")).get(0)).get("format"));
        assertEquals("Body chars wrong", "<p>Bob Smith</p>",((Map<String,Object>)((List)tAfterJson.get("resource")).get(0)).get("chars"));
        
        assertTrue("Target (on) should be a Map", tAfterJson.get("on") instanceof Map);
        assertEquals("target type wrong", "oa:SpecificResource",((Map<String,Object>)tAfterJson.get("on")).get("@type"));
        assertEquals("target canvas wrong", "http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json",((Map<String,Object>)tAfterJson.get("on")).get("full"));
        assertEquals("target selector type wrong", "oa:FragmentSelector",((Map<String,Object>)((Map<String,Object>)tAfterJson.get("on")).get("selector")).get("@type"));
        assertEquals("target selector value wrong", "xywh=5626,1853,298,355",((Map<String,Object>)((Map<String,Object>)tAfterJson.get("on")).get("selector")).get("value"));
	}

	@Test
	public void testDelete() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));
		_store.deleteAnnotation(tAnno.getId());
		//RDFDataMgr.write(System.out, tDelModel, Lang.NQUADS);
		assertNull("Annotation should be deleted but it isn't.", _store.getAnnotation(tAnno.getId()));
        String tOrignalId = tAnno.getId();
        // test reuse of id
        tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));
        assertEquals("Couldn't re add annotation", tOrignalId, tAnno.getId());
	}

	@Test
	public void testUpdate() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

		((List<Map<String,Object>>)tAnnotationJSON.get("resource")).get(0).put("chars","<p>New String</p>");

		tAnno = _store.updateAnnotation(new Annotation(tAnnotationJSON));

        assertTrue("Resource should be a Map", tAnno.toJson().get("resource") instanceof List);
        assertEquals("Body chars wrong", "<p>New String</p>",((Map<String,Object>)((List)tAnno.toJson().get("resource")).get(0)).get("chars"));
	}

	@Test
	public void testPage() throws IOException, IDConflictException, MalformedAnnotation {
		List<Map<String, Object>> tAnnotationList = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationList2.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		for (Map<String,Object> tAnnotation : tAnnotationList) {
			_store.addAnnotation(new Annotation(tAnnotation));
		}

		AnnotationList tPageAnnos = _store.getAnnotationsFromPage(new Canvas("http://example.com/image2", ""));

        assertEquals("Different number of annotations than expected", 2, tPageAnnos.size());
        Annotation tAnno1 = tPageAnnos.get("http://example.com/annotation/2");
        assertNotNull("First anno not found", tAnno1);
        Annotation tAnno2 = tPageAnnos.get("http://example.com/annotation/3");
        assertNotNull("Second anno not found", tAnno2);
	}

	@Test
	public void testUTF8() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/utf-8.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

        Body tBody = tAnno.getBodies().get(0);
        assertEquals("UTF string didn't match.", "<p>UTF 8 test â</p>", tBody.toJson().get("chars"));
	}

	//@Test(expected=IDConflictException.class)
	@Test
	public void testDuplicate() throws IOException, IDConflictException, InterruptedException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotationId.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		Map<String, Object> tAnnotationJSON2 = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotationId.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		_store.addAnnotation(new Annotation(tAnnotationJSON));

		 Annotation tSecondAnno = _store.addAnnotation(new Annotation(tAnnotationJSON2));

         assertEquals("Unexpected ID for second Anno", "http://example.com/annotation/clash1", tSecondAnno.getId());
	}

	@Test
	public void testDates() throws IOException, IDConflictException, InterruptedException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));

		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));

        Date tCreatedDate = tAnno.getCreated();
		assertNotNull("Annotation missing created date", tCreatedDate);
        Date tOrignMod = tAnno.getModified();
		assertNotNull("Annotation is modification date",tAnno.getModified());

		((List<Map<String,Object>>)tAnnotationJSON.get("resource")).get(0).put("chars","<p>New String</p>");
        try {
            // Sleep for 1 second to ensure mod time is different
            Thread.sleep(1000);
        } catch (InterruptedException tExcpt) {
        }

		tAnno  = _store.updateAnnotation(new Annotation(tAnnotationJSON));
		assertNotNull("Annotation missing created date after update.", tAnno.getCreated());
		assertEquals("Created date is different on update.", tCreatedDate, tAnno.getCreated());

		assertNotNull("Annotation is missing modification date after update. ", tAnno.getModified());
        assertNotEquals("Modification date should have changed after update orginal: " + tOrignMod + " (" + tOrignMod.getTime() +") after update: " + tAnno.getModified() + " (" + tAnno.getModified().getTime() + ")", tOrignMod.getTime(), tAnno.getModified().getTime());

        // Check in correct place in JSON
        assertNotNull("Anno missing create date in json", tAnno.toJson().get("dcterms:created"));
        assertNotNull("Anno missing modification date in json", tAnno.toJson().get("dcterms:modified"));
	}

	@Test
	public void testInvalidAnnotation() throws IOException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/invalidAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		Model tAnnoModel = _annotationUtils.convertAnnoToModel(tAnnotationJSON);
		List<Model> tAnnotations = new ArrayList<Model>();
		tAnnotations.add(tAnnoModel);
        try {
    		List tAnnotationList = _annotationUtils.createAnnotationList(tAnnotations);

    		if (!tAnnotationList.isEmpty()) {
    			System.out.println("Annotations that made it through:");
    			System.out.println(JsonUtils.toPrettyString(tAnnotationList));
    		}
    		assertEquals("Annotations are invalid so shouldn't have created any annotations in the list.", 0, tAnnotationList.size());
        } catch (MalformedAnnotation tExcpt) {
            assertTrue("",tExcpt.getMessage().contains("n array in the on/selector/value "));
        }
	}

    @Test
	public void testBrokenAnnotation() throws IOException, IDConflictException, MalformedAnnotation {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/brokenAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
        try {
    		Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));
        } catch (MalformedAnnotation tExcpt) {
            //tExcpt.printStackTrace();
        }

        tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null));
		Annotation tAnnoModel = _store.addAnnotation(new Annotation(tAnnotationJSON));
        assertNotNull("Failed to load good annotation after bad.",tAnnoModel);
	}
}
