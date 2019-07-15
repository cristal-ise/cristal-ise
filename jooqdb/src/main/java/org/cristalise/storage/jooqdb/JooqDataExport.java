/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.name;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.utils.FileStringUtility;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Schema;
import org.jooq.Table;

public class JooqDataExport extends JooqHandler {

    static final String PUBLIC_SCHEMA = "public";

    static final String EVENT_TABLE      = "EVENT";
    static final String ITEM_TABLE       = "ITEM";
    static final String OUTCOME_TABLE    = "OUTCOME";
    static final String PROPERTY_TABLE   = "ITEM_PROPERTY";
    static final String ATTACHMENT_TABLE = "ATTACHMENT";
    static final String JOB_TABLE        = "JOB";
    static final String COLLECTION_TABLE = "COLLECTION";
    static final String VIEWPOINT_TABLE  = "VIEWPOINT";
    static final String LIFECYCLE_TABLE  = "LIFECYCLE";

    private static final String EXPORTED_DIR = "src/test/exportedData/"; // temp folder

    /**
     * 
     * @param context
     * @param item
     * @param tableName
     * @param index
     * @param last
     * @return
     * @throws InvalidItemPathException
     * @throws ObjectNotFoundException
     * @throws IOException
     */
    @SuppressWarnings({ "resource" })
    public void exportDataByLastSyncDate(DSLContext context, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws ObjectNotFoundException, IOException {
        Schema schema = getPublicSchema(context);
        if (!Objects.isNull(schema)) {
            exportDataByEvent(context, schema, lastSyncDate, syncDate);
            exportDataByItem(context, schema, lastSyncDate, syncDate);
            exportDataByProperty(context, schema, lastSyncDate, syncDate);
            exportDataByAttachment(context, schema, lastSyncDate, syncDate);
            exportDataByLifeCycle(context, schema, lastSyncDate, syncDate);
            exportDataByJob(context, schema, lastSyncDate, syncDate);
            exportDataByOutcome(context, schema, lastSyncDate, syncDate);
            exportDataByViewpoint(context, schema, lastSyncDate, syncDate);
            exportDataByCollection(context, schema, lastSyncDate, syncDate);

        }
        else {
            throw new ObjectNotFoundException("No Schema found");
        }
    }

    /**
     * 
     * @param context
     * @param schema
     * @param lastSyncDate
     * @throws IOException
     */
    public void exportDataByEvent(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        FileStringUtility.createNewDir(EXPORTED_DIR);
        FileWriter writer = new FileWriter(new File(EXPORTED_DIR + "/" + EVENT_TABLE + ".csv"));
        Table<?> EXPORT_TABLE = schema.getTable(EVENT_TABLE).as("E");
        Field<LocalDateTime> TIMESTAMP = EXPORT_TABLE.field(name("TIMESTAMP"), LocalDateTime.class);
        context.selectFrom(EXPORT_TABLE)
            .where(TIMESTAMP.between(lastSyncDate, syncDate))
            .fetchLazy()   
            .formatCSV(writer);
    }

    public void exportDataByItem(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate) throws IOException {
        exportDataProcess(context, schema, lastSyncDate, syncDate, ITEM_TABLE);
    }

    public void exportDataByProperty(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcess(context, schema, lastSyncDate, syncDate, PROPERTY_TABLE);
    }

    public void exportDataByLifeCycle(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcess(context, schema, lastSyncDate, syncDate, LIFECYCLE_TABLE);
    }

    public void exportDataByJob(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate) throws IOException {
        exportDataProcess(context, schema, lastSyncDate, syncDate, JOB_TABLE);
    }

    public void exportDataByCollection(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcess(context, schema, lastSyncDate, syncDate, COLLECTION_TABLE);
    }
    
    
    
    public void exportDataByOutcome(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcessWithJoin(context, schema, lastSyncDate, syncDate, OUTCOME_TABLE);
    }

    public void exportDataByViewpoint(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcessWithJoin(context, schema, lastSyncDate, syncDate, VIEWPOINT_TABLE);
       
    }
    public void exportDataByAttachment(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate)
            throws IOException {
        exportDataProcessWithJoin(context, schema, lastSyncDate, syncDate, ATTACHMENT_TABLE);
        
    }
    
    
    public void exportDataProcessWithJoin(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate, String tableName) throws IOException{
        FileStringUtility.createNewDir(EXPORTED_DIR);
        FileWriter writer = new FileWriter(new File(EXPORTED_DIR + "/" + tableName + ".csv"));

        Table<?> EXPORT_TABLE = schema.getTable(tableName);
        Field<UUID> EXPORT_UUID = EXPORT_TABLE.field(name("UUID"), UUID.class);
        Field<UUID> EXPORT_EVENTID = EXPORT_TABLE.field(name("EVENT_ID"), UUID.class);

        Table<?> EVENT = schema.getTable(EVENT_TABLE);
        Field<LocalDateTime> TIMESTAMP = EVENT.field(name("TIMESTAMP"), LocalDateTime.class);
        Field<UUID> EVENT_UUID = EVENT.field(name("UUID"), UUID.class);
        Field<UUID> EVENT_ID = EVENT.field(name("ID"), UUID.class);

        context.select().from(EXPORT_TABLE).join(EVENT).on(EXPORT_UUID.equal(EVENT_UUID),
                EXPORT_EVENTID.equal(EVENT_ID)).where(TIMESTAMP.between(lastSyncDate, syncDate)).fetchLazy().formatCSV(writer);
    }

    /**
     * 
     * @param context
     * @param tableName
     * @return
     * @throws ObjectNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public void exportData(DSLContext context, String tableName, File file) throws ObjectNotFoundException, IOException {
        FileWriter writer = new FileWriter(file);
        Schema schema = getPublicSchema(context);
        if (!Objects.isNull(schema)) {
            final Table<?> EXPORT_TABLE = schema.getTable(tableName.toUpperCase());
            context.selectFrom(EXPORT_TABLE).fetchLazy().formatCSV(writer);
        }
        else {
            throw new ObjectNotFoundException("No Schema found");
        }

    }

    /**
     * 
     * @param context
     * @param schema
     * @param lastSyncDate
     * @param tableName
     * @throws IOException
     */
    private void exportDataProcess(DSLContext context, Schema schema, LocalDateTime lastSyncDate, LocalDateTime syncDate, String tableName)
            throws IOException {
        FileWriter writer = new FileWriter(new File(EXPORTED_DIR + "/" + tableName + ".csv"));

        Table<?> EXPORT_TABLE = schema.getTable(tableName);
        Field<UUID> EXPORT_UUID = EXPORT_TABLE.field(name("UUID"), UUID.class);

        Table<?> EVENT = schema.getTable(EVENT_TABLE);
        Field<LocalDateTime> TIMESTAMP = EVENT.field(name("TIMESTAMP"), LocalDateTime.class);
        Field<UUID> EVENT_UUID = EVENT.field(name("UUID"), UUID.class);

        context.select().from(EXPORT_TABLE)
                .where(EXPORT_UUID.in(context.selectDistinct(EVENT_UUID).from(EVENT).where(TIMESTAMP.between(lastSyncDate, syncDate))))
                .fetchLazy().formatCSV(writer);
    }

    public Schema getPublicSchema(DSLContext context) {
        return context.meta().getSchemas().stream().filter(s -> s.getName().equals(PUBLIC_SCHEMA)).findFirst().get();
    }

    @Override
    protected Table<?> getTable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        // TODO Auto-generated method stub

    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        // TODO Auto-generated method stub

    }

    @Override
    public int update(DSLContext context, java.util.UUID uuid, C2KLocalObject obj) throws PersistencyException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int insert(DSLContext context, java.util.UUID uuid, C2KLocalObject obj) throws PersistencyException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, java.util.UUID uuid, String... primaryKeys) throws PersistencyException {
        // TODO Auto-generated method stub
        return null;
    }

}
