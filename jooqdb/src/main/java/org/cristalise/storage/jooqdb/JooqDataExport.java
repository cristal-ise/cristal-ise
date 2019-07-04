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

import static org.jooq.impl.DSL.field;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.SelectWhereStep;
import org.jooq.Table;


public class JooqDataExport extends JooqHandler{
    
    public static final String PUBLIC_SCHEMA        = "public";
    static final Field<UUID>  UUID                  = field("UUID", UUID.class);
    static final Field<Integer> EVENT_ID            = field("EVENT_ID", Integer.class);
    
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
     */
    @SuppressWarnings("unchecked")
    public String exportDataByItem (DSLContext context, ItemPath item, String tableName, int index, int last) throws ObjectNotFoundException{
        Schema schema =  getPublicSchema(context);
        String csvData = "";
        
        if (!Objects.isNull(item)){
            throw new ObjectNotFoundException("No Item found");
        }
        
        UUID itemUuid = item.getUUID();
        
        // TO DO: fetch data for specific item
        return csvData;
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
    public void exportData(DSLContext context, String tableName, File file) throws ObjectNotFoundException, IOException{
        FileWriter writer = new FileWriter(file);
        Schema schema =  getPublicSchema(context);
        String csvData = "";
        if(!Objects.isNull(schema)){
            final Table<?> EXPORT_TABLE = schema.getTable(tableName.toUpperCase());
            context.selectFrom(EXPORT_TABLE).fetchLazy().formatCSV(writer);
        } else {
            throw new ObjectNotFoundException("No Schema found");
        } 
        
    }
    
    public Schema getPublicSchema(DSLContext context){
        return context.meta().getSchemas().stream().filter( s -> s.getName().equals(PUBLIC_SCHEMA)).findFirst().get();
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

