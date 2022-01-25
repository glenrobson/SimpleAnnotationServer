package uk.org.llgc.annotation.store.adapters;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.IIIFSearchResults;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.AnnotationUtils;

import java.util.List;
import java.util.Map;

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
    /**
     * Check for any canvases that have been annotated before the Manifest was loaded
     * Call after index Manifest
     */ 
    public void linkupOrphanCanvas(final Manifest pManifest) throws IOException;

	public List<Manifest> getManifests() throws IOException;
	public List<Manifest> getSkeletonManifests(final User pUser) throws IOException;
    public String getManifestId(final String pShortId) throws IOException;
	public Manifest getManifest(final String pId) throws IOException;
	public Manifest getManifestForCanvas(final Canvas pCanvasId) throws IOException; 

    // CRUD canvas
    public Canvas resolveCanvas(final String pShortId) throws IOException;
    public void storeCanvas(final Canvas pCanvas) throws IOException;

    // Search
	public IIIFSearchResults search(final SearchQuery pQuery) throws IOException; // TODO
	public AnnotationList getAnnotationsFromPage(final User pUser, final Canvas pPage) throws IOException;

    // CRUD users
    /**
     * This will save the user if it doesnt exist
     */
    public User retrieveUser(final User pUser) throws IOException;
    /**
     * This will get the user using the ID
     */
    public User getUser(final User pUser) throws IOException;
    public User saveUser(final User pUser) throws IOException;
    public User deleteUser(final User pUser) throws IOException;
    public List<User> getUsers() throws IOException;
    public List<User> getUsers(final String pGroup) throws IOException;

    // CRUD Collections
    public Collection createCollection(final Collection pCollection) throws IOException;
    public List<Collection> getCollections(final User pUser) throws IOException;
    public Collection getCollection(final String pId) throws IOException;
    public void deleteCollection(final Collection pCollection) throws IOException;
    public void updateCollection(final Collection pCollection) throws IOException;

    // Used in ListAnnotations can we get rid?
	public AnnotationList getAllAnnotations() throws IOException;
    // Stats
    // If user is null then all user annotations will be returned
	public List<PageAnnoCount> listAnnoPages(final Manifest pManifest, final User pUser) throws IOException; // TODO

    // Pass in null user to get total annotations
    public int getTotalAnnotations(final User pUser) throws IOException;
    public int getTotalManifests(final User pUser) throws IOException;
    public int getTotalAnnoCanvases(final User pUser) throws IOException;
    public Map<String,Integer> getTotalAuthMethods() throws IOException;

}
