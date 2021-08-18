package uk.org.llgc.annotation.store.controllers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import javax.annotation.PostConstruct;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;

import java.util.Base64;
import java.util.zip.Deflater;
import java.util.Collections;
import java.util.Comparator;

import  javax.servlet.http.HttpServletRequest;
import javax.faces.context.FacesContext;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

@ApplicationScoped
@ManagedBean
public class StoreService {
    protected StoreAdapter _store = null;

    @PostConstruct
    public void init() {
        _store = StoreConfig.getConfig().getStore();
    }

    public List<PageAnnoCount> listAnnoPages(final String pURI) {
        Manifest tManifest = new Manifest();
        tManifest.setURI(pURI);

        return this.listAnnoPages(tManifest);
    }

    protected HttpServletRequest getRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return (HttpServletRequest)facesContext.getExternalContext().getRequest();
    }

    public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) {
        HttpServletRequest tRequest = this.getRequest();
        if (tRequest.getAttribute(pManifest.getURI()) != null) {
            return (List<PageAnnoCount>)tRequest.getAttribute(pManifest.getURI());
        }
        try {
            System.out.println("Start Anno Page");
            //new  PageAnnoCount(final Canvas pCanvas, final int pCount, final Manifest pManifest)
            List<PageAnnoCount> tAnnosCount =  _store.listAnnoPages(pManifest);
            List<PageAnnoCount> tFullCanvasList = new ArrayList<PageAnnoCount>();
            for (Canvas tCanvas : pManifest.getCanvases()) {
                PageAnnoCount tCanvasCount = new PageAnnoCount(tCanvas, 0, pManifest);
                if (tAnnosCount.contains(tCanvasCount)) {
                    tCanvasCount = tAnnosCount.get(tAnnosCount.indexOf(tCanvasCount));
                }
                tFullCanvasList.add(tCanvasCount);
            }
            System.out.println("End Anno Page");
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
            System.out.println("Resolving " + pId);
            Canvas tCanvas = new Canvas(pId, "");
            tCanvas = _store.resolveCanvas(tCanvas.getShortId());
            System.out.println("Found: " + tCanvas);
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
            UserService tService = new UserService();
            return _store.getSkeletonManifests(tService.getUser());
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
        HttpServletRequest tRequest = this.getRequest();
        String tStoreKey = "al_" + pCanvasURI;
        if (tRequest.getAttribute(tStoreKey) != null) {
            return (AnnotationList)tRequest.getAttribute(tStoreKey);
        }

        try {
            UserService tUserService = new UserService();
            AnnotationList tAnnos = _store.getAnnotationsFromPage(tUserService.getUser(), new Canvas(pCanvasURI, ""));
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
        UserService tService = new UserService(pRequest.getSession(true));
        User tUser = tService.getUser();
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

        return tCollections;
    }

    public Collection getCollection(final String pID, final HttpServletRequest pRequest) throws IOException {
        if (pID == null || pID.equals("")) {
            UserService tService = new UserService(pRequest.getSession(true));
            User tUser = tService.getUser();
            // Get default collection
            Collection tDefaultCollection = new Collection();
            tDefaultCollection.setUser(tUser);
            tDefaultCollection.createDefaultId(StoreConfig.getConfig().getBaseURI(pRequest));
            return _store.getCollection(tDefaultCollection.getId());
        } else {
            return _store.getCollection(pID);
        }
    }
}
