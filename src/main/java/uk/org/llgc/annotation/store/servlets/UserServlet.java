package uk.org.llgc.annotation.store.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.IOException;

import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.users.LocalUser;
import uk.org.llgc.annotation.store.controllers.UserService;

import java.util.Map;
import java.util.HashMap;

import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserServlet extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(UserServlet.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
    }

	public void doDelete(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        AuthorisationController tAuth = new AuthorisationController(pReq);
        User tLoggedInUser = new UserService(pReq).getUser();

        User tUser = new User();
        try {
            tUser.setId(pReq.getParameter("uri"));
        } catch (URISyntaxException tExcpt) {
            _logger.debug("failed to delete " + tLoggedInUser + "due to: " + tLoggedInUser);
        }

        Map<String,Object> tResponse = new HashMap<String,Object>();
        if (tAuth.deleteUser(tLoggedInUser, tUser)) {
            _store.deleteUser(tUser);
            tResponse.put("code", pRes.SC_OK);
            tResponse.put("message", "User deleted");
            this.sendJson(pRes, pRes.SC_OK, tResponse);
        } else {
            tResponse.put("code", pRes.SC_UNAUTHORIZED);
            tResponse.put("message", "You can only delete users if you are Admin");
            this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
        }
    }

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        AuthorisationController tAuth = new AuthorisationController(pReq);
        User tLoggedInUser = new UserService(pReq).getUser();
        User tUser = getUser(pReq);
        System.out.println("Saved user " + tUser);
        Map<String,Object> tResponse = new HashMap<String,Object>();
        if (tAuth.changeUserDetails(tUser)) {
            tUser.setName(pReq.getParameter("name"));
            tUser.setEmail(pReq.getParameter("email"));
            if (pReq.getParameter("password") != null && pReq.getParameter("password").trim().length() != 0) {
                ((LocalUser)tUser).setPassword(pReq.getParameter("password"));
            }

            System.out.println("Updated User " + tUser);

            User tUpdated = _store.saveUser(tUser);
            if (tLoggedInUser.getId().equals(tUser.getId())) {
                // If editing the logged in user copy across info and update session
                tUser.setToken(tLoggedInUser.getToken());
                pReq.getSession().setAttribute("user", tUser);
            }
            tResponse.put("code", pRes.SC_OK);
            tResponse.put("message", "Profile updated.");
            this.sendJson(pRes, pRes.SC_OK, tResponse);
        } else {
            tResponse.put("code", pRes.SC_UNAUTHORIZED);
            tResponse.put("message", "You can only edit your own details unless you are Admin");
            this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
        }
    }

    protected void sendJson(final HttpServletResponse pRes, final int pCode, final Map<String,Object> pPayload) throws IOException {
        pRes.setStatus(pCode);
        pRes.setContentType("application/json");
        pRes.setCharacterEncoding("UTF-8");
        JsonUtils.writePrettyPrint(pRes.getWriter(), pPayload);
    }

    protected User getUser(final HttpServletRequest pReq) throws IOException {
        String relativeId = pReq.getRequestURI().substring(pReq.getRequestURI().lastIndexOf("/user/"));
        String tPersonURI = StoreConfig.getConfig().getBaseURI(pReq) + relativeId;

        User tSkeletonUser = new User();
        try {
            tSkeletonUser.setId(tPersonURI);
            tSkeletonUser.setShortId(relativeId.substring("/user/".length()));
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Unable to add user because " + tPersonURI + " is not a valid URI");
        }
        User tFullUser = _store.getUser(tSkeletonUser);
        return tFullUser;
    }
}
