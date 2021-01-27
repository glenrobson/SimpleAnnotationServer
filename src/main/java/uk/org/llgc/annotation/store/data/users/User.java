package uk.org.llgc.annotation.store.data.users;

import com.github.scribejava.core.model.OAuth2AccessToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Date;

public class User {
	protected static Logger _logger = LogManager.getLogger(User.class.getName());

    protected String _id = "";
    protected String _shortId = "";
    protected String _name = "";
    protected String _email = "";
    protected String _pic = "";
    protected boolean _isAdmin = false;
    protected OAuth2AccessToken _token = null;
    protected String _authenticationMethod = "";
    protected Date _created = null;
    protected Date _lastModified = null;

    public User() {
        _created = new Date();
        _lastModified = _created;
    }

    public Date getCreated() {
        return _created;
    }

    public void setCreated(final Date pDate) {
        _created = pDate;
    }

    public Date getLastModified() {
        return _lastModified;
    }

    public void setLastModified(final Date pDate) {
        _lastModified = pDate;
    }

    public Date updateLastModified() {
        _lastModified = new Date();
        return _lastModified;
    }

    public boolean isAdmin() {
        return _isAdmin;
    }

    public void setAdmin(final boolean pValue) {
        _isAdmin = pValue;
    }

    public String getAuthenticationMethod() {
        return _authenticationMethod;
    }

    public void setAuthenticationMethod(final String pAuthMethod) {
        _authenticationMethod = pAuthMethod;
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
        StringBuffer tBuffer = new StringBuffer("Id: ");
        tBuffer.append(_id);
        tBuffer.append("\nShortid: ");
        tBuffer.append(_shortId);
        tBuffer.append("\nName: ");
        tBuffer.append(_name);
        tBuffer.append("\nEmail: ");
        tBuffer.append(_email);
        tBuffer.append("\nPic: ");
        tBuffer.append(_pic);
        tBuffer.append("\nCreated: ");
        if (_created != null) {
            tBuffer.append(_created.getTime());
        } else {
            tBuffer.append(" is null");
        }
        tBuffer.append("\nAdmin: ");
        tBuffer.append(_isAdmin);
        tBuffer.append("\nAuth method: ");
        tBuffer.append(_authenticationMethod);

        return tBuffer.toString();
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
    public void setId(final String pId) throws URISyntaxException {
        new URI(pId);
         _id = pId;
    }

    public String getShortId() {
        return _shortId;
    }

    public void setShortId(final String pShortId) {
        _shortId = pShortId;
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

    public boolean equals(final Object pOther) {
        if (!(pOther instanceof User)) {
            return false;
        }
        User pOtherUser = (User)pOther;
        if (_token != null && pOtherUser.getToken() != null) {
            if (!_token.getAccessToken().equals(pOtherUser.getToken().getAccessToken())) {
                _logger.debug("Token different");
                return false;
            }
        } else {
            if (!(_token == null && pOtherUser.getToken() == null)) {
                _logger.debug("Token different one was null");
                return false;
            }
        }
        if (_pic == null) {
            if (pOtherUser.getPicture() != null) {
                _logger.debug("Pic different");
                return false;
            }
        } else if (!_pic.equals(pOtherUser.getPicture())) {
            _logger.debug("Pic different");
            return false;
        }
        _logger.debug("ID " + _id.equals(pOtherUser.getId()));
        _logger.debug("shortid " + _shortId.equals(pOtherUser.getShortId()));
        _logger.debug("name " + _name.equals(pOtherUser.getName()));
        _logger.debug("email " + _email.equals(pOtherUser.getEmail()));
        _logger.debug("admin " + (_isAdmin == pOtherUser.isAdmin()));
        _logger.debug("authMethod " + _authenticationMethod.equals(pOtherUser.getAuthenticationMethod()));
        return _id.equals(pOtherUser.getId()) 
                && _shortId.equals(pOtherUser.getShortId())
                && _name.equals(pOtherUser.getName())
                && _email.equals(pOtherUser.getEmail())
                && _isAdmin == pOtherUser.isAdmin()
                && _created.equals(pOtherUser.getCreated())
                && _authenticationMethod.equals(pOtherUser.getAuthenticationMethod());
    }
}
