package uk.org.llgc.annotation.store.controllers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.annotation.PostConstruct;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.data.Collection;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import java.io.IOException;

@RequestScoped
@ManagedBean
public class AuthorisationController {
    protected UserService _users = null;
    protected HttpSession _session = null;

    public AuthorisationController() {
        _users = new UserService();
        init();
    }

    public AuthorisationController(final HttpSession pSession) {
        _users = new UserService(pSession);
        init();
    }
    public AuthorisationController(final UserService pService) {
        _users = pService;
        init();
    }

    @PostConstruct
    public void init() {
    }

    private User getUser() {
        if (tUser == null) {
            return _users.getUser();
        } else {
            return tUser;
        }
    }
    private User tUser = null;
    // Only for use with unit testing!
    public void setUser(final User pUser) {
        tUser = pUser;
    }

    /**
     * Allow change if logged in user is the same as the one being edited
     * or user is admin
     */
    public boolean changeUserDetails(final User pUserToChange) {
        User tLoggedInUser = this.getUser();
        return tLoggedInUser.getId().equals(pUserToChange.getId()) || tLoggedInUser.isAdmin();
    }

    public boolean allowUpdate(final Annotation pSavedAnno, final Annotation pNewAnno) {
        User tLoggedInUser = this.getUser();
        return (pSavedAnno.getCreator() != null && pSavedAnno.getCreator().getId().equals(tLoggedInUser.getId()) || tLoggedInUser.isAdmin());
    }

    public boolean allowDelete(final Annotation pSavedAnno) {
        User tLoggedInUser = this.getUser();
        return (pSavedAnno.getCreator() != null && pSavedAnno.getCreator().getId().equals(tLoggedInUser.getId()) || tLoggedInUser.isAdmin());
    }

    public boolean allowDeleteCollection(final Collection pCollection) {
        User tLoggedInUser = this.getUser();
        return (pCollection.getUser() != null && pCollection.getUser().getId().equals(tLoggedInUser.getId())) || tLoggedInUser.isAdmin();
    }

    public boolean allowCollectionEdit(final Collection pCollection) {
        User tLoggedInUser = this.getUser();
        System.out.println("Logged in user " + tLoggedInUser);
        System.out.println("Collection " + pCollection);
        return (pCollection.getUser() != null && pCollection.getUser().getId().equals(tLoggedInUser.getId())) || tLoggedInUser.isAdmin();
    }

    public boolean allowViewCollection(final Collection pCollection) {
        if (StoreConfig.getConfig().isPublicCollections() && !pCollection.getId().endsWith("all.json")) {
            return true;
        } else {    
            User tLoggedInUser = this.getUser();
            return (pCollection.getUser() != null && pCollection.getUser().getId().equals(tLoggedInUser.getId())) || tLoggedInUser.isAdmin();
        }
    }

    public boolean allowExportAllAnnotations() {
        User tLoggedInUser = this.getUser();
        return tLoggedInUser.isAdmin(); // Only admin can do this
    }

    public boolean allowThrough(final HttpServletRequest pRequest) {
        if (pRequest.getRequestURI().contains("/collection/")) {
            if (StoreConfig.getConfig().isPublicCollections()) {
                return pRequest.getMethod().equals("GET") && !pRequest.getRequestURI().endsWith("all.json");
            }
        }
        return false;
    }
}
