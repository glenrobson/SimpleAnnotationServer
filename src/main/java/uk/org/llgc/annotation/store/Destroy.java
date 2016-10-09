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

import com.hp.hpl.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;

public class Destroy extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Destroy.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		AnnotationUtils tAnnoUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store.init(tAnnoUtils);
	}

	public void doDelete(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		_logger.debug("uri " + pReq.getParameter("uri"));
		String tURI = pReq.getParameter("uri");
		_store.deleteAnnotation(tURI);
		pRes.setStatus(pRes.SC_NO_CONTENT);
	}
}
