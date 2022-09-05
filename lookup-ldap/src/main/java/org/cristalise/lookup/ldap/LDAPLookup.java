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

import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.ADD;
import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.DELETE;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import lombok.extern.slf4j.Slf4j;

/**
 * The LDAPLookup object, statically accessible through the Gateway, manages the LDAP connection for the cristal process. It provides:
 * <ul>
 * <li>Authentication - returning an AgentProxy object if a user has logged in
 * <li>System key generation - through the NextKeyManager
 * <li>Agent and Role lookup/modification - through the RoleManager
 * <li>
 */
@Slf4j
public class LDAPLookup implements LookupManager {

    protected LDAPAuthManager     mLDAPAuth;
    protected LDAPPropertyManager mPropManager;
    protected LDAPProperties      ldapProps;

    private String mGlobalPath, mRootPath, mLocalPath, mRoleTypeRoot, mItemTypeRoot, mDomainTypeRoot;

    /**
     *
     */
    public LDAPLookup() {
        super();
    }

    /**
     * Initializes the DN paths from the Root, global and local paths supplied by the LDAP properties.
     *
     * @param props
     */
    protected void initPaths(LDAPProperties props) {

        log.debug("LDAPLookup.initPaths(): - initialising with LDAPProperties");
        ldapProps = props;

        mGlobalPath = props.mGlobalPath;
        mRootPath   = props.mRootPath;
        mLocalPath  = props.mLocalPath;

        mItemTypeRoot   = "cn=entity," + props.mLocalPath;
        mDomainTypeRoot = "cn=domain," + props.mLocalPath;
        mRoleTypeRoot   = "cn=role," + props.mLocalPath;
    }

    /**
     * Initializes the LDAPLookup manager with the Gateway properties. This should be only done by the Gateway during initialisation.
     *
     * @param auth A LDAPAuthManager authenticator
     */
    @Override
    public void open(Authenticator auth) {
        if (ldapProps == null)
            initPaths(new LDAPProperties(Gateway.getProperties()));

        mLDAPAuth = (LDAPAuthManager) auth;
        mPropManager = new LDAPPropertyManager(this, mLDAPAuth);

    }

    @Override
    public void postStartServer() {
        //INIMPLEMENTED
    }

    @Override
    public void postBoostrap() {
        //INIMPLEMENTED
    }

    private void migrateOldRoles(TransactionKey transactionKey) {
        // search the mDomainPath tree uniqueMember=userDN
        // filter = objectclass=cristalrole AND uniqueMember=userDN

        String oldAgentPath = "cn=agent," + mDomainTypeRoot;
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        searchCons.setMaxResults(0);
        String[] attr = { LDAPConnection.ALL_USER_ATTRS };
        try {
            LDAPSearchResults res = mLDAPAuth.getAuthObject().search(
                    oldAgentPath,
                    LDAPConnection.SCOPE_SUB,
                    "(objectclass=cristalrole)",
                    attr,
                    false,
                    searchCons);

            Stack<LDAPEntry> toDelete = new Stack<LDAPEntry>();

            while (res.hasMore()) {
                LDAPEntry role = res.next();
                toDelete.push(role);
                String choppedRole = role.getDN().substring(0, role.getDN().lastIndexOf(oldAgentPath));
                if (choppedRole.length() == 0) continue;
                String[] roleComponents = choppedRole.split(",");
                String[] rolePathStr = new String[roleComponents.length];
                for (int i = 0; i < roleComponents.length; i++) {
                    log.info(i + ": " + roleComponents[i]);
                    if (roleComponents[i].matches("^cn=.*"))
                        rolePathStr[roleComponents.length - i - 1] = roleComponents[i].substring(3);
                }
                boolean hasJobList = role.getAttribute("jobList").getStringValue().equals("TRUE");
                RolePath newRole = new RolePath(rolePathStr, hasJobList);
                log.info("Migrating role: " + newRole.toString());
                try {
                    createRole(newRole, transactionKey);
                }
                catch (ObjectAlreadyExistsException e1) {
                    log.warn("Role " + newRole.toString() + " already exists");
                }
                catch (ObjectCannotBeUpdated e1) {
                    log.error("Could not migrate role " + newRole);
                }

                LDAPAttribute memberAttr = role.getAttribute("uniqueMember");
                if (memberAttr != null) {
                    String[] members = memberAttr.getStringValueArray();
                    for (String member : members) {
                        String uuid = member.substring(3, member.indexOf(','));
                        AgentPath agent;
                        try {
                            ItemPath item = new ItemPath(uuid);
                            agent = new AgentPath(item);
                            if (!agent.hasRole(newRole)) {
                                try {
                                    log.info("Adding agent " + agent.getAgentName() + " to new role " + newRole.toString());
                                    addRole(agent, newRole, transactionKey);
                                }
                                catch (Exception e) {
                                    log.error("Could not add agent " + agent.getAgentName() + " to role " + newRole);
                                }
                            }
                        }
                        catch (InvalidItemPathException e) {
                            log.error("Invalid agent in role " + newRole + ": " + uuid);
                        }
                    }
                }
            }
            while (!toDelete.isEmpty()) {
                try {
                    LDAPLookupUtils.delete(mLDAPAuth.getAuthObject(), toDelete.pop().getDN());
                }
                catch (Exception ex) { // must be out of order, try again next time
                    log.error("Error deleting old Role. ", ex);
                }
            }
        }
        catch (LDAPException e) {
            log.error("LDAP Exception migrating roles",e);
        }
    }

    /**
     * Gets the property manager, that is used to read and write cristal properties to the LDAP store.
     *
     * @return Returns the global LDAPPropertyManager.
     */
    public LDAPPropertyManager getPropManager() {
        return mPropManager;
    }

    /**
     * Disconnects the connection with the LDAP server during shutdown
     */
    @Override
    public void close() {
        log.info("LDAP Lookup: Shutting down LDAP connection.");
        if (mLDAPAuth != null) {
            mLDAPAuth.disconnect();
            mLDAPAuth = null;
        }
    }

    @Override
    public ItemPath resolvePath(DomainPath domPath, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException {
        LDAPEntry domEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(domPath), LDAPSearchConstraints.DEREF_ALWAYS);
        String entityKey = LDAPLookupUtils.getFirstAttributeValue(domEntry, "cn");

        log.debug("LDAPLookup.resolvePath() - DomainPath " + domPath + " is a reference to " + entityKey);

        String objClass = LDAPLookupUtils.getFirstAttributeValue(domEntry, "objectClass");

        ItemPath referencedPath = new ItemPath(entityKey);

        if (objClass.equals("cristalagent")) return new AgentPath(referencedPath);
        else                                 return referencedPath;
    }

    @Override
    public void add(Path path, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        try {
            checkLDAPContext(path);
            LDAPAttributeSet attrSet = createAttributeSet(path);
            LDAPEntry newEntry = new LDAPEntry(getFullDN(path), attrSet);
            LDAPLookupUtils.addEntry(mLDAPAuth.getAuthObject(), newEntry);

            // FIXME: Check if this is correct to call in the Lookup implementation
            if (path instanceof DomainPath)
                Gateway.sendProxyEvent(new ProxyMessage(null, path.toString(), ADD));
        }
        catch (LDAPException ex) {
            if (ex.getResultCode() == LDAPException.ENTRY_ALREADY_EXISTS)
                throw new ObjectAlreadyExistsException(
                        "Cannot add Path '" + path.getStringPath() + "' - LDAPException:" + ex.getLDAPErrorMessage());
            else
                throw new ObjectCannotBeUpdated("Cannot add Path '" + path.getStringPath() + "' - LDAPException:" + ex.getLDAPErrorMessage());
        }
    }

    // deletes a node
    // throws LDAPexception if node cannot be deleted (eg node is not a leaf)
    @Override
    public void delete(Path path, TransactionKey transactionKey) throws ObjectCannotBeUpdated {
        try {
            LDAPLookupUtils.delete(mLDAPAuth.getAuthObject(), getDN(path) + mLocalPath);
        }
        catch (LDAPException ex) {
            throw new ObjectCannotBeUpdated("Cannot delete Path '" + path.getStringPath() + "' - LDAPException:" + ex.getLDAPErrorMessage());
        }
        if (path instanceof DomainPath) {
            Gateway.sendProxyEvent(new ProxyMessage(null, path.toString(), DELETE));
        }
    }

    // change specs, add boolean alias leaf context
    protected void checkLDAPContext(Path path) {
        String dn = getFullDN(path);
        if (!LDAPLookupUtils.exists(mLDAPAuth.getAuthObject(), dn)) {
            String listDN[] = path.getPath();
            String name = "cn=" + path.getRoot() + "," + mLocalPath;
            int i = 0;
            while (i < listDN.length - 1) {
                name = "cn=" + LDAPLookupUtils.escapeDN(listDN[i]) + "," + name;
                if (!LDAPLookupUtils.exists(mLDAPAuth.getAuthObject(), name)) {
                    try {
                        // create cristalcontext
                        log.debug("addLDAPContext() context added " + name);
                        LDAPLookupUtils.createCristalContext(mLDAPAuth.getAuthObject(), name);
                    }
                    catch (Exception ex) {
                        log.error("addContext() ",ex);
                    }
                }
                i++;
            }
        }
    }

    public void createBootTree() {
        log.debug("Initializing LDAP Boot tree");

        // create org
        LDAPLookupUtils.createOrganizationContext(mLDAPAuth.getAuthObject(), mGlobalPath);
        // create root
        LDAPLookupUtils.createCristalContext(mLDAPAuth.getAuthObject(), mRootPath);
        // create local
        LDAPLookupUtils.createCristalContext(mLDAPAuth.getAuthObject(), mLocalPath);
    }

    @Override
    public void initializeDirectory(TransactionKey transactionKey) throws ObjectNotFoundException {
        createBootTree();
        LDAPLookupUtils.createCristalContext(mLDAPAuth.getAuthObject(), mItemTypeRoot);
        LDAPLookupUtils.createCristalContext(mLDAPAuth.getAuthObject(), mDomainTypeRoot);
        try {
            createRole(new RolePath(), transactionKey);
        }
        catch (ObjectAlreadyExistsException e) {}
        catch (ObjectCannotBeUpdated e) {
            log.error("Could not create root Role");
        }
        if (new DomainPath("agent").exists()) migrateOldRoles(transactionKey);
    }

    // typically search for cn=barcode
    @Override
    public LDAPPathSet search(Path start, String filter, SearchConstraints constraints, TransactionKey transactionKey) {
        log.debug("LDAPLookup::search() From " + getDN(start) + " for cn=" + filter);
        return search(getFullDN(start), "cn=" + LDAPLookupUtils.escapeSearchFilter(filter));
    }

    @Override
    public LDAPPathSet search(Path start, TransactionKey transactionKey, Property... props) {
        StringBuffer filter = new StringBuffer();
        int propCount = 0;
        for (Property prop : props) {
            filter.append("(|(cristalprop=" + LDAPLookupUtils.escapeSearchFilter(prop.getName() +
                    ":" + prop.getValue()) + ")(cristalprop=" + LDAPLookupUtils.escapeSearchFilter("!" + prop.getName() +
                            ":" + prop.getValue())
                    + "))");
            propCount++;
        }

        String filterParam;
        if (propCount == 0)
            filterParam = "";
        else if (propCount == 1)
            filterParam = filter.toString();
        else
            filterParam = "(&" + filter.toString() + ")";

        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_SEARCHING);
        return search(getFullDN(start), LDAPConnection.SCOPE_SUB, filterParam, searchCons);
    }

    @Override
    public LDAPPathSet search(Path start, PropertyDescriptionList props, TransactionKey transactionKey) {
        ArrayList<Property> params = new ArrayList<Property>();
        for (PropertyDescription propDesc : props.list) {
            if (propDesc.getIsClassIdentifier())
                params.add(propDesc.getProperty());
        }
        return search(start, transactionKey, params.toArray(new Property[params.size()]));
    }

    protected LDAPPathSet search(String startDN, int scope, String filter, LDAPSearchConstraints searchCons) {
        log.debug("Searching for " + filter + " in " + startDN);
        searchCons.setMaxResults(0);
        String[] attr = { LDAPConnection.ALL_USER_ATTRS };
        try {
            LDAPSearchResults res = mLDAPAuth.getAuthObject().search(startDN, scope,
                    filter, attr, false, searchCons);
            return new LDAPPathSet(res, this);
        }
        catch (LDAPException ex) {
            log.error("LDAPException::LDAPLookup::search() ", ex);
            return new LDAPPathSet(this);
        }
    }

    // typically search for (any filter combination)
    public LDAPPathSet search(String startDN, String filter) {
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        return search(startDN, LDAPConnection.SCOPE_SUB, filter, searchCons);
    }

    @Override
    public LDAPPathSet searchAliases(ItemPath entity, TransactionKey transactionKey) {
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        return search(getFullDN(new DomainPath()), LDAPConnection.SCOPE_SUB, "(&(objectClass=aliasObject)(aliasedObjectName=" +
                LDAPLookupUtils.escapeDN(getFullDN(entity)) + "))", searchCons);
    }

    @Override
    public boolean exists(Path path, TransactionKey transactionKey) {
        return LDAPLookupUtils.exists(mLDAPAuth.getAuthObject(), getFullDN(path));
    }

    @Override
    public ItemPath getItemPath(String uuid, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidItemPathException {
        String[] attr = { LDAPConnection.ALL_USER_ATTRS };
        try {
            ItemPath item = new ItemPath(uuid);
            LDAPEntry anEntry = mLDAPAuth.getAuthObject().read(getDN(item) + mLocalPath, attr);
            String type = LDAPLookupUtils.getFirstAttributeValue(anEntry, "objectClass");

            if (type.equals("cristalentity"))     return item;
            else if (type.equals("cristalagent")) return new AgentPath(item);
            else                                  throw new ObjectNotFoundException("Not an entity '" + uuid + "'");

        }
        catch (LDAPException ex) {
            if (ex.getResultCode() == LDAPException.NO_SUCH_OBJECT) throw new ObjectNotFoundException("Entity '" + uuid + "' does not exist");
            log.error("",ex);
            throw new ObjectNotFoundException("Error getting entity class for '" + uuid + "'");
        }
    }

    /**
     * converts an LDAPentry to a Path object Note that the search producing the entry should have retrieved
     * 'uniquemember' @throws ObjectNotFoundException @throws ObjectNotFoundException @throws
     */
    protected Path nodeToPath(LDAPEntry entry) throws InvalidItemPathException, ObjectNotFoundException {
        String dn = entry.getDN();
        ItemPath entityKey;

        // extract syskey
        try {
            String entityKeyStr = LDAPLookupUtils.getFirstAttributeValue(entry, "cn");
            entityKey = new ItemPath(entityKeyStr);
        }
        catch (ObjectNotFoundException ex) {
            entityKey = null;
        }
        catch (InvalidItemPathException ex) {
            entityKey = null;
        }

        /* Find the right path class */
        Path thisPath;
        if (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "cristalagent")) { // cristalagent
            String agentID = LDAPLookupUtils.getFirstAttributeValue(entry, "uid");
            thisPath = new AgentPath(entityKey, agentID);
        }
        else if (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "cristalrole")) { // cristalrole
            thisPath = new RolePath(getPathComponents(dn.substring(0, dn.lastIndexOf(mRoleTypeRoot))),
                    LDAPLookupUtils.getFirstAttributeValue(entry, "jobList").equals("TRUE"));
        }
        else if (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "aliasObject") ||
                (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "cristalcontext") && dn.endsWith(mDomainTypeRoot)))
        {
            DomainPath domainPath = new DomainPath();
            domainPath.setPath(getPathComponents(dn.substring(0, dn.lastIndexOf(mDomainTypeRoot))));
            thisPath = domainPath;
        }
        else if (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "cristalentity") ||
                (LDAPLookupUtils.existsAttributeValue(entry, "objectclass", "cristalcontext") && dn.endsWith(mItemTypeRoot)))
        {
            if (dn.endsWith(mItemTypeRoot)) {
                if (entityKey == null) throw new InvalidItemPathException(entry.getDN() + " was not a valid itemPath");
                thisPath = entityKey;
            }
            else
                throw new ObjectNotFoundException("Item found outside entity tree");
        }
        else {
            throw new ObjectNotFoundException("Unrecognised LDAP entry. Not a cristal entry '" + entry + "'");
        }

        return thisPath;
    }

    public String getDN(Path path) {
        StringBuffer dnBuffer = new StringBuffer();
        String[] pathComp = path.getPath();
        for (int i = pathComp.length - 1; i >= 0; i--)
            dnBuffer.append("cn=").append(LDAPLookupUtils.escapeDN(pathComp[i])).append(",");
        dnBuffer.append("cn=" + path.getRoot() + ",");
        return dnBuffer.toString();
    }

    public String getFullDN(Path path) {
        return getDN(path) + mLocalPath;
    }

    public String[] getPathComponents(String dnFragment) {
        ArrayList<String> newPath = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(dnFragment, ",");
        String[] path = new String[tok.countTokens()];
        while (tok.hasMoreTokens()) {
            String nextPath = tok.nextToken();
            if (nextPath.indexOf("cn=") == 0)
                newPath.add(0, LDAPLookupUtils.unescapeDN(nextPath.substring(3)));
            else
                break;
        }
        return newPath.toArray(path);
    }

    @Override
    public Iterator<Path> getChildren(Path path, TransactionKey transactionKey) {
        String filter = "objectclass=*";
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(10);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_FINDING);
        return search(getFullDN(path), LDAPConnection.SCOPE_ONE, filter, searchCons);
    }

    protected LDAPAttributeSet createAttributeSet(Path path) throws ObjectCannotBeUpdated {
        LDAPAttributeSet attrs = new LDAPAttributeSet();

        if (path instanceof RolePath) {
            RolePath rolePath = (RolePath) path;
            attrs.add(new LDAPAttribute("objectclass", "cristalrole"));
            String jobListString = rolePath.hasJobList() ? "TRUE" : "FALSE";
            attrs.add(new LDAPAttribute("jobList", jobListString));
            attrs.add(new LDAPAttribute("cn", rolePath.getName()));
        }
        else if (path instanceof DomainPath) {
            DomainPath domPath = (DomainPath) path;
            attrs.add(new LDAPAttribute("cn", domPath.getName()));
            try {
                attrs.add(new LDAPAttribute("aliasedObjectName", getFullDN(domPath.getItemPath())));
                String objectclass_values[] = { "alias", "aliasObject" };
                attrs.add(new LDAPAttribute("objectclass", objectclass_values));
            }
            catch (ObjectNotFoundException e) { // no entity - is a context
                attrs.add(new LDAPAttribute("objectclass", "cristalcontext"));
            }
        }
        else if (path instanceof ItemPath) {
            ItemPath itemPath = (ItemPath) path;
            attrs.add(new LDAPAttribute("cn", itemPath.getUUID().toString()));

            if (path instanceof AgentPath) {
                AgentPath agentPath = (AgentPath) path;
                attrs.add(new LDAPAttribute("objectclass", "cristalagent"));

                String agentName = agentPath.getAgentName();

                if (agentName != null && agentName.length() > 0)
                    attrs.add(new LDAPAttribute("uid", agentName));
                else
                    throw new ObjectCannotBeUpdated("Cannot create agent '" + agentName + "'. No userId specified");

                //password was removed from AgentPath because it was a vulnerability
                attrs.add(new LDAPAttribute("userPassword", "{sha}!"));
            }
            else {
                attrs.add(new LDAPAttribute("objectclass", "cristalentity"));
            }
        }

        return attrs;

    }

    // Creates a cristalRole
    // CristalRole is-a specialized CristalContext which contains multi-valued uniqueMember attribute pointing to cristalagents
    @Override
    public RolePath createRole(RolePath rolePath, TransactionKey transactionKey) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        // create the role
        String roleDN = getFullDN(rolePath);
        LDAPEntry roleNode;
        try {
            roleNode = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(rolePath));
            throw new ObjectAlreadyExistsException("Cannot create Role '" + rolePath.getName() + "' because it exists");
        }
        catch (ObjectNotFoundException ex) {}

        // create CristalRole if it does not exist
        roleNode = new LDAPEntry(roleDN, createAttributeSet(rolePath));
        try {
            LDAPLookupUtils.addEntry(mLDAPAuth.getAuthObject(), roleNode);
        }
        catch (LDAPException e) {
            throw new ObjectCannotBeUpdated("Cannot create Role '" + rolePath.getName() + "'- LDAPException:" + e.getLDAPErrorMessage());
        }
        return rolePath;

    }

    public void deleteRole(RolePath role) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        try {
            LDAPLookupUtils.delete(mLDAPAuth.getAuthObject(), getFullDN(role));
        }
        catch (LDAPException ex) {
            throw new ObjectCannotBeUpdated("Could not remove role '" + role.getName() + "'");
        }
    }

    @Override
    public void addRole(AgentPath agent, RolePath role, TransactionKey transactionKey)
            throws ObjectCannotBeUpdated, ObjectNotFoundException {
        LDAPEntry roleEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(role));
        // add memberDN to uniqueMember if it is not yet a member
        if (!LDAPLookupUtils.existsAttributeValue(roleEntry, "uniqueMember", getFullDN(agent)))
            LDAPLookupUtils.addAttributeValue(mLDAPAuth.getAuthObject(), roleEntry, "uniqueMember", getFullDN(agent));
        else
            throw new ObjectCannotBeUpdated("Agent " + agent.getAgentName() + " already has role " + role.getName());
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        LDAPEntry roleEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(role));
        if (LDAPLookupUtils.existsAttributeValue(roleEntry, "uniqueMember", getFullDN(agent)))
            LDAPLookupUtils.removeAttributeValue(mLDAPAuth.getAuthObject(), roleEntry, "uniqueMember", getFullDN(agent));
        else
            throw new ObjectCannotBeUpdated("Agent '" + agent.getAgentName() + "' did not have role '" + role.getName() + "'");
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role, TransactionKey transactionKey) {
        String filter = "(&(objectclass=cristalrole)(uniqueMember=" + getFullDN(agent) + ")(cn=" + role.getName() + "))";
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        return search(mRoleTypeRoot, LDAPConnection.SCOPE_SUB, filter, searchCons).hasNext();
    }

    @Override
    public AgentPath[] getAgents(RolePath role, TransactionKey transactionKey)
            throws ObjectNotFoundException {
        // get the roleDN entry, and its uniqueMember entry pointing to
        LDAPEntry roleEntry;
        try {
            roleEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(role));
        }
        catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Role '" + role.getName() + "' does not exist");
        }

        String[] res = LDAPLookupUtils.getAllAttributeValues(roleEntry, "uniqueMember");
        ArrayList<AgentPath> agents = new ArrayList<AgentPath>();
        for (String userDN : res) {
            try {
                LDAPEntry userEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), userDN);
                AgentPath path = (AgentPath) nodeToPath(userEntry);
                agents.add(path);
            }
            catch (ObjectNotFoundException ex) {
                log.error("Agent " + userDN + " does not exist");
            }
            catch (InvalidItemPathException ex) {
                log.error("Agent " + userDN + " is not a valid entity");
            }
        }
        AgentPath[] usersList = new AgentPath[0];
        usersList = agents.toArray(usersList);
        return usersList;
    }

    // returns the role/s of a user
    @Override
    public RolePath[] getRoles(AgentPath agentPath, TransactionKey transactionKey) {
        // search the mDomainPath tree uniqueMember=userDN
        // filter = objectclass=cristalrole AND uniqueMember=userDN
        String filter = "(&(objectclass=cristalrole)(uniqueMember=" + getFullDN(agentPath) + "))";
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        Iterator<?> roles = search(mRoleTypeRoot, LDAPConnection.SCOPE_SUB, filter, searchCons);
        ArrayList<RolePath> roleList = new ArrayList<RolePath>();

        while (roles.hasNext()) {
            RolePath path = (RolePath) roles.next();
            roleList.add(path);
        }
        RolePath[] roleArr = new RolePath[roleList.size()];
        roleArr = roleList.toArray(roleArr);
        return roleArr;
    }

    @Override
    public AgentPath getAgentPath(String agentName, TransactionKey transactionKey) throws ObjectNotFoundException {
        // search to get the userDN equivalent of the userID
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        String filter = "(&(objectclass=cristalagent)(uid=" + agentName + "))";
        Iterator<Path> res = search(mItemTypeRoot, LDAPConnection.SCOPE_SUB, filter, searchCons);
        if (!res.hasNext())
            throw new ObjectNotFoundException("Agent not found: " + agentName);
        Path result = res.next();
        if (result instanceof AgentPath)
            return (AgentPath) result;
        else
            throw new ObjectNotFoundException("Entry '" + agentName + "' was not an Agent");
    }

    @Override
    public RolePath getRolePath(String roleName, TransactionKey transactionKey) throws ObjectNotFoundException {
        // empty rolename gives the core role
        if (roleName.length() == 0) return new RolePath();

        if (roleName.contains("/")) { // absolute path
            RolePath absPath = new RolePath();
            absPath.setPath(roleName);
            if (absPath.exists()) {
                LDAPEntry entry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(absPath));
                try {
                    absPath.setHasJobList(LDAPLookupUtils.getFirstAttributeValue(entry, "jobList").equals("TRUE"));
                }
                catch (Exception e) {
                    log.error("",e);
                    throw new ObjectNotFoundException("Could not find role " + roleName);
                }
                return absPath;
            }
        }

        // else search for named role
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);
        String filter = "(&(objectclass=cristalrole)(cn=" + roleName + "))";
        Iterator<Path> res = search(mRoleTypeRoot, LDAPConnection.SCOPE_SUB, filter, searchCons);
        if (!res.hasNext())
            throw new ObjectNotFoundException("Role '" + roleName + "' not found");
        Path result = res.next();
        if (result instanceof RolePath)
            return (RolePath) result;
        else
            throw new ObjectNotFoundException("Entry '" + roleName + "' was not a Role");
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        // get entry
        LDAPEntry roleEntry;
        try {
            roleEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(role));
        }
        catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Role '" + role.getName() + "' does not exist");
        }
        // set attribute
        LDAPLookupUtils.setAttributeValue(mLDAPAuth.getAuthObject(), roleEntry, "jobList", hasJobList ? "TRUE" : "FALSE");
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword, boolean temporary, TransactionKey transactionKey)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException
    {
        if (temporary) log.warn("LDAPLookup.setAgentPassword() - Does NOT support temporary passords!");

        if (!newPassword.matches("^\\{[a-zA-Z0-5]*\\}")) newPassword = LDAPLookupUtils.generateUserPassword(newPassword);
        LDAPEntry agentEntry;
        try {
            agentEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(agent));
        }
        catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Agent " + agent.getAgentName() + " does not exist");
        }
        LDAPLookupUtils.setAttributeValue(mLDAPAuth.getAuthObject(), agentEntry, "userPassword", newPassword);
    }

    @Override
    public String getAgentName(AgentPath agentPath, TransactionKey transactionKey) throws ObjectNotFoundException {
        LDAPEntry agentEntry = LDAPLookupUtils.getEntry(mLDAPAuth.getAuthObject(), getFullDN(agentPath));
        return LDAPLookupUtils.getFirstAttributeValue(agentEntry, "uid");
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit, TransactionKey transactionKey) {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.getChildren() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, IteratorUtils.toList(getChildren(path, transactionKey)));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public PagedResult search(Path start, List<Property> props, int offset, int limit, TransactionKey transactionKey) {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.search() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, IteratorUtils.toList(search(start, props.toArray(new Property[0]))));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit, TransactionKey transactionKey) {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.search() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, IteratorUtils.toList(search(start, props)));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit, TransactionKey transactionKey) {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.searchAliases() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, IteratorUtils.toList(searchAliases(itemPath, transactionKey)));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public PagedResult getAgents(RolePath rolePath, int offset, int limit, TransactionKey transactionKey) throws ObjectNotFoundException {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.getAgents() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, Arrays.asList(getAgents(rolePath, transactionKey)));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public PagedResult getRoles(AgentPath agentPath, int offset, int limit, TransactionKey transactionKey) {
        if (ldapProps.mEnablePagingMethods) {
            log.warn("LDAPLookup.getRoles() - Paging support is not implemented, original method is used");
            return new PagedResult(-1, Arrays.asList(getRoles(agentPath, transactionKey)));
        }

        throw new NotImplementedException("Paging support is not implemented");
    }

    @Override
    public void setPermission(RolePath role, String permission, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        throw new NotImplementedException("Permission support is not implemented");
    }

    @Override
    public void setPermissions(RolePath role, List<String> permissions, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        throw new NotImplementedException("Permission support is not implemented");
    }

    @Override
    public List<DomainPath> getContextTree(DomainPath path, TransactionKey transactionKey) {
        throw new NotImplementedException("Retrieving ContextTree support is not implemented");
    }
}
