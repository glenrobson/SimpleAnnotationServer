package uk.org.llgc.annotation.store.contollers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import javax.annotation.PostConstruct;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;

import java.util.Base64;
import java.util.zip.Deflater;

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

    public List<PageAnnoCount> listAnnoPages(final Manifest pManifest) {
        try {
            List<PageAnnoCount> tAnnos =  _store.listAnnoPages(pManifest);
            return tAnnos;
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
}
