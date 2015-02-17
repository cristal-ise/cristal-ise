package org.cristalise.lookup.ldap;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.Logger;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

public class LDAPAuthManager implements Authenticator {

    protected LDAPConnection mLDAPConn;
    protected LDAPProperties ldapProps;
    
	public LDAPAuthManager() {
	}

	@Override
	public boolean authenticate(String agentName,
			String password, String resource) throws InvalidDataException, ObjectNotFoundException {
		
		ldapProps = new LDAPProperties(Gateway.getProperties());
		
        if (ldapProps.mHost!=null && ldapProps.mPort!= null && ldapProps.mLocalPath!=null )
        {
            try { // anonymously bind to LDAP and find the agent entry for the username
                ldapProps.mUser = "";
                ldapProps.mPassword = "";
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
                LDAPLookup anonLookup = new LDAPLookup();
                anonLookup.initPaths(ldapProps);
                anonLookup.open(this);
                String agentDN = anonLookup.getFullDN(anonLookup.getAgentPath(agentName));

                //found agentDN, try to log in with it
                ldapProps.mUser = agentDN;
                ldapProps.mPassword = password;
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
                return true;
            } catch (LDAPException e) {
                return false;
            }
        }
        else
        {
            throw new InvalidDataException("Cannot log in. Some connection properties are not set.");
        }

	}
	
	@Override
	public boolean authenticate(String resource) throws InvalidDataException, ObjectNotFoundException {
		ldapProps = new LDAPProperties(Gateway.getProperties());
		
		if (ldapProps.mUser == null || ldapProps.mUser.length()==0 ||
			ldapProps.mPassword == null || ldapProps.mPassword.length()==0)
			throw new InvalidDataException("LDAP root user properties not found in config.");
		try {
			mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
			return true;
		} catch (LDAPException e) {
            return false;
        }
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.process.auth.Authenticator#getAuthObject()
	 */
	@Override
	public LDAPConnection getAuthObject() {
        
		if (mLDAPConn==null || !mLDAPConn.isConnected()) {
            Logger.warning("LDAPAuthManager - lost connection to LDAP server. Attempting to reconnect.");
            try {
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
            } catch (LDAPException ex) { }
        }
        return mLDAPConn;
	}

	@Override
	public void disconnect() {
        Logger.msg(1, "LDAP Lookup: Shutting down LDAP connection.");
        if (mLDAPConn != null) {
            try {
                mLDAPConn.disconnect();
            } catch (LDAPException e) {
                Logger.error(e);
            }
            mLDAPConn = null;
        }
		
	}
}
