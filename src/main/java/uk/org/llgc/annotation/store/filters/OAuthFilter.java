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
import uk.org.llgc.annotation.store.contollers.UserService;
import uk.org.llgc.annotation.store.StoreConfig;

public class OAuthFilter implements Filter {

	public void init(final FilterConfig filterConfig) {
	}

	public void doFilter(final ServletRequest pRequest, final ServletResponse pRes, final FilterChain pChain) throws IOException, ServletException {
        HttpServletRequest pReq = (HttpServletRequest)pRequest; 
        if (StoreConfig.getConfig().isAuth()) {
            HttpSession tSession = pReq.getSession();
            UserService tUsers = new UserService(tSession);
            if (!tUsers.isAuthenticated()) {
                String tCallingURL = pReq.getRequestURI();
                if (pReq.getQueryString() != null) {
                    tCallingURL += "?" + pReq.getQueryString();
                }

                tSession.setAttribute("oauth_url", tCallingURL);        
                if (StoreConfig.getConfig().getAuthTargets().size() == 1) {
                    // if there is only 1 target forward straight onto the oauth process
                    ((HttpServletResponse)pRes).sendRedirect("/login?type=" + StoreConfig.getConfig().getAuthTargets().get(0).getId());
                } else {
                    // Otherwise ask the user how they want to authenticate
                    ((HttpServletResponse)pRes).sendRedirect("/login.xhtml");
                }
                return;
            } else {
                System.out.println("Found user: " + tSession.getAttribute("user"));
            }
        }
        pChain.doFilter(pReq, pRes);
	}

	public void destroy() {
	}
}
