package uk.org.llgc.annotation.store.contollers;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.annotation.PostConstruct;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;
import uk.org.llgc.annotation.store.StoreConfig;

import java.util.List;

@RequestScoped
@ManagedBean
public class UserService {

    protected HttpSession _session = null;

    public UserService() {
    }

    public UserService(final HttpSession pSession) {
        _session = pSession;
    }
 
    @PostConstruct
    public void init() {
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

    public void setUser(final User pUser) {
        this.getSession().setAttribute("user", pUser);
    }

    public User getUser() {
        HttpSession tSession = this.getSession();
        if (tSession.getAttribute("user") != null) {
            return (User)tSession.getAttribute("user");
        } else {
            return null;
        }
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
