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
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.github.jsonldjava.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonParseException;

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
import uk.org.llgc.annotation.store.controllers.StoreService;


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
        try {
            User tUser = new UserService(pReq).getUser();
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
                StringBuffer tBuff = new StringBuffer();
                BufferedReader tReader = null;
                if (pReq.getCharacterEncoding() != null) {
                    tReader = new BufferedReader(new InputStreamReader(pReq.getInputStream(), pReq.getCharacterEncoding()));
                } else {
                    tReader = new BufferedReader(new InputStreamReader(pReq.getInputStream(), "UTF-8"));
                }
                String tLine = "";
                while ((tLine = tReader.readLine()) != null) {
                    tBuff.append(tLine);
                }

                String tManifestStr = tBuff.toString();
                  
                if (tManifestStr.isEmpty()) {
                    Map<String,Object> tResponse = new HashMap<String,Object>();
                    tResponse.put("code", pRes.SC_NOT_FOUND);
                    tResponse.put("message", "Manifest not found POST was empty");
                    sendJson(pRes, HttpServletResponse.SC_NOT_FOUND, tResponse);
                    return;
                }
                
                try {
                    // This calls the fromReader anyway so probably more efficient to pass the reader above.
                    tManifestJson = (Map<String,Object>)JsonUtils.fromString(tManifestStr);
                } catch (JsonParseException tExcpt) {
                    System.out.println("Failed to load the following manifest: ");
                    System.out.println(tManifestStr);
                    throw tExcpt;
                }

                if (tManifestJson.get("within") != null) {
                    tCollectionId = (String)tManifestJson.get("within");
                    tManifestJson.remove("within");
                }
            }
            if (tCollectionId.isEmpty()) {
                Collection tTmpCollection = new Collection();
                tTmpCollection.setUser(tUser);
                tCollectionId = tTmpCollection.createDefaultId(StoreConfig.getConfig().getBaseURI(pReq));
            }

            Collection tCollection = _store.getCollection(tCollectionId);
            if (tCollection == null) {
                tCollection = _store.getCollection(StoreConfig.getConfig().getBaseURI(pReq) + "/collection/" + tUser.getShortId() + "/inbox.json");
            }
            AuthorisationController tAuth = new AuthorisationController(pReq);
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
        } catch (IOException tExcpt) {
            tExcpt.printStackTrace();
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_INTERNAL_SERVER_ERROR);
            tResponse.put("message", "Failed to process manifest due to: " + tExcpt.getMessage());
            this.sendJson(pRes, pRes.SC_INTERNAL_SERVER_ERROR, tResponse);
        } catch (Exception tExcpt) {
            tExcpt.printStackTrace();
            Map<String,Object> tResponse = new HashMap<String,Object>();
            tResponse.put("code", pRes.SC_INTERNAL_SERVER_ERROR);
            tResponse.put("message", "Failed to process manifest due to: " + tExcpt.getMessage());
            this.sendJson(pRes, pRes.SC_INTERNAL_SERVER_ERROR, tResponse);
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
        AuthorisationController tAuth = new AuthorisationController(pReq);

        if (tManifestId != null) {
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
        } else {
            // Get enriched manifest
            boolean regenerate = pReq.getParameter("regenerate") != null && pReq.getParameter("regenerate").equals("true");
            String relativeId = pReq.getRequestURI().substring(pReq.getRequestURI().lastIndexOf("/manifests/") + "/manifests/".length());
            // 1245635613/1969268/5bbada360fbe7c8f72a8153896686398.json
            int tLastSlash = relativeId.lastIndexOf("/");
            User tUser = new User();
            String tFilename = relativeId.substring(tLastSlash + 1);
            try {
                tUser = User.createUserFromShortID(StoreConfig.getConfig().getBaseURI(pReq), relativeId.substring(0, tLastSlash));
            } catch (URISyntaxException tExcpt) {
                throw new IOException("Unable to create user due to " + tExcpt);
            }
            tUser = _store.getUser(tUser);

            String tURI = _store.getManifestId(tFilename.split("\\.json")[0]);
            Manifest tManifest = new Manifest();
            tManifest.setURI(tURI);

            if (tAuth.allowReadManifest(tManifest, tUser)) {
                StoreService tService = new StoreService(pReq);

                Manifest tFullManifest = tService.getEnhancedManifest(tUser, tManifest, regenerate);

                this.sendJson(pRes, pRes.SC_OK, tFullManifest.toJson());
            } else {
                Map<String,Object> tResponse = new HashMap<String,Object>();
                tResponse.put("code", pRes.SC_UNAUTHORIZED);
                tResponse.put("message", "You are not allowed to see this users annotations");
                this.sendJson(pRes, pRes.SC_UNAUTHORIZED, tResponse);
                return;
            }
        }
    }
}
