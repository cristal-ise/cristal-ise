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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.cristalise.kernel.SystemProperties.Shiro_iniFile;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SECURITY_ACTION;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.SECURITY_DOMAIN;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.env.BasicIniEnvironment;
import org.apache.shiro.env.Environment;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
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
import org.cristalise.kernel.utils.FileStringUtility;

import java.io.IOException;

@Getter
@Slf4j
public class SecurityManager {
    
    private static final String securityMsgBegin = "[errorMessage]";
    private static final String securityMsgEnd   = "[/errorMessage]";

    /**
     * Constructs a new instance of the SecurityManager class.
     * This constructor initializes the security environment by loading the 
     * Shiro configuration through the {@code setupShiro} method.
     *
     * @throws InvalidDataException if there is an issue with the initialization process
     *                              such as invalid data in the configuration.
     */
    public SecurityManager() throws InvalidDataException {
        setupShiro();
    }

    /**
     * Authenticates the system user to establish a connection with the underlying 
     * security technology (e.g., LDAP/AD/JDBC).
     * The method does not perform actual user authentication but initializes the 
     * necessary setup to authenticate the 'system' agent.
     * Note that due to restrictions in the current configuration, the system agent
     * cannot be created with a password. 
     *
     * @throws InvalidDataException      if the initialization process encounters invalid data.
     * @throws ObjectNotFoundException   if the 'system' agent is not found.
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
     * Authenticates an agent using the provided credentials and resource details.
     *
     * @param agentName     the name of the agent to authenticate
     * @param agentPassword the password of the agent
     * @param resource      the resource the agent is attempting to access
     * @param isClient      specifies if the caller is a client process
     * @return the authenticated AgentProxy object or null if {@code isClient} is true
     * 
     * @throws InvalidDataException      if the provided data, such as credentials, is invalid
     * @throws ObjectNotFoundException  if the agent or resource is not found
     */
    public AgentProxy authenticate(String agentName, String agentPassword, String resource, boolean isClient)
            throws InvalidDataException, ObjectNotFoundException
    {
        return authenticate(agentName, agentPassword, resource, isClient, null);
    }

    /**
     * Authenticates an agent using the provided credentials, resource details, and a transaction key.
     *
     * @param agentName       the name of the agent to authenticate
     * @param agentPassword   the password of the agent
     * @param resource        the resource the agent is attempting to access
     * @param isClient        specifies if the caller is a client process
     * @param transactionKey  the transaction key associated with the action
     * @return the authenticated AgentProxy object if the authentication is successful, or null if {@code isClient} is true
     * 
     * @throws InvalidDataException       if the provided credentials are invalid
     * @throws ObjectNotFoundException    if the agent, resource, or other associated objects are not found
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
     * Retrieves a Subject object based on the provided agent path.
     *
     * @param agent the AgentPath representing the agent for which the subject should be retrieved
     * @return the Subject associated with the given agent
     */
    public Subject getSubject(AgentPath agent) {;
        return getSubject(agent.getAgentName());
    }

    /**
     * Retrieves a Subject object based on the provided principal name.
     *
     * @param principal the principal name representing the identity for which the Subject should be retrieved
     * @return the Subject associated with the given principal
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
        String passwordFile = sIni.getSectionProperty("ds", "passwordFile");

        if (isNotBlank(passwordFile)) {
            log.info("setupShiro() - setting value for ds.password from passwordFile:{}", passwordFile);
            try {
                String pwd = FileStringUtility.file2String(passwordFile);
                sIni.setSectionProperty("ds", "password", pwd);
                pwd = null;
            }
            catch (IOException e) {
                log.error("setupShiro() - Failed to read passwordFile:{}", passwordFile, e);
                System.exit(1);
            }
        }

        Environment shiroEnv = new BasicIniEnvironment(sIni);
        SecurityUtils.setSecurityManager(shiroEnv.getSecurityManager());

        log.info("setupShiro() - DONE shiroIni:{}", shiroIni);
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
              //log.error("agentName:{}", agentName, ex);

              String publicMsg = decodePublicSecurityMessage(ex);

              if (isNotBlank(publicMsg)) {
                log.debug("shiroAuthenticate() - Failed with public message:{}", publicMsg);
                throw new InvalidDataException(encodePublicSecurityMessage(publicMsg));
              }
            }
        }

        return false;
    }

    /**
     * Checks whether the specified agent has permission to perform a given action 
     * on a specified item within the context of a transaction.
     *
     * @param agent           the {@code AgentPath} representing the agent whose permissions 
     *                        are being checked
     * @param act             the {@code Activity} representing the action being evaluated
     * @param itemPath        the {@code ItemPath} representing the target resource
     * @param transactionKey  the {@code TransactionKey} representing the transaction context
     * @return {@code true} if the agent has the required permissions, {@code false} otherwise
     * @throws AccessRightsException   if there is an error determining access rights
     * @throws ObjectNotFoundException if any of the specified objects (e.g., agent, activity, 
     *                                 or resource) cannot be found
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

    private String getWildcardPermissionDomain(ItemPath itemPath, TransactionKey transactionKey) throws ObjectNotFoundException, AccessRightsException {
        String type   = PropertyUtility.getPropertyValue(itemPath, TYPE, "", transactionKey);
        String domain = PropertyUtility.getPropertyValue(itemPath, SECURITY_DOMAIN, type, transactionKey);

        if (isBlank(domain)) throw new AccessRightsException("Domain was blank - Specify 'SecurityDomain' or 'Type' ItemProperties");

        return domain;
    }

    private String getWildcardPermissionAction(Activity act) throws AccessRightsException {
        String action = (String) act.getBuiltInProperty(SECURITY_ACTION, "");

        if (isBlank(action)) action = act.getName();
        if (isBlank(action)) throw new AccessRightsException("Action was blank - Specify 'SecurityAction' or 'Name' ActivityProperties");

        return action;
    }
}
