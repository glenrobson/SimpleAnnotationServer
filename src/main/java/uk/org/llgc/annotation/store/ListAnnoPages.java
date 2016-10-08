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

public class ListAnnoPages extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(ListAnnoPages.class.getName()); 
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")),StoreConfig.getConfig().getEncoder());
		_store = StoreConfig.getConfig().getStore();
		_store.init(_annotationUtils);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		List<PageAnnoCount> tAnnotations = _store.listAnnoPages();
		_logger.debug(tAnnotations);

		StringBuffer tContent = new StringBuffer();
		for (PageAnnoCount tPage : tAnnotations) {
			tContent.append("<li><a href=\"/annotation/search?uri=");
			tContent.append(tPage.getPageId());
			tContent.append("\">");
			tContent.append(tPage.getPageId());
			tContent.append("</a> ");
			tContent.append("(");
			tContent.append(tPage.getCount());
			tContent.append(" annotations)");
			tContent.append("</li>");
		}

		File tTemplate = new File(new File(super.getServletContext().getRealPath("/templates")), "list.template");
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
