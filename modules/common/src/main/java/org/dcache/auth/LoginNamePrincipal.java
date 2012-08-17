package org.dcache.auth;

import java.security.Principal;
import java.io.Serializable;

/**
 * This Principal represents the untrusted username: the username that
 * the user wishes to become.  Typically, no checks are made to verify
 * that the end-user is identified as that user; there, it is
 * recommended not to use this principal for any authorisation decisions.
 *
 * @see UserNamePrincipal
 */
public class LoginNamePrincipal implements Principal, Serializable
{
    static final long serialVersionUID = -6665363415876118030L;

    private String _name;

    public LoginNamePrincipal(String name)
    {
        if (name == null) {
            throw new NullPointerException();
        }
        _name = name;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LoginNamePrincipal)) {
            return false;
        }
        LoginNamePrincipal other = (LoginNamePrincipal) obj;
        return other._name.equals(_name);
    }

    @Override
    public int hashCode()
    {
        return _name.hashCode();
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "]";
    }
}