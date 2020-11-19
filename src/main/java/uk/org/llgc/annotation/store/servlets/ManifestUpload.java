package uk.org.llgc.annotation.store.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.BufferedReader;

import java.net.URL;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.data.ManifestProcessor;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.data.Collection;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;
import uk.org.llgc.annotation.store.controllers.UserService;


public class ManifestUpload extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ManifestUpload.class.getName());
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;
	protected File _manifestDir = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
		_manifestDir = new File(super.getServletContext().getRealPath(pConfig.getInitParameter("manifest_dir")));
		if (!_manifestDir.exists()) {
			_logger.debug("Making " + _manifestDir.getPath());
			_manifestDir.mkdirs();
		} else {
			_logger.debug("exists " + _manifestDir.getPath());
		}
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        User tUser = new UserService(pReq.getSession()).getUser();
		String tID = "";
		Map<String, Object> tManifestJson = null;
        String tCollectionId = "";
		if (pReq.getParameter("uri") != null) {
			tID = pReq.getParameter("uri");
			tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(new URL(tID).openStream());

            if (pReq.getParameter("collection") != null) {
                tCollectionId = pReq.getParameter("collection");
            }
		} else {
			InputStream tManifestStream = pReq.getInputStream();
            
			tManifestJson = (Map<String,Object>)JsonUtils.fromInputStream(tManifestStream);

            if (tManifestJson.get("within") != null) {
                tCollectionId = (String)tManifestJson.get("within");
                tManifestJson.remove("within");
            }
		}

        Collection tCollection = _store.getCollection(tCollectionId);
        if (tCollection == null) {
            tCollection = _store.getCollection(StoreConfig.getConfig().getBaseURI(pReq) + "/collection/" + tUser.getShortId() + "/inbox.json");
        }
        AuthorisationController tAuth = new AuthorisationController(pReq.getSession());
        if (tAuth.allowCollectionEdit(tCollection)) {
            Manifest tManifest = new Manifest(tManifestJson, null);
            if (!tCollection.getManifests().contains(tManifest)) {
                System.out.println("Adding Manifest " + tManifest);
                String tShortId = _store.indexManifest(tManifest);

                System.out.println("To collection " + tCollection);
                tCollection.getManifests().add(tManifest);
                _store.updateCollection(tCollection);
            }

            Map<String,Object> tJson = new HashMap<String,Object>();
            Map<String,String> tLinks = new HashMap<String,String>();
            tJson.put("loaded", tLinks);
            tLinks.put("uri", tManifest.getURI());
            tLinks.put("short_id", tManifest.getShortId());

            this.sendJson(pRes, pRes.SC_OK, tJson);
        } else {
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_UNAUTHORIZED);
            tResponse.put("message", "You can only edit your own collections unless you are Admin");
            this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
        }
	}

    protected void sendJson(final HttpServletResponse pRes, final int pCode, final Map<String,Object> pPayload) throws IOException {
        pRes.setStatus(pCode);
        pRes.setContentType("application/json");
        pRes.setCharacterEncoding("UTF-8");
        JsonUtils.writePrettyPrint(pRes.getWriter(), pPayload);
    }

	// if asked for without path then return collection of manifests that are loaded
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        String tCollectionId = pReq.getParameter("collection");
        String tManifestId = pReq.getParameter("manifest");

        Manifest tManifest = _store.getManifest(tManifestId);
        if (tManifest == null) {
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_NOT_FOUND);
            tResponse.put("message", "Manifest is not loaded");
            sendJson(pRes, HttpServletResponse.SC_NOT_FOUND, tResponse);
        } else {
            if (tCollectionId != null) {
                Collection tCollection = _store.getCollection(tCollectionId);
                if (tCollection != null && !tCollection.contains(tManifest)) {
                    AuthorisationController tAuth = new AuthorisationController(pReq.getSession());
                    if (tAuth.allowCollectionEdit(tCollection)) {
                        tCollection.add(tManifest);
                        _store.updateCollection(tCollection);
                    } else {
                        Map<String,Object> tResponse = new HashMap<String,Object>();
                        tResponse.put("code", pRes.SC_UNAUTHORIZED);
                        tResponse.put("message", "You can only edit your own collections unless you are Admin");
                        this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
                        return;
                    }
                }
            }
            Map<String,Object> tJson = new HashMap<String,Object>();
            Map<String,String> tLinks = new HashMap<String,String>();
            tJson.put("loaded", tLinks);
            tLinks.put("uri", tManifest.getURI());
            tLinks.put("short_id", tManifest.getShortId());

            this.sendJson(pRes, pRes.SC_OK, tJson);
        }
    }
}
