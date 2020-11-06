package uk.org.llgc.annotation.store.contollers;

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

    @PostConstruct
    public void init() {
    }

    /**
     * Allow change if logged in user is the same as the one being edited
     * or user is admin
     */
    public boolean changeUserDetails(final User pUserToChange) {
        User tLoggedInUser = _users.getUser();
        return tLoggedInUser.getId().equals(pUserToChange.getId()) || tLoggedInUser.isAdmin();
    }
}
