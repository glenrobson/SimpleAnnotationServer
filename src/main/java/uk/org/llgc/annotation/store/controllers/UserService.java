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

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.net.URISyntaxException;

@RequestScoped
@ManagedBean
public class UserService {
    protected StoreAdapter _store = null;
    protected HttpSession _session = null;

    public UserService() {
        init();
    }

    public UserService(final HttpSession pSession) {
        _session = pSession;
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

    public User getUser() {
        HttpSession tSession = this.getSession();
        if (tSession.getAttribute("user") != null) {
            return (User)tSession.getAttribute("user");
        } else {
            return null;
        }
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
