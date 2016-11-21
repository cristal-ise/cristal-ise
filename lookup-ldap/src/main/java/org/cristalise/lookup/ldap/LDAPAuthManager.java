/**
 * This file is part of the CRISTAL-iSE LDAP lookup plugin.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.lookup.ldap;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.Logger;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

public class LDAPAuthManager implements Authenticator {

    protected LDAPConnection mLDAPConn;
    protected LDAPProperties ldapProps;

    public LDAPAuthManager() {}

    @Override
    public boolean authenticate(String agentName, String password, String resource) 
            throws InvalidDataException, ObjectNotFoundException
    {
        ldapProps = new LDAPProperties(Gateway.getProperties());

        if (ldapProps.mHost!=null && ldapProps.mPort!= null && ldapProps.mLocalPath!=null ) {
            try { 
                // anonymously bind to LDAP and find the agent entry for the username
                ldapProps.mUser = "";
                ldapProps.mPassword = "";
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
                LDAPLookup anonLookup = new LDAPLookup();
                anonLookup.initPaths(ldapProps);
                anonLookup.open(this);
                String agentDN = anonLookup.getFullDN(anonLookup.getAgentPath(agentName));
                anonLookup.close();

                //found agentDN, try to log in with it
                Logger.msg(5, "LDAPAuthManager.authenticate() - agentDN:: "+agentDN);
                ldapProps.mUser = agentDN;
                ldapProps.mPassword = password;
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
                return true;
            } 
            catch (LDAPException e) {
                Logger.error(e);
                return false;
            }
        }
        else {
            throw new InvalidDataException("Cannot log in. Some connection properties are not set.");
        }

    }

    @Override
    public boolean authenticate(String resource) throws InvalidDataException, ObjectNotFoundException {
        ldapProps = new LDAPProperties(Gateway.getProperties());

        if (ldapProps.mUser == null || ldapProps.mUser.length()==0 || ldapProps.mPassword == null || ldapProps.mPassword.length()==0) {
            throw new InvalidDataException("LDAP root user properties not found in config.");
        }

        try {
            mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
            return true;
        }
        catch (LDAPException e) {
            Logger.error(e);
            return false;
        }
    }

    @Override
    public LDAPConnection getAuthObject() {
        if (mLDAPConn==null || !mLDAPConn.isConnected()) {
            Logger.warning("LDAPAuthManager.getAuthObject() - lost connection to LDAP server. Attempting to reconnect.");
            try {
                mLDAPConn = LDAPLookupUtils.createConnection(ldapProps);
            }
            catch (LDAPException ex) {
                Logger.error(ex);
            }
        }
        return mLDAPConn;
    }

    @Override
    public void disconnect() {
        Logger.msg(1, "LDAPAuthManager.disconnect() - Shutting down LDAP connection.");
        if (mLDAPConn != null) {
            try {
                mLDAPConn.disconnect();
            }
            catch (LDAPException e) {
                Logger.error(e);
            }
            mLDAPConn = null;
        }
    }
}
