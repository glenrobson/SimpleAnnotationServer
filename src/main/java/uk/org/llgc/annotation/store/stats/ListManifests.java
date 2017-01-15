package uk.org.llgc.annotation.store.stats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import com.github.jsonldjava.utils.JsonUtils;

import com.hp.hpl.jena.rdf.model.Model;

import java.util.Map;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.data.Manifest;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;

public class ListManifests extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ListManifests.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")),StoreConfig.getConfig().getEncoder());
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		List<Manifest> tManifests = _store.getManifests();
		_logger.debug(tManifests);

		StringBuffer tContent = new StringBuffer();
		for (Manifest tManifest: tManifests) {
			tContent.append("<li><a href=\"");
			tContent.append(tManifest.getShortId());
			tContent.append(".html\">");
			tContent.append(tManifest.getLabel());
			tContent.append("</a>");
		}

		File tTemplate = new File(new File(super.getServletContext().getRealPath("/templates")), "list.manifests.template");
		BufferedReader tReader = null;
		try {
			tReader = new BufferedReader(new FileReader(tTemplate));
			String tLine = null;
			StringBuffer tHTML = new StringBuffer();
			while ((tLine = tReader.readLine()) != null ) {
				tHTML.append(tLine);
			}

			String tResult = tHTML.toString().replaceAll("##CONTENT##", tContent.toString());

			pRes.setContentType("text/html");
			pRes.getOutputStream().println(tResult);
		} finally {
			if (tReader != null) {
				tReader.close();
			}
		}

	}
}
