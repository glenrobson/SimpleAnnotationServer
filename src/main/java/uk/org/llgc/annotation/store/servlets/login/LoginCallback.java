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

import java.util.concurrent.ExecutionException;

import java.io.IOException;

import java.util.Collections;
import java.util.Random;
import java.util.Map;

import uk.org.llgc.annotation.store.data.users.User;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;
import uk.org.llgc.annotation.store.contollers.UserService;
import uk.org.llgc.annotation.store.StoreConfig;

import com.github.jsonldjava.utils.JsonUtils;

public class LoginCallback extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(LoginCallback.class.getName()); 

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        for (String tKey : Collections.list(pReq.getParameterNames())) {
            System.out.println("Key: " + tKey + "\tValue: " + pReq.getParameter(tKey));
        }
        HttpSession tSession = pReq.getSession();
        System.out.println("Original URL: " + tSession.getAttribute("oauth_url"));
        System.out.println("Secret match (" + pReq.getParameter("state") + " = " + tSession.getAttribute("oauth_state") + ") is " + pReq.getParameter("state").equals(tSession.getAttribute("oauth_state")));
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
            User tUser = tTarget.getMapping().createUser((Map<String,Object>)JsonUtils.fromString(tResponse.getBody()));
            tUser.setToken(accessToken);
                
            UserService tUsers = new UserService(tSession);
            tUsers.setUser(tUser);
            if (tSession.getAttribute("oauth_url") != null) {
                pRes.sendRedirect((String)tSession.getAttribute("oauth_url"));
            } else {
                pRes.sendRedirect("index.html");
            }
        } catch (InterruptedException tExcpt) {
            tExcpt.printStackTrace();
        } catch (ExecutionException tExcpt) {
            tExcpt.printStackTrace();
            // should redirect to login fail page
        }
    }
}
