package uk.org.llgc.annotation.store;

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

import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

public class Populate extends HttpServlet {
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Dataset tDataset = TDBFactory.createDataset(super.getServletContext().getInitParameter("data_dir"));
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("contexts")));
		_store = new StoreAdapter(tDataset);
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		InputStream tAnnotationList = null;
		if (pReq.getParameter("uri") != null) {
			System.out.println("Reading from " + pReq.getParameter("uri"));
			tAnnotationList = new URL(pReq.getParameter("uri")).openStream();
		} else {
			tAnnotationList = pReq.getInputStream();
		}
		List<Map<String, Object>> tAnnotationListJSON = _annotationUtils.readAnnotationList(tAnnotationList); //annotaiton list
		/**/System.out.println("JSON in:");
		/**/System.out.println(JsonUtils.toPrettyString(tAnnotationListJSON));

		_store.addAnnotationList(tAnnotationListJSON);

		pRes.setStatus(HttpServletResponse.SC_CREATED);
		pRes.setContentType("text/plain");
		pRes.getOutputStream().println("SUCCESS");
	}
}
