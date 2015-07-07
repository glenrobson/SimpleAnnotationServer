package uk.org.llgc.annotation.store;

import java.io.IOException;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import com.hp.hpl.jena.rdf.model.Model;

import java.util.Map;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class Search extends HttpServlet {
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("contexts")));
		_store = StoreConfig.getConfig().getStore();
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		System.out.println("Annotations for page: " + pReq.getParameter("uri"));
		/*System.out.println("media " + pReq.getParameter("media"));
		System.out.println("limit " + pReq.getParameter("limit"));*/

		List<Model> tAnnotations = _store.getAnnotationsFromPage(pReq.getParameter("uri"));
		List tAnnotationList = _annotationUtils.createAnnotationList(tAnnotations);

		pRes.setContentType("application/ld+json");
		/**/System.out.println(JsonUtils.toPrettyString(tAnnotationList));
		pRes.getOutputStream().println(JsonUtils.toPrettyString(tAnnotationList));
	}
}
