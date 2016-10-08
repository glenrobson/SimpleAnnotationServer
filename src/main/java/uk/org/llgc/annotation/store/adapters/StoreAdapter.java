package uk.org.llgc.annotation.store.adapters;

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import java.nio.charset.Charset;

public interface StoreAdapter {

	public void init(final AnnotationUtils pAnnoUtils);
	public Model addAnnotation(final Map<String,Object> pJson) throws IOException, IDConflictException;
	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException;

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException, IDConflictException;

	public String indexManifest(Map<String,Object> pManifest) throws IOException;
	public List<String> getManifests() throws IOException;
	public String getManifestId(final String pShortId) throws IOException;
	public Map<String,Object> getManifest(final String pShortId) throws IOException;
	public Map<String, Object> getAllAnnotations() throws IOException;
	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException;

	public Map<String, Object> search(final SearchQuery pQuery) throws IOException;

	/**
	 * Return the annotaiton with the given id
	 * @return the annotation with id. If there is no annotation with that id return null
	 */
	public Model getAnnotation(final String pId) throws IOException;

	public void deleteAnnotation(final String pAnnoId) throws IOException;

	public List<Model> getAnnotationsFromPage(final String pPageId) throws IOException;

	public List<PageAnnoCount> listAnnoPages() throws IOException;
}
