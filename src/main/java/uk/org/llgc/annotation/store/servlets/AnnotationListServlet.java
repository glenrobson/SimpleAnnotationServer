package uk.org.llgc.annotation.store.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

import java.net.URISyntaxException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

public class AnnotationListServlet extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(AnnotationList.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
        _store.init(new AnnotationUtils());
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        String relativeId = pReq.getRequestURI().substring(pReq.getRequestURI().lastIndexOf("/annotations/") + "/annotations/".length());
        int tLastSlash = relativeId.lastIndexOf("/");
        String tFilename = relativeId.substring(tLastSlash + 1);
        User tUser = null;
        try {
            tUser = _store.getUser(User.createUserFromShortID(StoreConfig.getConfig().getBaseURI(pReq), relativeId.substring(0, tLastSlash)));
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Unable to create user due to " + tExcpt);
        }

        Canvas tCanvas = _store.resolveCanvas(tFilename.split("\\.")[0]);

        AuthorisationController tAuth = new AuthorisationController(pReq);
        if (tAuth.allowReadAnnotations(tCanvas, tUser)) {
            AnnotationList tList = new AnnotationList();
            String tAnnoId = StoreConfig.getConfig().getBaseURI(pReq) + "/annotations/" + relativeId;
            if (tUser != null && tCanvas != null) {
                tList = _store.getAnnotationsFromPage(tUser, tCanvas);
                tList.setId(tAnnoId);
            }        
            pRes.setContentType("application/ld+json; charset=UTF-8");
            pRes.setCharacterEncoding("UTF-8");
            pRes.getWriter().println(JsonUtils.toPrettyString(tList.toJson()));
        } else {
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_UNAUTHORIZED);
            tResponse.put("message", "You are not allowed to view this users annotations");
            this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
        }
    }

    protected void sendJson(final HttpServletResponse pRes, final int pCode, final Map<String,Object> pPayload) throws IOException {
        pRes.setStatus(pCode);
        pRes.setContentType("application/json");
        pRes.setCharacterEncoding("UTF-8");
        JsonUtils.writePrettyPrint(pRes.getWriter(), pPayload);
    }

}
