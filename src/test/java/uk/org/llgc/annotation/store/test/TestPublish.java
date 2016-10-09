package uk.org.llgc.annotation.store.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

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

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.query.* ;

import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import java.util.Properties;

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
	public void testPublish() throws IOException, IDConflictException {
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationList1.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); //annotaiton list

		List<Model> tAnnosAsModel = _store.addAnnotationList(tAnnotationListJSON);
		// add models to single model and use sparql or something to test for valid content
		Model tMasterModel = tAnnosAsModel.get(0);
		String tOtherId = "";
		String tKnownID = "http://example.com/annotation/1";
		_annoIds.add(tKnownID);
		for (int i = 1; i < tAnnosAsModel.size(); i++) {
			StmtIterator tResults = tAnnosAsModel.get(i).listStatements(null,tMasterModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),tMasterModel.createResource("http://www.w3.org/ns/oa#Annotation"));
			Statement tResult = tResults.nextStatement();
			String tId = tResult.getSubject().toString();
			_annoIds.add(tId);
			if (!tId.equals(tKnownID)) {
				tOtherId = tId;
			}

			tMasterModel.add(tAnnosAsModel.get(i));
		}

		this.testAnnotation(tMasterModel, tKnownID, "Test content 1","http://example.com/image1#xywh=0,132,102,10"); 
		this.testAnnotation(tMasterModel, tOtherId, "Test Content 2","http://example.com/image1#xywh=1873,132,102,10"); 
	}


	@Test
	public void testCreate() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		
		_annoIds.add(super.getAnnoId(tModel));
		this.testAnnotation(tModel, "Bob Smith","http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json#xywh=5626,1853,298,355"); 
	}

	// test reuse of id
	@Test
	public void testDelete() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		String tAnnoID = super.getAnnoId(tModel);
		_annoIds.add(tAnnoID);
		_store.deleteAnnotation(tAnnoID);
		//RDFDataMgr.write(System.out, tDelModel, Lang.NQUADS);
		assertNull("Annotation should be deleted but it isn't.", _store.getAnnotation(tAnnoID));
	}

	@Test
	public void testUpdate() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		_annoIds.add(super.getAnnoId(tModel));

		_logger.debug("ID : " + (String)tAnnotationJSON.get("@id"));
		((Map<String,Object>)tAnnotationJSON.get("resource")).put("chars","<p>New String</p>");

		tModel = _store.updateAnnotation(tAnnotationJSON);

		this.testAnnotation(tModel, "New String","http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json#xywh=5626,1853,298,355"); 
	}

	@Test
	public void testPage() throws IOException, IDConflictException {
		List<Map<String, Object>> tAnnotationList = _annotationUtils.readAnnotationList(new FileInputStream(getClass().getResource("/jsonld/testAnnotationList2.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		for (Map<String,Object> tAnnotation : tAnnotationList) {
			Model tModel = _store.addAnnotation(tAnnotation);
			_annoIds.add(super.getAnnoId(tModel));
		}

		List<Model> tAnnotationsModel = _store.getAnnotationsFromPage("http://example.com/image2"); 
		Model tModel = ModelFactory.createDefaultModel();
		for (Model tModelAnno : tAnnotationsModel) {
			tModel.add(tModelAnno);
		}

		this.testAnnotation(tModel, "http://example.com/annotation/2", "Test content 1a","http://example.com/image2#xywh=0,132,102,10"); 
		this.testAnnotation(tModel, "http://example.com/annotation/3", "Test Content 2a","http://example.com/image2#xywh=1873,132,102,10"); 
	}

	@Test
	public void testUTF8() throws IOException, IDConflictException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/utf-8.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		Model tModel = _store.addAnnotation(tAnnotationJSON);
		_annoIds.add(super.getAnnoId(tModel));

		this.testAnnotation(tModel, "http://example.com/annotation/utf-8", new String("UTF 8 test â".getBytes("UTF8"),"UTF8"),"http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891217.json#xywh=5626,1853,298,355"); 
	}

	//@Test(expected=IDConflictException.class)
	@Test
	public void testDuplicate() throws IOException, IDConflictException, InterruptedException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotationId.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 
		Map<String, Object> tAnnotationJSON2 = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotationId.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 

		_store.addAnnotation(tAnnotationJSON);
		_annoIds.add((String)tAnnotationJSON.get("@id"));

		 Model tSecondAnno = _store.addAnnotation(tAnnotationJSON2);
		_annoIds.add(super.getAnnoId(tSecondAnno));

		 this.testAnnotation(tSecondAnno,"http://example.com/annotation/clash1","Bob Smith","http://dev.llgc.org.uk/iiif/examples/photos/canvas/3891216.json#xywh=5626,1853,298,355");
	}

	@Test
	public void testDates() throws IOException, IDConflictException, InterruptedException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(new FileInputStream(getClass().getResource("/jsonld/testAnnotation.json").getFile()), StoreConfig.getConfig().getBaseURI(null)); 
		
		String tAnnoId = (String)tAnnotationJSON.get("@id");
		_logger.debug("ID " + tAnnoId);
		Model tModel = _store.addAnnotation(tAnnotationJSON);
		_annoIds.add(super.getAnnoId(tModel));

		Resource tAnnoRes = tModel.getResource(tAnnoId);
		Statement tCreatedSt = tAnnoRes.getProperty(DCTerms.created);
		assertNotNull("Annotation missing created date", tCreatedSt);
		String tCreatedDate = tCreatedSt.getString();

		Statement tModifiedSt = tAnnoRes.getProperty(DCTerms.modified);
		assertNull("Annotation has a modification date and it shouldn't",tModifiedSt);

		_logger.debug("ID : " + (String)tAnnotationJSON.get("@id"));
		((Map<String,Object>)tAnnotationJSON.get("resource")).put("chars","<p>New String</p>");
		Thread.sleep(1000);

		tModel = _store.updateAnnotation(tAnnotationJSON);
		tAnnoRes = tModel.getResource(tAnnoId);
		
		tCreatedSt = tAnnoRes.getProperty(DCTerms.created);
		assertNotNull("Annotation missing created date after update.", tCreatedSt);
		assertEquals("Created date is different on update.", tCreatedDate, tCreatedSt.getString());

		tModifiedSt = tAnnoRes.getProperty(DCTerms.modified);
		assertNotNull("Annotation is missing modification date after update. ", tModifiedSt);
	}

}
