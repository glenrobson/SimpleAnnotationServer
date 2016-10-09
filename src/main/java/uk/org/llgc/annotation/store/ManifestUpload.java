package uk.org.llgc.annotation.store;

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

import com.hp.hpl.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.data.ManifestProcessor;

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
		String tID = "";
		Map<String, Object> tManifest = null;
		if (pReq.getParameter("uri") != null) {
			tID = pReq.getParameter("uri");
			tManifest = (Map<String,Object>)JsonUtils.fromInputStream(new URL(tID).openStream());
		} else {
			InputStream tManifestStream = pReq.getInputStream();
			tManifest = (Map<String,Object>)JsonUtils.fromInputStream(tManifestStream);
		}

		String tShortId = _store.indexManifest(tManifest);

		pRes.sendRedirect(StoreConfig.getConfig().getBaseURI(pReq) + "/search-api/" + tShortId + "search");
	}

	// if asked for without path then return collection of manifests that are loaded
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		String tRequestURI = pReq.getRequestURI();
		String[] tSplitURI = tRequestURI.split("/");

		// Return collection 
		List<String> tManifests = _store.getManifests();

		Map<String,Object> tCollection = new HashMap<String,Object>();

		tCollection.put("@context", "http://iiif.io/api/presentation/2/context.json");
		tCollection.put("@id", StoreConfig.getConfig().getBaseURI(pReq) + "/annotation//collection/managed.json");
		tCollection.put("@type", "sc:Collection");
		tCollection.put("label","Collection of all manifests known by this annotation server");

		List<Map<String,Object>> tMembers = new ArrayList<Map<String,Object>>();
		for (String tManifestURI : tManifests) {
			Map<String, Object> tManifest = new HashMap<String,Object>();
			tManifest.put("@id", tManifestURI);
			tManifest.put("@type", "sc:Manifest");

			tMembers.add(tManifest);
		}

		tCollection.put("members", tMembers);
		tCollection.put("manifests", tMembers);

		pRes.setStatus(HttpServletResponse.SC_CREATED);
		pRes.setContentType("application/ld+json; charset=UTF-8");
		pRes.setCharacterEncoding("UTF-8");
		JsonUtils.write(pRes.getWriter(), tCollection);
	}
}
