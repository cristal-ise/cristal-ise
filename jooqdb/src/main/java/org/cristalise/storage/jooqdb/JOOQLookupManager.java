/**
 * 
 */
package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;

/**
 *
 */
public class JOOQLookupManager implements LookupManager {

    public static final String JOOQ_URI      = "JOOQ.URI";
    public static final String JOOQ_USER     = "JOOQ.user";
    public static final String JOOQ_PASSWORD = "JOOQ.password";
    public static final String JOOQ_DIALECT  = "JOOQ.dialect";

    private DSLContext context;

    @Override
    public void open(Authenticator auth) {
        String uri  = Gateway.getProperties().getString(JOOQ_URI);
        String user = Gateway.getProperties().getString(JOOQ_USER); 
        String pwd  = Gateway.getProperties().getString(JOOQ_PASSWORD);

        if (StringUtils.isAnyBlank(uri, user, pwd)) {
            throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");
        }

        SQLDialect dialect = SQLDialect.valueOf(Gateway.getProperties().getString(JOOQ_DIALECT, "POSTGRES"));

        Logger.msg(1, "JOOQLookupManager.open() - uri:'"+uri+"' user:'"+user+"' dialect:'"+dialect+"'");

        try {
            context = using(DriverManager.getConnection(uri, user, pwd), dialect);

            //TODO: call create tables here
        }
        catch (SQLException | DataAccessException ex) {
            Logger.error("Could not connect to URI '" + uri + "' with user '" + user + "' and password '" + pwd + "'");
            Logger.error(ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            context.close();
        }
        catch (DataAccessException e) {
            Logger.error(e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getItemPath(java.lang.String)
     */
    @Override
    public ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#resolvePath(org.cristalise.kernel.lookup.DomainPath)
     */
    @Override
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getIOR(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public String getIOR(Path path) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#exists(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public boolean exists(Path path) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getChildren(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public Iterator<Path> getChildren(Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#search(org.cristalise.kernel.lookup.Path, java.lang.String)
     */
    @Override
    public Iterator<Path> search(Path start, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#search(org.cristalise.kernel.lookup.Path, org.cristalise.kernel.property.Property[])
     */
    @Override
    public Iterator<Path> search(Path start, Property... props) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#search(org.cristalise.kernel.lookup.Path, org.cristalise.kernel.property.PropertyDescriptionList)
     */
    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#searchAliases(org.cristalise.kernel.lookup.ItemPath)
     */
    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getAgentPath(java.lang.String)
     */
    @Override
    public AgentPath getAgentPath(String agentName) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getRolePath(java.lang.String)
     */
    @Override
    public RolePath getRolePath(String roleName) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getAgents(org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public AgentPath[] getAgents(RolePath rolePath) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getRoles(org.cristalise.kernel.lookup.AgentPath)
     */
    @Override
    public RolePath[] getRoles(AgentPath agentPath) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#hasRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public boolean hasRole(AgentPath agentPath, RolePath role) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getAgentName(org.cristalise.kernel.lookup.AgentPath)
     */
    @Override
    public String getAgentName(AgentPath agentPath) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#initializeDirectory()
     */
    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#add(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#delete(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#createRole(org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#addRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#removeRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#setAgentPassword(org.cristalise.kernel.lookup.AgentPath, java.lang.String)
     */
    @Override
    public void setAgentPassword(AgentPath agent, String newPassword)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#setHasJobList(org.cristalise.kernel.lookup.RolePath, boolean)
     */
    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub

    }

}
