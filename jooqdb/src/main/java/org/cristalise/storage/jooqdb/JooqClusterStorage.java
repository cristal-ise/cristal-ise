package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.TransactionalClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DefaultConnectionProvider;

public class JooqClusterStorage extends TransactionalClusterStorage {
    public static final String JOOQ_URI      = "JOOQ.URI";
    public static final String JOOQ_USER     = "JOOQ.user";
    public static final String JOOQ_PASSWORD = "JOOQ.password";
    public static final String JOOQ_DIALECT  = "JOOQ.dialect";

    private DSLContext context;

    JooqItemProperty propertyHandler;
    JooqOutcome      outcomeHandler;

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        String uri  = Gateway.getProperties().getString(JOOQ_URI);
        String user = Gateway.getProperties().getString(JOOQ_USER); 
        String pwd  = Gateway.getProperties().getString(JOOQ_PASSWORD);

        if (StringUtils.isAnyBlank(uri, user, pwd)) {
            throw new PersistencyException("JOOQ (uri, user, password) config values must not be blank");
        }

        SQLDialect dialect = SQLDialect.valueOf(Gateway.getProperties().getString(JOOQ_DIALECT, "POSTGRES"));

        Logger.msg(1, "JOOQClusterStorage.open() - uri:'"+uri+"' user:'"+user+"' dialect:'"+dialect+"'");

        try {
            context = using(DriverManager.getConnection(uri, user, pwd), dialect);

            propertyHandler = new JooqItemProperty();
            propertyHandler.createTables(context);

            outcomeHandler = new JooqOutcome();
            outcomeHandler.createTables(context);
        }
        catch (SQLException | DataAccessException ex) {
            Logger.error("Could not connect to URI '" + uri + "' with user '" + user + "' and password '" + pwd + "'");
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    @Override
    public void close() throws PersistencyException {
        Logger.msg(1, "JOOQClusterStorage.close()");
        try {
            context.close();
        }
        catch (DataAccessException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void begin(Object locker) {
        Logger.msg(1, "JOOQClusterStorage.begin()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).setAutoCommit(false);
        } 
        catch (DataAccessException e) {
            Logger.error(e);
        }
    }

    @Override
    public void commit(Object locker) throws PersistencyException {
        Logger.msg(1, "JOOQClusterStorage.commit()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).commit();
        } 
        catch (DataAccessException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void abort(Object locker) {
        Logger.msg(1, "JOOQClusterStorage.abort()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).rollback();
        } 
        catch (DataAccessException e) {
            Logger.error(e);
        }
    }

    @Override
    public short queryClusterSupport(String clusterType) {
        if(     PROPERTY.equals(clusterType))   return READWRITE;
        else if(OUTCOME.equals(clusterType))    return READWRITE;
        else if(VIEWPOINT.equals(clusterType))  return NONE;
        else if(LIFECYCLE.equals(clusterType))  return NONE;
        else if(HISTORY.equals(clusterType))    return NONE;
        else if(COLLECTION.equals(clusterType)) return NONE;
        else if(JOB.equals(clusterType))        return NONE;
        else                                    return NONE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        return false;
        //return "mysql:sql".equals(language.trim().toLowerCase());
    }

    @Override
    public String getName() {
        return "JOOQ:"+context.dialect()+" ClusterStorage";
    }

    @Override
    public String getId() {
        return "JOOQ:"+context.dialect();
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UnImplemented");
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        throw new PersistencyException("Read is not supported");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        UUID uuid = itemPath.getUUID();
        String[] pathArray = path.split("/");

        if (path.startsWith(ClusterStorage.PROPERTY)) {
            Property p = propertyHandler.fetch(context, uuid, pathArray[1]);
            if (p == null) throw new PersistencyException("Could not fetch '"+itemPath+"/"+path+"'");
            return p;
        }
        else if (path.startsWith(ClusterStorage.OUTCOME)) {
            Outcome outcome = outcomeHandler.fetch(context, uuid, pathArray[1], new Integer(pathArray[2]), new Integer(pathArray[3]));

            if (outcome == null) throw new PersistencyException("Could not fetch '"+itemPath+"/"+path+"'");

            try {
                outcome.setSchema(LocalObjectLoader.getSchema(pathArray[1], new Integer(pathArray[2])));
            }
            catch (NumberFormatException | ObjectNotFoundException | InvalidDataException e) {
                Logger.error(e);
                throw new PersistencyException(e.getMessage());
            }
            return outcome;
        }
        throw new PersistencyException("Read is not supported for '"+path+"'");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        put(itemPath, obj, null);
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        if (obj instanceof Property) {
            propertyHandler.put(context, itemPath.getUUID(), (Property)obj);
        }
        else if (obj instanceof Outcome) {
            try {
                outcomeHandler.put(context, uuid, (Outcome) obj);
            }
            catch (Exception e) {
                Logger.error(e);
                throw new PersistencyException(itemPath + " could not be persisted" + e.getMessage());
            }
        }
        else{
            throw new PersistencyException("Unimplemented feature to store '"+obj.getClusterPath()+"'");
        }
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        throw new PersistencyException("Delete is not supported");
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        throw new PersistencyException("Delete is not supported");
    }
}