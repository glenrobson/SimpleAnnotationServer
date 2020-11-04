


package uk.org.llgc.annotation.store.servlets.login;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Logout extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(Logout.class.getName()); 
    protected String _redirectURL = "index.html";

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
        if (pConfig.getInitParameter("post_logout_url") != null) {
            _redirectURL = pConfig.getInitParameter("post_logout_url");
        }
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        HttpSession tSession = pReq.getSession();
        tSession.invalidate();
        pRes.sendRedirect(_redirectURL);
        return; 
    }
}
