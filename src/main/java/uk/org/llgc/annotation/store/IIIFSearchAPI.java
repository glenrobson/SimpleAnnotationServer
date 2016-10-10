package uk.org.llgc.annotation.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import com.hp.hpl.jena.rdf.model.Model;

import java.util.Map;
import java.util.List;
import java.text.ParseException;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.data.SearchQuery;
import uk.org.llgc.annotation.store.data.DateRange;

public class IIIFSearchAPI extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(IIIFSearchAPI.class.getName()); 
	protected StoreAdapter _store = null;
	protected int _resultsPerPage = 1000;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
		AnnotationUtils tAnnotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), StoreConfig.getConfig().getEncoder());
		_store.init(tAnnotationUtils);
		if (pConfig.getInitParameter("results_per_page") != null) {
			_resultsPerPage = Integer.parseInt(pConfig.getInitParameter("results_per_page"));
		}	
	}

	// http://universalviewer.io/examples/?manifest=http://193.61.220.59:8888/manifests/4642022.json&locale=en-GB#?c=0&m=0&s=0&cv=6&z=-37.9666%2C0%2C9949.9332%2C7360
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		String[] tRequestURI = pReq.getRequestURI().split("/");
		String tManifestShortId = tRequestURI[tRequestURI.length - 2]; 

		SearchQuery tQuery = null;
		try { 
			StringBuffer tURI = null;
			if (pReq.getParameter("base") != null) {
				// a supplied base overides config
				tURI= new StringBuffer(pReq.getParameter("base"));
			} else {
				tURI = new StringBuffer(StoreConfig.getConfig().getBaseURI(pReq));
				tURI.append("/search-api/" + tManifestShortId + "/search");
			}	
			if (pReq.getQueryString() != null) {
				tURI.append("?");
				tURI.append(pReq.getQueryString());
			}	
			System.out.println("URI " + tURI.toString());
			tQuery = new SearchQuery(new URI(tURI.toString()));
			tQuery.setResultsPerPage(_resultsPerPage);
			tQuery.setScope(_store.getManifestId(tManifestShortId));
		} catch (ParseException tExcpt) {
			pRes.sendError(pRes.SC_BAD_REQUEST,"Failed to parse date paratmeters " + tExcpt);
			return;
		} catch (URISyntaxException tExcpt) {
			pRes.sendError(pRes.SC_BAD_REQUEST,"Failed to parse uri " + tExcpt);
			return;
		}

		Map<String, Object> tResults = _store.search(tQuery);

		pRes.setContentType("application/ld+json; charset=UTF-8");
		pRes.setCharacterEncoding("UTF-8");
		//**/_logger.debug(JsonUtils.toPrettyString(tAnnotationList));
		pRes.getWriter().println(JsonUtils.toPrettyString(tResults));
	}
}
