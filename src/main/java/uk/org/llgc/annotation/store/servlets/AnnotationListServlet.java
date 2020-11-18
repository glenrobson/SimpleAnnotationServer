package uk.org.llgc.annotation.store.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.github.jsonldjava.utils.JsonUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import uk.org.llgc.annotation.store.adapters.StoreAdapter;
import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.AnnotationUtils;
import uk.org.llgc.annotation.store.data.Canvas;
import uk.org.llgc.annotation.store.data.AnnotationList;
import uk.org.llgc.annotation.store.contollers.UserService;

public class AnnotationListServlet extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(AnnotationList.class.getName()); 
	protected StoreAdapter _store = null;

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
		_store = StoreConfig.getConfig().getStore();
        _store.init(new AnnotationUtils());
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        UserService tUserService = new UserService(pReq.getSession());
        String[] tRequestURI = pReq.getRequestURI().split("/");
        String tCanvasShortID = tRequestURI[tRequestURI.length -1].replace(".json","");

        Canvas tCanvas = _store.resolveCanvas(tCanvasShortID);

        AnnotationList tList = _store.getAnnotationsFromPage(tUserService.getUser(), tCanvas);
        tList.setId(StoreConfig.getConfig().getBaseURI(pReq) + "/annotation/list/" + tCanvasShortID + ".json");

        pRes.setContentType("application/ld+json; charset=UTF-8");
        pRes.setCharacterEncoding("UTF-8");
        pRes.getWriter().println(JsonUtils.toPrettyString(tList.toJson()));
    }
}
