package uk.org.llgc.annotation.store.servlets.oa;

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

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.exceptions.MalformedAnnotation;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

public class Update extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Update.class.getName());

	protected AnnotationUtils _annotationUtils = null;
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		_annotationUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store = StoreConfig.getConfig().getStore();
	}
	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		_logger.debug("get called");
	}

	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
		try {
            UserService tUserService = new UserService(pReq.getSession());
            AuthorisationController tAuth = new AuthorisationController(tUserService);

			Map<String, Object> tAnnotationJSON = _annotationUtils.readAnnotaion(pReq.getInputStream(), StoreConfig.getConfig().getBaseURI(pReq) + "/annotation");

			_logger.debug("JSON in:");
			_logger.debug(JsonUtils.toPrettyString(tAnnotationJSON));
            Annotation tUpdate = new Annotation(tAnnotationJSON);
            tUpdate.setCreator(tUserService.getUser());

            Annotation tSavedAnno = _store.getAnnotation(tUpdate.getId());
            if (tAuth.allowUpdate(tSavedAnno, tUpdate)) {
                tSavedAnno = _store.updateAnnotation(tUpdate);

                pRes.setStatus(HttpServletResponse.SC_CREATED);
                pRes.setContentType("application/ld+json; charset=UTF-8");
                pRes.setCharacterEncoding("UTF-8");
                _logger.debug("JSON out:");
                _logger.debug(JsonUtils.toPrettyString(tSavedAnno.toJson()));
                pRes.getWriter().println(JsonUtils.toPrettyString(tSavedAnno.toJson()));
            } else {
                pRes.sendError(pRes.SC_FORBIDDEN, "You must be the owner of the annotation to edit it.");
            }
		} catch (IOException tException) {
			System.err.println("Exception occured trying to add annotation:");
			tException.printStackTrace();
			throw tException;
        } catch (MalformedAnnotation tExcpt) {
            tExcpt.printStackTrace();
            pRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pRes.setContentType("text/plain");
            pRes.getOutputStream().println("Falied to load annotation as it was badly informed: " + tExcpt.toString());
		}
	}
}
