package org.cristalise.storage.jooqdb;

import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

public interface JooqHandler {

    public DataType<UUID>    UUID_TYPE    = SQLDataType.UUID;
    public DataType<String>  NAME_TYPE    = SQLDataType.VARCHAR.length(64);
    public DataType<String>  VERSION_TYPE = SQLDataType.VARCHAR.length(64);
    public DataType<String>  STRING_TYPE  = SQLDataType.VARCHAR.length(4096);
    public DataType<Integer> EVENTID_TYPE = SQLDataType.INTEGER;
    public DataType<String>  XML_TYPE     = SQLDataType.CLOB;

    public static String[] pathToPrimaryKeys(String path) {
        String[] pathArray = path.split("/");
        return Arrays.copyOfRange(pathArray, 1, pathArray.length-1);
    }

    /**
     * 
     * @param context
     */
    public void createTables(DSLContext context);

    /**
     * 
     * @param context
     * @param primaryKeys
     * @return
     */
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys);

    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj);
    
    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj);

    /**
     * 
     * @param context
     * @param uuid
     * @param primaryKeys
     */
    public int delete(DSLContext context, UUID uuid, String...primaryKeys);

    /**
     * 
     * @param context
     * @param uuid
     * @param obj
     * @return
     */
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj);

    /**
     * 
     * @param context
     * @param uuid
     * @param id
     * @return
     */
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys);
}
