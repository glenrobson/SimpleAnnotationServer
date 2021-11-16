package uk.org.llgc.annotation.store.servlets.login;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

import uk.org.llgc.annotation.store.StoreConfig;
import uk.org.llgc.annotation.store.data.login.OAuthTarget;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OAuth extends HttpServlet {
	protected static Logger _logger = LogManager.getLogger(OAuth.class.getName()); 

	public void init(final ServletConfig pConfig) throws ServletException {
		super.init(pConfig);
	}

	public void doGet(final HttpServletRequest pReq, final HttpServletResponse pRes) throws IOException {
        HttpSession tSession = pReq.getSession();
        if (pReq.getParameter("type") != null) {
            OAuthTarget tTarget = StoreConfig.getConfig().getAuthTarget(pReq.getParameter("type"));
            if (tTarget != null) {
                final String secretState = tTarget.getId() + new Random().nextInt(999_999);
                final OAuth20Service service = new ServiceBuilder(tTarget.getClientId())
                        .apiSecret(tTarget.getClientSecret())
                        .defaultScope(tTarget.getScopes()) // replace with desired scope
                        .callback(StoreConfig.getConfig().getBaseURI(pReq) + "/login-callback")
                        .build(tTarget.getEndpoints());
                _logger.debug("Sending callback " + StoreConfig.getConfig().getBaseURI(pReq) + "/login-callback");        
                System.out.println("Sending callback " + StoreConfig.getConfig().getBaseURI(pReq) + "/login-callback with client_id " + tTarget.getClientId());        

                Map<String, String> additionalParams = new HashMap<>();
                if (tTarget.getAdditionalParams() != null) {
                    additionalParams = tTarget.getAdditionalParams();
                }
                //force to re-get refresh token (if user are asked not the first time)
                //additionalParams.put("prompt", "consent");
                final String authorizationUrl = service.createAuthorizationUrlBuilder()
                        .state(secretState)
                        .additionalParams(additionalParams)
                        .build();        
                tSession.setAttribute("oauth_state", secretState);        
                tSession.setAttribute("oauth_target", tTarget);        
                pRes.sendRedirect(authorizationUrl);
            } else {
                // Type parameter unrecognised
            }
        } else {
            // No type parameter sent
        }
    }
}
