package uk.org.llgc.annotation.store.data.login;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.net.URISyntaxException;

import uk.org.llgc.annotation.store.data.users.User;

public class LocalAuth {
    protected List<Map<String,String>> _users = new ArrayList<Map<String,String>>();
    protected String _id = "";
    protected String _type = "local";

    public LocalAuth() {
    }

    public void addUser(final Map<String,String> tUser) {
        _users.add(tUser);
    }

    public boolean authenticate(final String pEmail, final String pPassword) {
        for (Map<String, String> tUser : _users) {
            System.out.println("Looking for user " + pEmail + " found " + tUser.get("email"));
            if (tUser.get("email") != null && tUser.get("email").equals(pEmail)) {
                // found user
                // should encrypt password!
                return tUser.get("password") != null && tUser.get("password").equals(pPassword);
            }
        }
        // User not found
        return false;
    }

    public User getUser(final String pEmail, final String pBaseURI) throws URISyntaxException {
        User tUserObj = new User();
         
        for (Map<String, String> tUser : _users) {
            if (tUser.get("email") != null && tUser.get("email").equals(pEmail)) {
                if (tUser.get("id") != null) {
                    try {
                        tUserObj = User.createUserFromShortID(pBaseURI, tUser.get("id"));
                    } catch (URISyntaxException tExcpt) {
                        System.err.println("Failed to create ID for user " + tUser.get("id") + " as it is not a valid URI");
                        throw tExcpt;
                    }
                    tUserObj.setShortId(tUser.get("id"));
                } else {
                    throw new URISyntaxException(tUser.get("id"), "ID can't be empty for User " + pEmail);
                }

                
                tUserObj.setEmail(tUser.get("email"));
                tUserObj.setAuthenticationMethod("local");

                boolean admin = false;
                if (tUser.get("group") != null && tUser.get("group").equals("admin")) {
                    admin = true;
                }
                tUserObj.setAdmin(admin);

                if (tUser.get("name") != null) {
                    tUserObj.setName(tUser.get("name"));
                } else if (admin) {
                    tUserObj.setName("admin");
                }

                if (tUser.get("pic") != null) {
                    tUserObj.setPicture(tUser.get("pic"));
                } else if (admin) {
                    tUserObj.setPicture("/images/AdminIcon.svg");
                }

                break;
            }
        }
        return tUserObj;
    }

    public static LocalAuth createLocal(final Map<String, Object> pConfig) {
        LocalAuth tAuth = new LocalAuth();
        if (pConfig.get("id") != null) {
            tAuth.setId((String)pConfig.get("id"));
        }
        if (pConfig.get("type") != null) {
            tAuth.setType((String)pConfig.get("type"));
        }
        if (pConfig.get("users") != null) {
            List<Map<String,String>> tUsers = (List<Map<String,String>>)pConfig.get("users");
            for (Map<String,String> tUser : tUsers) {
                tAuth.addUser(tUser);
            }
        }
        return tAuth;
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
     * Get type.
     *
     * @return type as String.
     */
    public String getType() {
        return _type;
    }
    
    /**
     * Set type.
     *
     * @param type the value to set.
     */
    public void setType(final String pType) {
         _type = pType;
    }
}
