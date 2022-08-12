package uk.org.llgc.annotation.store.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import uk.org.llgc.annotation.store.servlets.login.LoginCallback;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.controllers.AuthorisationController;
import uk.org.llgc.annotation.store.StoreConfig;

public class AdminFilter implements Filter {
    protected String _loginPage = "/admin.xhtml";

	public void init(final FilterConfig pConfig) {
	}

	public void doFilter(final ServletRequest pRequest, final ServletResponse pRes, final FilterChain pChain) throws IOException, ServletException {
        HttpServletRequest pReq = (HttpServletRequest)pRequest; 
        if (StoreConfig.getConfig().isAuth()) {
            HttpSession tSession = pReq.getSession();
            UserService tUsers = new UserService(pReq);
            AuthorisationController tAuth = new AuthorisationController(tUsers);
            if (!tUsers.isAuthenticated() && !tAuth.allowThrough((HttpServletRequest)pRequest)) {
                String tCallingURL = pReq.getRequestURI();
                if (pReq.getQueryString() != null) {
                    tCallingURL += "?" + pReq.getQueryString();
                }

                tSession.setAttribute("oauth_url", tCallingURL);        
               
                ((HttpServletResponse)pRes).sendRedirect(_loginPage);
                //}
                return;
            /*} else {
                System.out.println("Found logged in user: " + tSession.getAttribute("user"));*/
            }
        }
        pChain.doFilter(pReq, pRes);
	}

	public void destroy() {
	}
}
