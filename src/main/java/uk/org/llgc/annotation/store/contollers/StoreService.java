package uk.org.llgc.annotation.store.contollers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import javax.annotation.PostConstruct;

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;

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

    public List<PageAnnoCount> getListAnnoPages() {
        try {
            return _store.listAnnoPages();
        } catch (IOException tExcpt) {
            return new ArrayList<PageAnnoCount>();
        }
    }

    public List<Manifest> getManifests() {
        try {
            return _store.getManifests();
        } catch (IOException tExcpt) {
            return new ArrayList<Manifest>();
        }
    }
}
