package uk.org.llgc.annotation.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.rdf.model.Model;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.text.ParseException;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.DateRange;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

public class IIIFSearchAPI extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(IIIFSearchAPI.class.getName()); 
	protected StoreAdapter _store = null;
	protected int _resultsPerPage = 1000;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
		AnnotationUtils tAnnotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), StoreConfig.getConfig().getEncoder());
		_store.init(tAnnotationUtils);
		if (pConfig.getInitParameter("results_per_page") != null) {
			_resultsPerPage = Integer.parseInt(pConfig.getInitParameter("results_per_page"));
		}	
	}

	// http://universalviewer.io/examples/?manifest=http://193.61.220.59:8888/manifests/4642022.json&locale=en-GB#?c=0&m=0&s=0&cv=6&z=-37.9666%2C0%2C9949.9332%2C7360
    // example URL: http://localhost:8888/search-api/1245635613/1969268/5bbada360fbe7c8f72a8153896686398/search
    //  baseURL + /<user_id>/<manifest_id>/search
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        String relativeId = pReq.getRequestURI().substring(pReq.getRequestURI().lastIndexOf("/iiif-search/") + "/iiif-search/".length());

        String tManifestShortId = "";
        String tUserId = "";
        String[] tSplit = relativeId.split("/");
        for (int i = tSplit.length - 1; i >= 0; i--) {
            if (i == tSplit.length - 2) {
                tManifestShortId = tSplit[i];
            }
            if (i < tSplit.length - 2) {
                if (tUserId.length() == 0){
                    tUserId = tSplit[i];
                } else {
                    tUserId = tSplit[i] + "/" + tUserId;
                }
            }
        }
        User tUser = null;
        try {
            System.out.println("Userid '" + tUserId + "'");
            if ((tUserId == null || tUserId.length() == 0) && pReq.getParameter("user") != null) {
                System.out.println("Found user param " + pReq.getParameter("user"));
                // User has been passed as a parameter
                tUser = new User();
                tUser.setId(pReq.getParameter("user"));
                tUser = _store.getUser(tUser);
            } else {
                tUser = _store.getUser(User.createUserFromShortID(StoreConfig.getConfig().getBaseURI(pReq), tUserId));
            }
        } catch (URISyntaxException tExcpt) {
            throw new IOException("Unable to create user due to " + tExcpt);
        }

		SearchQuery tQuery = null;
        Manifest tManifest = new Manifest();
        tManifest.setURI(_store.getManifestId(tManifestShortId));

        if (tUser == null || tManifest.getURI() == null || tManifest.getURI().length() == 0) {
            System.out.println("User " + tUserId + " Manifest " + tManifestShortId);
            // unable to find user or manifest
			pRes.sendError(pRes.SC_NOT_FOUND,"Failed to find user or manifest supplied");
			return;
        }

        AuthorisationController tAuth = new AuthorisationController(pReq);
        if (tAuth.allowSearchManifest(tManifest, tUser)) {
            URL tSearchURL = tManifest.getSearchURL(StoreConfig.getConfig().getBaseURI(pReq), tUser);

            try { 
                StringBuffer tURI = new StringBuffer(tSearchURL.toString());
                if (pReq.getQueryString() != null) {
                    tURI.append("?");
                    tURI.append(pReq.getQueryString());
                }	
                System.out.println("Search URI: " + tURI.toString());
                tQuery = new SearchQuery(new URI(tURI.toString()));
                tQuery.setResultsPerPage(_resultsPerPage);
                tQuery.setScope(tManifest.getURI());

                // Query may already contain user from Query string
                // If so don't duplicate
                tQuery.addUser(tUser);
                System.out.println("Users: " + tQuery.getUsers());
            } catch (ParseException tExcpt) {
                pRes.sendError(pRes.SC_BAD_REQUEST,"Failed to parse date paratmeters " + tExcpt);
                return;
            } catch (URISyntaxException tExcpt) {
                pRes.sendError(pRes.SC_BAD_REQUEST,"Failed to parse uri " + tExcpt);
                return;
            }

            AnnotationList tResults = _store.search(tQuery);

            pRes.setContentType("application/ld+json; charset=UTF-8");
            pRes.setCharacterEncoding("UTF-8");
            pRes.getWriter().println(JsonUtils.toPrettyString(tResults.toJson()));
        } else {
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_UNAUTHORIZED);
            tResponse.put("message", "You are not allowed to search this users annotations");
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
