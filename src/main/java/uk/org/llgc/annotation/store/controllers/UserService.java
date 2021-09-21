package uk.org.llgc.annotation.store.controllers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.annotation.PostConstruct;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.users.LocalUser;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.net.URISyntaxException;

@RequestScoped
@ManagedBean
public class UserService {
    protected StoreAdapter _store = null;
    protected HttpSession _session = null;
    protected HttpServletRequest _request = null;

    public UserService() {
        init();
    }

    public UserService(final HttpServletRequest pRequest) {
        _request = pRequest;
        _session = pRequest.getSession();
        init();
    }

    @PostConstruct
    public void init() {
        _store = StoreConfig.getConfig().getStore();
    }

    protected HttpSession getSession() {
        if (_session == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpSession tSession = (HttpSession)facesContext.getExternalContext().getSession(true);
            return tSession;
        } else {
            return _session;
        }
    }

    protected HttpServletRequest getRequest() {
        if (_request == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            return (HttpServletRequest)facesContext.getExternalContext().getRequest();
        } else {
            return _request;
        }
    }

    public void setUser(final User pUser) throws IOException {
        // Try to get from Store to enhance this logged in user
        // If not present then this method should also add the user to the database
        User tEnhancedUser = _store.retrieveUser(pUser);
        this.getSession().setAttribute("user", tEnhancedUser);
    }

    public List<User> getUsers() throws IOException {
        List<User> tUsers = new ArrayList<User>();
        // check if admin then return users
        User tUser = this.getUser();
        if (tUser.isAdmin()) {
            tUsers = _store.getUsers();
        } else {
            System.out.println("User not admin so returning current user");
            tUsers.add(tUser);
        }

        return tUsers;
    }

    public boolean isAdminSetup() {
        try {
            // Is there at least one Admin with a password?
            List<User> tAdminUsers = _store.getUsers("admin");

            for (User tUser : tAdminUsers) {
                if (tUser instanceof LocalUser && ((LocalUser)tUser).hasPassword()) {
                    return true;
                }
            }
            return false;
        } catch (IOException pExcpt) {
            System.err.println("Failed to get Admin users due to:");
            pExcpt.printStackTrace();
            return false;
        }
    }

    public User getUser(final String pID) {
        User tUser = this.getUser();
        if (tUser.isAdmin()) {
            try {
                User tSearchUser = User.createUserFromID(pID);
                System.out.println("Search user " + tSearchUser);
                System.out.println("User " + _store.getUser(tSearchUser));
                return _store.getUser(tSearchUser);
            } catch (IOException tExcpt) {
                System.err.println("Failed to get user due to: " + tExcpt);
                return null;
            } catch (URISyntaxException tExcpt) {
                System.err.println("Failed to get user due to: " + tExcpt);
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isLocal(final User pUser) {
        return pUser instanceof LocalUser;
    }

    public User getUser() {
        HttpSession tSession = this.getSession();
        if (tSession.getAttribute("user") != null) {
            return (User)tSession.getAttribute("user");
        } else {
            return null;
        }
    }

    public LocalUser getLocalUser(final String pEmail) {
        try {
            List<User> tUsers = _store.getUsers("admin");
            for (User tUser : tUsers) {
                if (tUser instanceof LocalUser) {
                    LocalUser tAdminUser = (LocalUser)tUser;
                    if (tAdminUser.getEmail().equals(pEmail)) {
                        return tAdminUser;
                    }
                }
            }
            // if there is a admin set but not present in the DB create it.
            if (StoreConfig.getConfig().getAdminEmail() != null && pEmail.equals(StoreConfig.getConfig().getAdminEmail())) {
                LocalUser tAdmin = new LocalUser();
                try {

                    tAdmin.setId(User.createUserFromShortID(StoreConfig.getConfig().getBaseURI(this.getRequest()), "admin").getId());
                    tAdmin.setShortId("admin");
                    tAdmin.setEmail(StoreConfig.getConfig().getAdminEmail());
                    tAdmin.setPassword("", false);
                    tAdmin.setName("Admin User");
                    tAdmin.setAdmin(true);
                    tAdmin.setPicture("/images/AdminIcon.svg");

                    User tSavedUser = _store.saveUser(tAdmin);
                    return (LocalUser)tSavedUser;
                } catch (URISyntaxException tExcpt) {
                    System.err.println("Failed to create admin user due to an issue with the ID");
                    tExcpt.printStackTrace();
                }
            }
        } catch (IOException pExcpt) {
            System.err.println("Failed to get local user by email due to:");
            pExcpt.printStackTrace();
        }
        return null;
    }

    public String getRelativeId() {
        User tUser = this.getUser();
        return tUser.getId().substring(tUser.getId().lastIndexOf("user/"));
    }

    public boolean isAuthenticated() {
        return this.getSession().getAttribute("user") != null;
    }

    public boolean getAuthenticated() {
        return isAuthenticated();
    }

    public List<OAuthTarget> getConfig() {
        return StoreConfig.getConfig().getAuthTargets();
    }

    public boolean isAdmin() {
        HttpSession tSession = this.getSession();
        if (tSession.getAttribute("user") == null) {
            // No one is logged in
            return false;
        }
        User tUser = (User)tSession.getAttribute("user");
        return tUser.isAdmin();
    }
}
