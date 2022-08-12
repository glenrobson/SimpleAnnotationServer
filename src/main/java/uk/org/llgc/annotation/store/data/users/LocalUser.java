package uk.org.llgc.annotation.store.data.users;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LocalUser extends User {
    public final static String AUTH_METHOD = "local SAS authentication";
    
    // bcrypt hash of password
    protected String _password = null;

    public LocalUser() {
        super();
    }

    public PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    public boolean hasPassword() {
        return _password != null && _password.trim().length() != 0;
    }

    public void setPassword(final String pPassword) {
        this.setPassword(pPassword, true);
    }

    public void setPassword(final String pPassword, final boolean pEncode) {
        if (pEncode) {
            PasswordEncoder tEncoder = this.getEncoder();
            _password = this.getEncoder().encode(pPassword);
        } else {
            _password = pPassword;
        }
    }

    public String getPassword() {
        return _password;
    }

    public boolean authenticate(final String pPassword) {
        return this.getEncoder().matches(pPassword, _password);
    }

    public void setAuthenticationMethod() {
    }

    public String getAuthenticationMethod() {
        return AUTH_METHOD;
    }

    public String toString() {
        StringBuffer tBuffer = new StringBuffer("Local User:\n");

        tBuffer.append("Has password: ");
        tBuffer.append(this.hasPassword());
        tBuffer.append("\n");
        tBuffer.append(super.toString());

        return tBuffer.toString();
    }
}
