package uk.org.llgc.annotation.store;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;

public class Destroy extends HttpServlet {
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
	}

	public void doDelete(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		System.out.println("uri " + pReq.getParameter("uri"));
		System.out.println("media " + pReq.getParameter("media"));
		System.out.println("limit " + pReq.getParameter("limit"));
		String tURI = pReq.getParameter("uri");
		_store.deleteAnnotation(tURI);
		pRes.setStatus(pRes.SC_NO_CONTENT);
	}
}
