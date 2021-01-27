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

import org.apache.jena.rdf.model.Model;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.encoders.Encoder;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.Annotation;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;

public class Delete extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Delete.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
		Encoder tEncoder = StoreConfig.getConfig().getEncoder();
		AnnotationUtils tAnnoUtils = new AnnotationUtils(new File(super.getServletContext().getRealPath("/contexts")), tEncoder);
		_store.init(tAnnoUtils);
	}

	public void doDelete(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        UserService tUserService = new UserService(pReq.getSession());
        AuthorisationController tAuth = new AuthorisationController(tUserService);
		_logger.debug("uri " + pReq.getParameter("uri"));
		String tURI = pReq.getParameter("uri");

        Annotation tSavedAnno = _store.getAnnotation(tURI);
        if (tSavedAnno != null) {
            if (tAuth.allowDelete(tSavedAnno)) {
                _store.deleteAnnotation(tURI);
                pRes.setStatus(pRes.SC_NO_CONTENT);
            } else {
                pRes.sendError(pRes.SC_FORBIDDEN, "You must be the owner of the annotation to delete it.");
            }
        } else {
            pRes.setStatus(pRes.SC_NO_CONTENT, "Partly succeeded in that the annoation is no longer present but.. it wasn't there in the first place.");
        }
	}
}
