package uk.org.llgc.annotation.store;

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

public class Update extends HttpServlet {
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")));
		_store = StoreConfig.getConfig().getStore();
	}
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		System.out.println("get called");
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(pReq.getInputStream(), StoreConfig.getConfig().getBaseURI(pReq)); 
		/**/System.out.println("JSON in:");
		/**/System.out.println(JsonUtils.toPrettyString(tAnnotationJSON));
		String tAnnoId = (String)tAnnotationJSON.get("@id");

		_store.deleteAnnotation(tAnnoId);
		
		Model tModel = _store.updateAnnotation(tAnnotationJSON);

		Map<String, Object> tAnnotationList = _annotationUtils.createAnnotationList(tModel);

		pRes.setStatus(HttpServletResponse.SC_CREATED);
		pRes.setContentType("application/ld+json; charset=UTF-8");
		pRes.setCharacterEncoding("UTF-8");
		/**/System.out.println("JSON out:");
		/**/System.out.println(JsonUtils.toPrettyString(tAnnotationList));
		pRes.getWriter().println(JsonUtils.toPrettyString(tAnnotationList));
	}
}
