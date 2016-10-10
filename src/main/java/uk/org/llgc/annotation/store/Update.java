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

import java.util.Map;

import com.github.jsonldjava.utils.JsonUtils;

import com.hp.hpl.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;

public class Update extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Update.class.getName()); 

	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
	}
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		_logger.debug("get called");
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		try {
			Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(pReq.getInputStream(), StoreConfig.getConfig().getBaseURI(pReq) + "/annotation"); 
			_logger.debug("JSON in:");
			_logger.debug(JsonUtils.toPrettyString(tAnnotationJSON));
			String tAnnoId = (String)tAnnotationJSON.get("@id");

			Model tModel = _store.updateAnnotation(tAnnotationJSON);

			Map<String, Object> tAnnotationList = _annotationUtils.createAnnotationList(tModel);

			pRes.setStatus(HttpServletResponse.SC_CREATED);
			pRes.setContentType("application/ld+json; charset=UTF-8");
			pRes.setCharacterEncoding("UTF-8");
			_logger.debug("JSON out:");
			_logger.debug(JsonUtils.toPrettyString(tAnnotationList));
			pRes.getWriter().println(JsonUtils.toPrettyString(tAnnotationList));
		} catch (IOException tException) {	
			System.err.println("Exception occured trying to add annotation:");
			tException.printStackTrace();
			throw tException;
		}	
	}
}
