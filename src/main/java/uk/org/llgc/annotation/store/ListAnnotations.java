package uk.org.llgc.annotation.store;

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

import uk.org.llgc.annotation.store.data.PageAnnoCount;
import uk.org.llgc.annotation.store.adapters.StoreAdapter;

public class ListAnnotations extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ListAnnotations.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		AnnotationUtils tAnnotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), StoreConfig.getConfig().getEncoder());
		_store = StoreConfig.getConfig().getStore();
		_store.init(tAnnotationUtils);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		Map<String, Object> tAnnotations = _store.getAllAnnotations();

		StringBuffer tURI = new StringBuffer(StoreConfig.getConfig().getBaseURI(pReq));
		tURI.append("/annotation/");
		tAnnotations.put("@id", tURI.toString());

		pRes.setContentType("application/ld+json; charset=UTF-8");
		pRes.setCharacterEncoding("UTF-8");
		pRes.getWriter().println(JsonUtils.toPrettyString(tAnnotations));
	}
}
