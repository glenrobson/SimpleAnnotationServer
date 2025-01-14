package uk.org.llgc.annotation.store.controllers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import jakarta.annotation.PostConstruct;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.exceptions.PermissionDenied;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

import java.util.Base64;
import java.util.zip.Deflater;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.faces.context.FacesContext;

import com.github.jsonldjava.utils.JsonUtils;

import java.net.URL;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
@ManagedBean
public class StoreService {
	protected static Logger _logger = LogManager.getLogger(StoreService.class.getName());
    protected StoreAdapter _store = null;
    protected HttpServletRequest _request = null;

    public StoreService() {
    }

    public StoreService(final HttpServletRequest pRequest) {
        _request = pRequest;
        this.init();
    }

    @PostConstruct
    public void init() {
        _store = StoreConfig.getConfig().getStore();
    }

    protected User getCurrentUser() {
        UserService tService = new UserService();
        return tService.getUser();
    }

    protected AuthorisationController getAuth() {
        return new AuthorisationController();
    }

    protected User getCurrentUser(final HttpServletRequest pRequest) {
        UserService tService = new UserService(pRequest);
        return tService.getUser();
    }

    public List<PageAnnoCount> listAnnoPages(final String pURI) throws PermissionDenied {
        return listAnnoPages(pURI, this.getCurrentUser());
    }

    public List<PageAnnoCount> listAnnoPages(final String pURI, final User pUser) throws PermissionDenied {
        if (this.getAuth().allowReadSomeoneElseAnnos(pUser, this.getCurrentUser())) { 
            Manifest tManifest = new Manifest();
            tManifest.setURI(pURI);

            try {
                return _store.listAnnoPages(tManifest, pUser);
            } catch (IOException tExcpt) {
                System.err.println("Failed to retrieve stats for " + pURI);
                tExcpt.printStackTrace();
            }
            return new ArrayList<PageAnnoCount>();
        } else {
            throw new PermissionDenied("User " + this.getCurrentUser().getName() + "(" + this.getCurrentUser().getId() + ") cannot access " + pUser.getName() + "(" + pUser.getId() + ") annotations as user is not an admin");
        }
    }

    public Map<String,Integer> countAnnotations(final Manifest pManifest) throws PermissionDenied {
        return countAnnotations(pManifest, this.getCurrentUser());
    }

    public Map<String,Integer> countAnnotations(final Manifest pManifest, final User pUser) throws PermissionDenied {
        String tKey = "stats_" + pManifest.getShortId();
        HttpServletRequest tRequest = this.getRequest();
        if (tRequest.getAttribute(tKey) != null) {
            return (Map<String,Integer>)tRequest.getAttribute(tKey);
        }

        Map<String,Integer> tStats = new HashMap<String,Integer>();
        tStats.put("canvas_count", 0);
        tStats.put("total_annos", 0);
        List<PageAnnoCount> tCount = this.listAnnoPages(pManifest.getURI(), pUser);
        tStats.put("canvas_count", tCount.size());

        int tTotalAnnos = 0;
        for (PageAnnoCount tPageCount : tCount) {
            tTotalAnnos += tPageCount.getCount();
        }
        tStats.put("total_annos", tTotalAnnos);

        if (tRequest.getAttribute(tKey) == null) {
            tRequest.setAttribute(tKey, tStats);
        }
        return tStats;
    }

    protected HttpServletRequest getRequest() {
        if (_request == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            return (HttpServletRequest)facesContext.getExternalContext().getRequest();
        } else {
            return _request;
        }
    }

    public Manifest getEnhancedManifest(final String pManifestURI) throws IOException {
        Manifest tManifest = this.getManifestId(pManifestURI);
        return this.getEnhancedManifest(this.getCurrentUser(), tManifest, false);
    }

    public Manifest getEnhancedManifest(final User pUser, final Manifest pManifest, final boolean regenerate) throws IOException {
        File tManifestPath = new File(StoreConfig.getConfig().getDataDir(),"manifests");
        File tUserDir = new File(tManifestPath, pUser.getShortId());
        File tManifestFile = new File(tUserDir, pManifest.getShortId() + ".json");

        if (tManifestFile.exists() && !regenerate) {
            Manifest tManifest = new Manifest();
            tManifest.setJson((Map<String,Object>)JsonUtils.fromInputStream(new FileInputStream(tManifestFile)));

            return tManifest;
        } else {
            tManifestFile.getParentFile().mkdirs();
            Manifest tManifest = this.generateEnhancedManifest(pUser, pManifest);
            StringBuffer tURL = new StringBuffer(StoreConfig.getConfig().getBaseURI(this.getRequest()));
            if (!tURL.toString().endsWith("/")) {
                tURL.append("/");
            }
            tURL.append("manifests/");
            tURL.append(pUser.getShortId());
            tURL.append("/");
            tURL.append(pManifest.getShortId());
            tURL.append(".json");
            tManifest.setURI(tURL.toString());

            JsonUtils.writePrettyPrint(new BufferedWriter(new FileWriter(tManifestFile)), tManifest.getJson());
            return tManifest;
        }
    }

    public Manifest generateEnhancedManifest(final User pUser, final Manifest pManifest) throws IOException {
        Manifest tSourceManifest = new Manifest();
        tSourceManifest.setJson((Map<String,Object>)JsonUtils.fromInputStream(new URL(pManifest.getURI()).openStream()));

        tSourceManifest.addSearchService(StoreConfig.getConfig().getBaseURI(this.getRequest()), pUser);

        tSourceManifest.addAnnotationLists(StoreConfig.getConfig().getBaseURI(this.getRequest()), pUser);

        return tSourceManifest;
    }

    public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) {
        HttpServletRequest tRequest = this.getRequest();
        if (tRequest.getAttribute(pManifest.getURI()) != null) {
            return (List<PageAnnoCount>)tRequest.getAttribute(pManifest.getURI());
        }
        try {
            //new  PageAnnoCount(final Canvas pCanvas, final int pCount, final Manifest pManifest)
            List<PageAnnoCount> tAnnosCount =  _store.listAnnoPages(pManifest, this.getCurrentUser());
            List<PageAnnoCount> tFullCanvasList = new ArrayList<PageAnnoCount>();
            for (Canvas tCanvas : pManifest.getCanvases()) {
                PageAnnoCount tCanvasCount = new PageAnnoCount(tCanvas, 0, pManifest);
                if (tAnnosCount.contains(tCanvasCount)) {
                    tCanvasCount = tAnnosCount.get(tAnnosCount.indexOf(tCanvasCount));
                }
                tFullCanvasList.add(tCanvasCount);
            }
            if (tRequest.getAttribute(pManifest.getURI()) == null) {
                tRequest.setAttribute(pManifest.getURI(), tFullCanvasList);
            }
            return tFullCanvasList;
        } catch (IOException tExcpt) {
            return new ArrayList<PageAnnoCount>();
        }
    }

    public String shorternCanvas(final String pCanvasId) throws IOException {
        byte[] output = new byte[pCanvasId.getBytes("UTF-8").length];
        Deflater compresser = new Deflater();
        compresser.setInput(pCanvasId.getBytes("UTF-8"));
        compresser.finish();
        int compressedDataLength = compresser.deflate(output);
        compresser.end();
        return Base64.getUrlEncoder().encodeToString(output);
    }

    public Manifest getManifestFromAnnotations(final List<PageAnnoCount> pAnnoCounts) {
        if (pAnnoCounts.isEmpty()) {
            return null;
        } else {
            return pAnnoCounts.get(0).getManifest();
        }
    }

    public Canvas getCanvasId(final String pId) {
        try {
            Canvas tCanvas = new Canvas(pId, "");
            tCanvas = _store.resolveCanvas(tCanvas.getShortId());
            return tCanvas;
        } catch (IOException tExcpt) {
            return null;
        }
    }

    public List<Manifest> getManifests() {
        try {
            return _store.getManifests();
        } catch (IOException tExcpt) {
            return new ArrayList<Manifest>();
        }
    }

    public List<Manifest> getAnnoManifests() {
        try {
            return _store.getSkeletonManifests(this.getCurrentUser());
        } catch (IOException tExcpt) {
            return new ArrayList<Manifest>();
        }
    }

    public Manifest getManifestFromCanvas(final String pCanvasURI) {
        try {
            Manifest tSkeleton = _store.getManifestForCanvas(new Canvas(pCanvasURI, "")); 
            return _store.getManifest(tSkeleton.getURI());
        } catch (IOException tExcpt) {
            return new Manifest();
        }
    }

    public AnnotationList getAnnotations(final String pCanvasURI) {
        return this.getAnnotations(pCanvasURI, this.getCurrentUser());
    }

    public AnnotationList getAnnotations(final String pCanvasURI, final User pUser) {
        HttpServletRequest tRequest = this.getRequest();
        String tStoreKey = "al_" + pCanvasURI;
        if (tRequest.getAttribute(tStoreKey) != null) {
            _logger.debug("Getting annotations from cache");
            return (AnnotationList)tRequest.getAttribute(tStoreKey);
        }

        try {
            AnnotationList tAnnos = _store.getAnnotationsFromPage(pUser, new Canvas(pCanvasURI, ""));
            Collections.sort(tAnnos.getAnnotations(), new Comparator() {
                            public int compare(Object o1, Object o2) {
                                Annotation tAnno1 = (Annotation)o1;
                                Annotation tAnno2 = (Annotation)o2;

                                try {
                                    return new Integer(lastPartOfID(tAnno1)).compareTo(new Integer(lastPartOfID(tAnno2)));
                                } catch (NumberFormatException tExcpt) {
                                    // do string compare instead
                                    return lastPartOfID(tAnno1).compareTo(lastPartOfID(tAnno2));
                                }
                            }

                            protected String lastPartOfID(final Annotation pAnno) {
                                return pAnno.getId().substring(pAnno.getId().lastIndexOf("/"));
                            }
                       }
                    );
            
            // sort and store in request
            tRequest.setAttribute(tStoreKey, tAnnos);

            _logger.debug("Getting annotations from db");
            return tAnnos;
        } catch (IOException tExcpt) {
            return new AnnotationList();
        }
    }

    public Manifest getManifest(final String pId) {
        try {
            return _store.getManifest(pId);
        } catch (IOException tExcpt) {
            return new Manifest();
        }
    }

    public Manifest getManifestId(final String pURI) {
        try {
            return _store.getManifest(pURI);
        } catch (IOException tExcpt) {
            return new Manifest();
        }
    }

    public List<Collection> getCollections(final HttpServletRequest pRequest) throws IOException {
        User tUser = this.getCurrentUser(pRequest);
        String tKey = "get_collections_from_request_" + tUser.getShortId();
        if (this.isCached(tKey)) {
            return (List<Collection>)this.getCacheObject(tKey);
        }
        _logger.debug("getCollections(pRequest)");
        List<Collection> tCollections = _store.getCollections(tUser);
        // if empty create the default collection
        if (tCollections.isEmpty()) {
            Collection tDefaultCollection = new Collection();
            tDefaultCollection.setUser(tUser);
            tDefaultCollection.setLabel(StoreConfig.getConfig().getDefaultCollectionName());
            tDefaultCollection.createDefaultId(StoreConfig.getConfig().getBaseURI(pRequest));
            tDefaultCollection = _store.createCollection(tDefaultCollection);
            tCollections.add(tDefaultCollection);
        }

        Collections.sort(tCollections);
        this.putCacheObject(tKey, tCollections);
        return tCollections;
    }

    protected void putCacheObject(final String pKey, final Object pObject) {
        HttpServletRequest tRequest = this.getRequest();
        tRequest.setAttribute(pKey, pObject);
    }

    protected Object getCacheObject(final String pKey) {
        HttpServletRequest tRequest = this.getRequest();
        return tRequest.getAttribute(pKey);
    }

    protected boolean isCached(final String pKey) {
        HttpServletRequest tRequest = this.getRequest();
        return tRequest.getAttribute(pKey) != null;
    }

    public List<Collection> getCollections(final User pUser) throws IOException {
        _logger.debug("getCollections(pUser)");
        String tKey = "collections_for_user_" + pUser.getShortId();

        if (this.isCached(tKey)) {
            return (List<Collection>)this.getCacheObject(tKey);
        }   

        List<Collection> tCollections = new ArrayList<Collection>();

        User tUser = this.getCurrentUser();
        if (tUser.isAdmin()) {
            tCollections = _store.getCollections(pUser);

            this.putCacheObject(tKey, tCollections);
        }

        return tCollections;
    }

    public Collection getCollection(final String pID, final HttpServletRequest pRequest) throws IOException {
        String tCollectionKey = "collection_";
        if (pID == null || pID.length() == 0) {
            User tUser = this.getCurrentUser(pRequest);

            List<Collection> tCollections = this.getCollections(pRequest);
            for (Collection tCollection : tCollections) {
                if (tCollection.isDefaultCollection()) {
                    return tCollection;
                }
            }
            // Create default collection
            Collection tDefaultCollection = new Collection();
            tDefaultCollection.setUser(tUser);
            tDefaultCollection.createDefaultId(StoreConfig.getConfig().getBaseURI(pRequest));
            Collection tResponse = _store.getCollection(tDefaultCollection.getId());
            this.putCacheObject(tCollectionKey + tResponse.getId(), tResponse);
            return tResponse;
        } else {
            if (this.isCached(tCollectionKey + pID)) {
                return (Collection)this.getCacheObject(tCollectionKey + pID);
            } else {
                Collection tResponse = _store.getCollection(pID);
                this.putCacheObject(tCollectionKey + tResponse.getId(), tResponse);
                return tResponse;
            }
        }
    }
}
