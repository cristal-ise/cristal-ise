package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.TransactionalClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
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

    public static final String JOOQ_DOMAIN_HANDLER  = "JOOQ.domainHandler";

    private DSLContext context;

    HashMap<String, JooqHandler> jooqHandlers = new HashMap<String, JooqHandler>();
    JooqHandler domainHandler;

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

            initialiseHandlers();
        }
        catch (SQLException | DataAccessException ex) {
            Logger.error("JooqClusterStorage could not connect to URI '" + uri + "' with user '" + user + "'");
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    /**
     * 
     */
    public void initialiseHandlers() throws PersistencyException {
        try {
            if(Gateway.getProperties().containsKey(JOOQ_DOMAIN_HANDLER)) {
                domainHandler = (JooqHandler)Gateway.getProperties().getInstance(JOOQ_DOMAIN_HANDLER);
            }
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.error("JooqClusterStorage could not instantiate domain handler");
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }

        jooqHandlers.put(ClusterStorage.PROPERTY, new JooqItemPropertyHandler());
        jooqHandlers.get(ClusterStorage.PROPERTY).createTables(context);

        jooqHandlers.put(ClusterStorage.OUTCOME, new JooqOutcomeHandler());
        jooqHandlers.get(ClusterStorage.OUTCOME).createTables(context);


        jooqHandlers.put(ClusterStorage.VIEWPOINT, new JooqViewpointHandler());
        jooqHandlers.get(ClusterStorage.VIEWPOINT).createTables(context);

        jooqHandlers.put(ClusterStorage.LIFECYCLE, new JooqLifecycleHandler());
        jooqHandlers.get(ClusterStorage.LIFECYCLE).createTables(context);
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
        else if(VIEWPOINT.equals(clusterType))  return READWRITE;
        else if(LIFECYCLE.equals(clusterType))  return READWRITE;
        else if(HISTORY.equals(clusterType))    return NONE;
        else if(COLLECTION.equals(clusterType)) return NONE;
        else if(JOB.equals(clusterType))        return NONE;
        else                                    return NONE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        String lang = language.trim().toUpperCase();
        return "SQL".equals(lang) || ("SQL:"+context.dialect()).equals(lang);
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
        UUID uuid = itemPath.getUUID();
        String[] pathArray = path.split("/");

        String cluster = pathArray[0];
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length-1);

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage-getClusterContents() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));

            return jooqHandlers.get(cluster).getNextPrimaryKeys(context, uuid, primaryKeys);
        }
        else
            throw new PersistencyException("Read is not supported for '"+path+"'");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        UUID uuid = itemPath.getUUID();
        String[] pathArray = path.split("/");

        String cluster = pathArray[0];
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length-1);

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage-get() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));

            C2KLocalObject obj = jooqHandlers.get(cluster).fetch(context, uuid, primaryKeys);
            if (obj == null) throw new PersistencyException("JooqClusterStorage could not fetch '"+itemPath+"/"+path+"'");
            return obj;
        }
        else
            throw new PersistencyException("Read is not supported for '"+path+"'");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        put(itemPath, obj, null);
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();
        String cluster = obj.getClusterPath().split("/")[0];

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage-get() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());
            handler.put(context, uuid, obj);
        }
        else
            throw new PersistencyException("Write is not supported for '"+obj.getClusterPath()+"'");
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        delete(itemPath, path, null);
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();
        String[] pathArray = path.split("/");

        String cluster = pathArray[0];
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length-1);

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.delete() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            handler.delete(context, uuid, primaryKeys);
        }
        else
            throw new PersistencyException("Delete is not supported for '"+path+"'");
    }
}