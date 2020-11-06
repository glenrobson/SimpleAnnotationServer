package uk.org.llgc.annotation.store.data.login;

import java.util.Map;

import uk.org.llgc.annotation.store.data.users.User;

public class UserMapping {
    protected String _type = "";
    protected String _endpoint = "";
    protected Map<String,String> _mapping = null;
    public UserMapping(final String pType, final Map<String, Object> pConfig) {
        _type = pType;
        _endpoint = (String)pConfig.get("endpoint");

        _mapping = (Map<String,String>)pConfig.get("responseKeys");
    }

    public String getEndpoint() {
        return _endpoint;
    }

    public User createUser(final String pBaseURL, final Map<String,Object> pResponse) {
        User tUser = new User();
        tUser.setId(pBaseURL + "/user/" + Math.abs(_type.hashCode()) + "/" + this.getKey("id", pResponse));
        tUser.setShortId(_type + "_" + this.getKey("id", pResponse));
        if (this.isIn("name", pResponse)) {
            tUser.setName(this.getKey("name", pResponse));
        }
        if (this.isIn("email", pResponse)) {
            tUser.setEmail(this.getKey("email", pResponse));
        }
        if (this.isIn("pic", pResponse)) {
            tUser.setPicture(this.getKey("pic", pResponse));
        }
        
        return tUser;
    }

    protected boolean isIn(final String pKey, final Map<String, Object> pMap) {
        if (_mapping.get(pKey) != null) {
            return pMap.get(_mapping.get(pKey)) != null;
        } else {
            return false;
        }
    }

    protected String getKey(final String pKey, final Map<String, Object> pMap) {
        return pMap.get(_mapping.get(pKey)).toString();
    }
}
