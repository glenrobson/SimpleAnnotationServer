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

import java.net.URL;

import java.util.Map;
import java.util.List;

import com.github.jsonldjava.utils.JsonUtils;

import com.hp.hpl.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;

public class Populate extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Populate.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		InputStream tAnnotationList = null;
		if (pReq.getParameter("uri") != null) {
			_logger.debug("Reading from " + pReq.getParameter("uri"));
			tAnnotationList = new URL(pReq.getParameter("uri")).openStream();
		} else {
			/*java.io.BufferedReader tReader = new java.io.BufferedReader( new java.io.InputStreamReader( pReq.getInputStream())); 
			String tLine = "";
			System.out.println("Printing results");
			while ((tLine = tReader.readLine()) != null) {
				System.out.println("line:" + tLine);
			}
			System.out.println("done");*/
			tAnnotationList = pReq.getInputStream();
		}
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(tAnnotationList, StoreConfig.getConfig().getBaseURI(pReq) + "/annotation"); //annotaiton list
		_logger.debug("JSON in:");
		_logger.debug(JsonUtils.toPrettyString(tAnnotationListJSON));

		try {
			_store.addAnnotationList(tAnnotationListJSON);

			pRes.setStatus(HttpServletResponse.SC_CREATED);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("SUCCESS");
		} catch (IDConflictException tException) {
			tException.printStackTrace();
			pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("Failed to load annotation list as there was a conflict in ids " + tException.toString());
		}
	}
}
