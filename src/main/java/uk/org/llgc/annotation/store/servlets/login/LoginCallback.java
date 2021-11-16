package uk.org.llgc.annotation.store.servlets.login;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

import java.util.concurrent.ExecutionException;

import java.io.IOException;

import java.util.Collections;
import java.util.Random;
import java.util.Map;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.users.LocalUser;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;
import uk.org.llgc.annotation.store.controllers.UserService;
import uk.org.llgc.annotation.store.StoreConfig;

import com.github.jsonldjava.utils.JsonUtils;

public class LoginCallback extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(LoginCallback.class.getName()); 

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        HttpSession tSession = pReq.getSession();
        try {
            OAuthTarget tTarget = (OAuthTarget)tSession.getAttribute("oauth_target");
            final OAuth20Service service = new ServiceBuilder(tTarget.getClientId())
                    .apiSecret(tTarget.getClientSecret())
                    .callback(StoreConfig.getConfig().getBaseURI(pReq) + "/login-callback")
                    .build(tTarget.getEndpoints());

            OAuth2AccessToken accessToken = service.getAccessToken(pReq.getParameter("code"));

            final OAuthRequest request = new OAuthRequest(Verb.GET, tTarget.getMapping().getEndpoint());
            service.signRequest(accessToken, request);
            Response tResponse = service.execute(request);
            User tUser = tTarget.getMapping().createUser(StoreConfig.getConfig().getBaseURI(pReq),(Map<String,Object>)JsonUtils.fromString(tResponse.getBody()));
            tUser.setToken(accessToken);
            tUser.setAuthenticationMethod(tTarget.getId());
                
            UserService tUsers = new UserService(pReq);
            tUsers.setUser(tUser);
            if (tSession.getAttribute("oauth_url") != null) {
                pRes.sendRedirect((String)tSession.getAttribute("oauth_url"));
            } else {
                pRes.sendRedirect("collections.xhtml");
            }
        } catch (InterruptedException tExcpt) {
            tExcpt.printStackTrace();
        } catch (ExecutionException tExcpt) {
            tExcpt.printStackTrace();
            // should redirect to login fail page
        }
    }

    // For local login
	public void doPost(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        final String tEmail = pReq.getParameter("email");
        final String tPassword = pReq.getParameter("password");
        HttpSession tSession = pReq.getSession();
        UserService tUsers = new UserService(pReq);

        LocalUser tUser = tUsers.getLocalUser(tEmail);
        // If user not set by email is registered as a user
        // then set password
        if (tUser != null && !tUser.hasPassword()) {
            tUsers.setUser(tUser);
            pRes.sendRedirect("/profile.xhtml");
        } else if (tUser != null && tUser.authenticate(tPassword)) {
            tUsers.setUser(tUser);
            if (tSession.getAttribute("oauth_url") != null) {
                pRes.sendRedirect((String)tSession.getAttribute("oauth_url"));
            } else {
                if (tUser.isAdmin()) {
                    pRes.sendRedirect("/admin/users.xhtml");
                } else {
                    pRes.sendRedirect("/collections.xhtml");
                }
            }

        } else {
            //throw 401
            pRes.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Failed to authenticate " + tEmail);
        }

        /*LocalAuth tAuth = StoreConfig.getConfig().getLocalAuth();
        if (tAuth.authenticate(tEmail, tPassword)) {
            try {
                UserService tUsers = new UserService(tSession);
                tUsers.setUser(tAuth.getUser(tEmail, StoreConfig.getConfig().getBaseURI(pReq)));
                if (tSession.getAttribute("oauth_url") != null) {
                    pRes.sendRedirect((String)tSession.getAttribute("oauth_url"));
                } else {
                    pRes.sendRedirect("/admin/users.xhtml");
                }
            } catch (URISyntaxException tExcpt) {
                String tMessage = "Config error in users config file. User " + tEmail + " has an invalid value for id";
                System.err.println(tMessage);
                tExcpt.printStackTrace();
                pRes.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tMessage);
            }
        } else {
            //throw 401
            pRes.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Failed to authenticate " + tEmail);
        }*/
    }
}
