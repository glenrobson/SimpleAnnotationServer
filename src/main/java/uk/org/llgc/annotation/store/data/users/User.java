package uk.org.llgc.annotation.store.data.users;

import com.github.scribejava.core.model.OAuth2AccessToken;

public class User {
    protected String _id = "";
    protected String _name = "";
    protected String _email = "";
    protected String _pic = "";
    protected boolean _isAdmin = false;
    protected OAuth2AccessToken _token = null;

    public User() {
    }

    public boolean isAdmin() {
        return _isAdmin;
    }

    public void setAdmin(final boolean pValue) {
        _isAdmin = pValue;
    }

    public String getAvatar() {
       /* String[] tNames = _name.split(" ");
        if (tNames.length > 1) {
            return tNames[0].substring(0,1).toUpperCase() + tNames[1].substring(0,1).toUpperCase();
        } else { */
            return _name.substring(0,1).toUpperCase();
        // }
    }

    public String toString() {
        return "Id: " + _id + "\nName: " + _name + "\nEmail: " + _email + "\nPic: " + _pic;
    }

    /**
     * Get id.
     *
     * @return id as String.
     */
    public String getId() {
        return _id;
    }
    
    /**
     * Set id.
     *
     * @param id the value to set.
     */
    public void setId(final String pId) {
         _id = pId;
    }
    
    /**
     * Get name.
     *
     * @return name as String.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * Set name.
     *
     * @param name the value to set.
     */
    public void setName(final String pName) {
         _name = pName;
    }
    
    /**
     * Get email.
     *
     * @return email as String.
     */
    public String getEmail() {
        return _email;
    }
    
    /**
     * Set email.
     *
     * @param email the value to set.
     */
    public void setEmail(final String pEmail) {
         _email = pEmail;
    }
    
    /**
     * Get pic.
     *
     * @return pic as String.
     */
    public String getPicture() {
        return _pic;
    }
    
    /**
     * Set pic.
     *
     * @param pic the value to set.
     */
    public void setPicture(final String pPic) {
         _pic = pPic;
    }
    
    /**
     * Get token.
     *
     * @return token as OAuth2AccessToken.
     */
    public OAuth2AccessToken getToken() {
        return _token;
    }
    
    /**
     * Set token.
     *
     * @param token the value to set.
     */
    public void setToken(final OAuth2AccessToken pToken) {
         _token = pToken;
    }
}
