package uk.org.llgc.annotation.store.adapters;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.util.List;

import java.io.IOException;

public interface StoreAdapter {

	public void init(final AnnotationUtils pAnnoUtils);

    // CRUD annotations
	public Annotation addAnnotation(final Annotation pJson) throws IOException, IDConflictException, MalformedAnnotation;
	public Annotation updateAnnotation(final Annotation pJson) throws IOException, MalformedAnnotation;
	public Annotation getAnnotation(final String pId) throws IOException;
	public void deleteAnnotation(final String pAnnoId) throws IOException;

	public AnnotationList addAnnotationList(final AnnotationList pJson) throws IOException, IDConflictException, MalformedAnnotation;

    // CRUD manifests
	public String indexManifest(final Manifest pManifest) throws IOException;
	public List<Manifest> getManifests() throws IOException;
	public List<Manifest> getSkeletonManifests() throws IOException;
	public String getManifestId(final String pShortId) throws IOException;
	public Manifest getManifest(final String pShortId) throws IOException;
	public Manifest getManifestForCanvas(final Canvas pCanvasId) throws IOException;

    // CRUD canvas
    public Canvas resolveCanvas(final String pShortId) throws IOException;
    public void storeCanvas(final Canvas pCanvas) throws IOException;

    // Search
	public IIIFSearchResults search(final SearchQuery pQuery) throws IOException;
	public AnnotationList getAnnotationsFromPage(final Canvas pPage) throws IOException;

    // Used in ListAnnotations can we get rid?
	public AnnotationList getAllAnnotations() throws IOException;
	public List<PageAnnoCount> listAnnoPages() throws IOException;
    // Stats
	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) throws IOException;
}
