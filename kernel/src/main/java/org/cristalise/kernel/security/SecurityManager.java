/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.security;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.cristalise.kernel.SystemProperties.Shiro_iniFile;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SECURITY_ACTION;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.SECURITY_DOMAIN;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyUtility;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class SecurityManager {
    
    private static final String securityMsgBegin = "[errorMessage]";
    private static final String securityMsgEnd   = "[/errorMessage]";

    /**
     * 
     * @throws InvalidDataException
     */
    public SecurityManager() throws InvalidDataException {
        setupShiro();
    }

    /**
     * 
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public void authenticate() throws InvalidDataException, ObjectNotFoundException {
        //NOTE: no code required because shiro cannot authenticate users without a password, and the current
        //setup does not allow us to create the 'system' Agent with password. Also the original auth.authenticate("system") 
        //code simply sets up the connection to the underlying technology (LDAP/AD/JDBC) to 'authenticate' the system user
    }

    public AgentProxy authenticate(String agentName, String agentPassword, String resource)
            throws InvalidDataException, ObjectNotFoundException
    {
        return authenticate(agentName, agentPassword, resource, true, null);
    }

    /**
     * 
     * @param agentName
     * @param agentPassword
     * @param resource
     * @param isClient
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public AgentProxy authenticate(String agentName, String agentPassword, String resource, boolean isClient)
            throws InvalidDataException, ObjectNotFoundException
    {
        return authenticate(agentName, agentPassword, resource, isClient, null);
    }

    /**
     * 
     * @param agentName
     * @param agentPassword
     * @param resource 
     * @param isClient ItemProxy should only be used in the client processes
     * @return AgentProxy of the user or returns null isClient is true
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public AgentProxy authenticate(String agentName, String agentPassword, String resource, boolean isClient, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException
    {
        if (!shiroAuthenticate(agentName, agentPassword)) throw new InvalidDataException("Login failed");

        // It can be invoked before Lookup is initialised
        if (isClient && Gateway.getLookup() != null) return Gateway.getAgentProxy(agentName, transactionKey);
        else                                         return null;
    }

    /**
     * 
     * @param agent
     * @return
     */
    public Subject getSubject(AgentPath agent) {;
        return getSubject(agent.getAgentName());
    }

    /**
     * 
     * @param principal
     * @return
     */
    public Subject getSubject(String principal) {
        PrincipalCollection principals = new SimplePrincipalCollection(principal, principal);
        return new Subject.Builder().principals(principals).buildSubject();
    }

    /**
     * Loads shiro.ini file from a file or from the classpath (default)
     */
    private void setupShiro() {
        String shiroIni = Shiro_iniFile.getString();

        if (isBlank(shiroIni)) shiroIni = "classpath:shiro.ini";
        else                   shiroIni = "file:" + shiroIni;

        Ini sIni = Ini.fromResourcePath(shiroIni);

//        if (! sIni.containsKey("ds.password")) {
//            try {
//                String pwd = FileStringUtility.file2String(sIni.getSectionProperty("ds", "passwordFile"));
//                sIni.setSectionProperty("ds", "password", pwd);
//                pwd = "";
//            }
//            catch (IOException e) {
//            }
//        }

        //FIXME: replace the use of IniSecurityManagerFactory with shiro Environment initialization
        Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory(sIni);

        org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        log.info("setupShiro() - Done inifile:{}", shiroIni);
    }

    /**
     * Reads the message from the exception that can be show to the user.
     * 
     * @param ex the exception to be processed
     * @return returns the message or null if nothing was found
     */
    public static String decodePublicSecurityMessage(Throwable ex) {
        String msg = StringUtils.substringBetween(ex.getMessage(), securityMsgBegin, securityMsgEnd);

        if (isBlank(msg) && ex.getCause() != null) {
            return decodePublicSecurityMessage(ex.getCause());
        }

        return msg;
    }

    /**
     * Wraps the massage with specific tokens indicating the the exception has a message to the user.
     * 
     * @param msg the message to be wrapped
     * @return the wrapped message
     */
    public static String encodePublicSecurityMessage(String msg) {
        return securityMsgBegin + msg + securityMsgEnd;
    }

    
    /**
     * 
     * @param agentName
     * @param agentPassword
     * @return
     */
    private boolean shiroAuthenticate(String agentName, String agentPassword) throws InvalidDataException {
        Subject agentSubject = getSubject(agentName);

        if ( !agentSubject.isAuthenticated() ) {
            UsernamePasswordToken token = new UsernamePasswordToken(agentName, agentPassword);

            token.setRememberMe(true);

            try {
                agentSubject.login(token);
                return true;
            }
            catch (Exception ex) {
              //NOTE: Enable this log for testing security problems only, but always remove it when merged
              //Logger.error(ex);

              String publicMsg = decodePublicSecurityMessage(ex);

              if (StringUtils.isNotBlank(publicMsg)) {
                log.debug("shiroAuthenticate() - Failed with public message:{}", publicMsg);
                throw new InvalidDataException(encodePublicSecurityMessage(publicMsg));
              }
            }
        }

        return false;
    }

    /**
     *
     * @param agent
     * @param act
     * @param itemPath
     * @param transactionKey
     * @return
     * @throws AccessRightsException
     * @throws ObjectNotFoundException
     */
    public boolean checkPermissions(AgentPath agent, Activity act, ItemPath itemPath, TransactionKey transactionKey)
            throws AccessRightsException, ObjectNotFoundException
    {
        String domain = getWildcardPermissionDomain(itemPath, transactionKey);
        String action = getWildcardPermissionAction(act);
        String target = PropertyUtility.getPropertyValue(itemPath, NAME, "", transactionKey);

        //The Shiro's WildcardPermission string 
        String permission = domain+":"+action+":"+target;

        log.debug("checkPermissions() - agent:'{}' permission:'{}'", agent.getAgentName(), permission);

        return getSubject(agent).isPermitted(permission);
    }
    
    /**
     * 
     * @param itemPath
     * @return
     * @throws ObjectNotFoundException Item was not found 
     * @throws AccessRightsException 
     */
    private String getWildcardPermissionDomain(ItemPath itemPath, TransactionKey transactionKey) throws ObjectNotFoundException, AccessRightsException {
        String type   = PropertyUtility.getPropertyValue(itemPath, TYPE, "", transactionKey);
        String domain = PropertyUtility.getPropertyValue(itemPath, SECURITY_DOMAIN, type, transactionKey);

        if (isBlank(domain)) throw new AccessRightsException("Domain was blank - Specify 'SecurityDomain' or 'Type' ItemProperties");

        return domain;
    }

    /**
     * 
     * @param act
     * @return
     * @throws AccessRightsException 
     */
    private String getWildcardPermissionAction(Activity act) throws AccessRightsException {
        String action = (String) act.getBuiltInProperty(SECURITY_ACTION, "");

        if (isBlank(action)) action = act.getName();
        if (isBlank(action)) throw new AccessRightsException("Action was blank - Specify 'SecurityAction' or 'Name' ActivityProperties");

        return action;
    }
}
