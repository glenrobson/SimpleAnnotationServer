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

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.IDConflictException;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;

import java.net.URISyntaxException;

public class Create extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Create.class.getName());
	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(pReq.getInputStream(), StoreConfig.getConfig().getBaseURI(pReq) + "/annotation");
		if (tAnnotationJSON.get("@context") instanceof String) {
			Map<String,Object> tJsonContext = (Map<String,Object>)JsonUtils.fromInputStream(super.getServletContext().getResourceAsStream("/contexts/iiif-2.0.json"));
			tAnnotationJSON.put("@context",tJsonContext.get("@context"));//"http://localhost:8080/bor/contexts/iiif-2.0.json"); // must have a remote context for a remote repo
		}
		_logger.debug("JSON in:");
		_logger.debug(JsonUtils.toPrettyString(tAnnotationJSON));
		try {
			Annotation tAnno = _store.addAnnotation(new Annotation(tAnnotationJSON));
            AnnotationList tAnnotationList = new AnnotationList();
            tAnnotationList.add(tAnno);

			pRes.setStatus(HttpServletResponse.SC_CREATED);
			pRes.setContentType("application/ld+json; charset=UTF-8");
			pRes.setCharacterEncoding("UTF-8");
			_logger.debug("JSON out:");
			_logger.debug(JsonUtils.toPrettyString(tAnnotationList.toJson()));
			pRes.getWriter().println(JsonUtils.toPrettyString(tAnnotationList.toJson()));
		} catch (IDConflictException tException) {
			tException.printStackTrace();
			pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			pRes.setContentType("text/plain");
			pRes.getOutputStream().println("Failed to load annotation due to conflict in ID: " + tException.toString());
        } catch (MalformedAnnotation tExcpt) {
            tExcpt.printStackTrace();
            pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pRes.setContentType("text/plain");
            pRes.getOutputStream().println("Falied to load annotation as it was badly informed: " + tExcpt.toString());
		} catch (IOException tException) {

			System.err.println("Exception occured trying to add annotation:");
			tException.printStackTrace();
			throw tException;
		}
	}
}
