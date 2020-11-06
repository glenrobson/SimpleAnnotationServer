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
import uk.org.llgc.annotation.store.contollers.AuthorisationController;
import uk.org.llgc.annotation.store.data.users.User;

import java.util.Map;
import java.util.HashMap;

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


	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        AuthorisationController tAuth = new AuthorisationController(pReq.getSession());
        User tLoggedInUser = (User)pReq.getSession().getAttribute("user");
        User tUser = getUser(pReq);
        Map<String,Object> tResponse = new HashMap<String,Object>();
        if (tAuth.changeUserDetails(tUser)) {
            tUser.setName(pReq.getParameter("name"));
            tUser.setEmail(pReq.getParameter("email"));

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
        tSkeletonUser.setId(tPersonURI);

        User tFullUser = _store.getUser(tSkeletonUser);
        return tFullUser;
    }
}
