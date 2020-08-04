package uk.org.llgc.annotation.store.adapters;

import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import java.net.URISyntaxException;

import java.nio.charset.Charset;

public interface StoreAdapter {

	public void init(final AnnotationUtils pAnnoUtils);

    // CRUD annotations
	public Model addAnnotation(final Map<String,Object> pJson) throws IOException, IDConflictException, MalformedAnnotation;
	public Model updateAnnotation(final Map<String,Object> pJson) throws IOException, MalformedAnnotation;
	public Model getAnnotation(final String pId) throws IOException;
	public void deleteAnnotation(final String pAnnoId) throws IOException;

	public List<Model> addAnnotationList(final List<Map<String,Object>> pJson) throws IOException, IDConflictException, MalformedAnnotation;

    // CRUD manifests
	public String indexManifest(Map<String,Object> pManifest) throws IOException;
	public List<Manifest> getManifests() throws IOException;
	public List<Manifest> getSkeletonManifests() throws IOException;
	public String getManifestId(final String pShortId) throws IOException;
	public Manifest getManifest(final String pShortId) throws IOException;
	public List<String> getManifestForCanvas(final String pCanvasId) throws IOException;

    // CRUD canvas
    public Canvas resolveCanvas(final String pShortId) throws IOException;
    public void storeCanvas(final Canvas pCanvas) throws IOException;

    // Search
	public Map<String, Object> search(final SearchQuery pQuery) throws IOException;
	public List<Model> getAnnotationsFromPage(final String pPageId) throws IOException;

    // Used in ListAnnotations can we get rid?
	public Map<String, Object> getAllAnnotations() throws IOException;
	public List<PageAnnoCount> listAnnoPages() throws IOException;
    // Stats
	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) throws IOException;
}
