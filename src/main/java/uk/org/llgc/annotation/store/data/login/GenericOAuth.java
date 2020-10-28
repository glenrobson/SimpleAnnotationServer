package uk.org.llgc.annotation.store.data.login;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignature;
import com.github.scribejava.core.oauth2.bearersignature.BearerSignatureURIQueryParameter;

import java.util.Map;

public class GenericOAuth extends DefaultApi20 {

    protected String _accessTokenEndpoint = "";
    protected Verb _accessTokenVerb = null;
    protected String _authBaseUrl = "";

    public GenericOAuth(final Map<String,String> pConfig) {
        _accessTokenEndpoint = pConfig.get("accessTokenEndpoint");
        if (pConfig.get("accessTokenVerb").toUpperCase().equals("POST")) {
            _accessTokenVerb = Verb.POST;
        } else {
            _accessTokenVerb = Verb.GET;
        }
        _authBaseUrl = pConfig.get("authorizationBaseUrl");
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    // From: https://wp-oauth.com/docs/general/main-concepts/
    @Override
    public String getAccessTokenEndpoint() {
        return "https://catalog.metascripta.org/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://catalog.metascripta.org/oauth/authorize";
    }

    @Override
    public BearerSignature getBearerSignature() {
        return BearerSignatureURIQueryParameter.instance();
    }
}
