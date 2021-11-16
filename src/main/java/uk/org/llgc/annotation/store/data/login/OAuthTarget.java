package uk.org.llgc.annotation.store.data.login;

import java.lang.reflect.Method;

import java.util.Map;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.Verb;

import java.io.IOException;

public class OAuthTarget {
    protected String _id = "";
    protected DefaultApi20 _endpoints = null;
    protected String _clientId = "";
    protected String _clientSecret = "";
    protected String _scope = null;
    protected Map<String,String> _additionalParams = null;
    protected Button _button = null;
    protected UserMapping _mapping = null;

    public OAuthTarget(final Map<String, Object> pConfig) throws IOException {
        this.setId(pConfig);
        this.setEndpoints(pConfig);
        this.setClientId(pConfig);
        this.setClientSecret(pConfig);
        this.setScopes(pConfig);
        this.setAdditionalParams(pConfig);
        this.setButton(pConfig);
        this.setMapping(this.getId(), pConfig);
    }

    
    /**
     * Get id.
     *
     * @return id as String.
     */
    public String getId() {
        return _id;
    }

    protected void setId(final Map<String, Object> pConfig) {
        _id = (String)pConfig.get("id");
    }
    
    /**
     * Get endpoints.
     *
     * @return endpoints as DefaultApi20.
     */
    public DefaultApi20 getEndpoints() {
        return _endpoints;
    }

    protected void setEndpoints(final Map<String, Object> pConfig) throws IOException {
        if (((String)pConfig.get("class")).equals("uk.org.llgc.annotation.store.data.login.GenericOAuth")) {
            _endpoints = new GenericOAuth((Map<String, String>)pConfig.get("endpoints"));
        } else {    
            Class<?> tClass = null;
            try {
                tClass = Class.forName((String)pConfig.get("class"));
            } catch (ClassNotFoundException tExcpt) {
                throw new IOException("Failed to load auth class " + (String)pConfig.get("class") + " due to " + tExcpt);
            } catch (LinkageError tExcpt) {
                throw new IOException("Failed to load auth class " + (String)pConfig.get("class") + " due to " + tExcpt);
            }
            Method tMethod = null;
            try {
                tMethod = tClass.getMethod("instance", null); 
            } catch (NoSuchMethodException tExcpt) {
                throw new IOException("Failed to load auth class " + (String)pConfig.get("class") + " due to problem loading the instance() method: " + tExcpt);
            }
                
            try {    
                _endpoints = (DefaultApi20)tMethod.invoke(null);
            } catch (ExceptionInInitializerError tExcpt) {
                throw new IOException("Failed to load auth class " + (String)pConfig.get("class") + " due to problem running the instance() method: " + tExcpt);
            } catch (ReflectiveOperationException tExcpt) {
                throw new IOException("Failed to load auth class " + (String)pConfig.get("class") + " due to problem running the instance() method: " + tExcpt);
            }
        }
    }
    
    /**
     * Get clientId.
     *
     * @return clientId as String.
     */
    public String getClientId() {
        return _clientId;
    }
    
    protected void setClientId(final Map<String, Object> pConfig) {
        _clientId = (String)pConfig.get("clientId");
    }

    /**
     * Get clientSecret.
     *
     * @return clientSecret as String.
     */
    public String getClientSecret() {
        return _clientSecret;
    }

    protected void setClientSecret(final Map<String, Object> pConfig) {
        _clientSecret = (String)pConfig.get("clientSecret");
    }

    public String getScopes() {
        return _scope;
    }

    public void setScopes(final Map<String, Object> pConfig) {
        if (pConfig.get("scope") != null) {
            _scope = (String)pConfig.get("scope");
        } else {
            _scope = null;
        }
    }

    public Map<String,String> getAdditionalParams() {
        return _additionalParams;
    }

    public void setAdditionalParams(final Map<String, Object> pConfig) {
        if (pConfig.get("additionalParam") != null) {
            _additionalParams = (Map<String,String>)pConfig.get("additionalParam");
        } else {
            _additionalParams = null; // Optional config
        }
    }

    
    /**
     * Get button.
     *
     * @return button as Button.
     */
    public Button getButton() {
        return _button;
    }

    protected void setButton(final Map<String,Object> pConfig) {
        _button = new Button((Map<String,String>)pConfig.get("button"));
    }
    
    /**
     * Get mapping.
     *
     * @return mapping as UserMapping.
     */
    public UserMapping getMapping() {
        return _mapping;
    }

    protected void setMapping(final String pType, final Map<String,Object> pConfig) {
        _mapping = new UserMapping(pType, (Map<String,Object>)pConfig.get("userMapping"));
    }

}
