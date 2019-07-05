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
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.field;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.ClusterType;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Schema;
import org.jooq.Table;

public class JooqDataImport extends JooqHandler{
    
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importOutcomeData(DSLContext context,  File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          Table<?> OUTCOME_TABLE = schema.getTable("OUTCOME");
          final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
          final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
          final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
          final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);
          final Field<String>  XML             = field(name("XML"),            String.class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(OUTCOME_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, SCHEMA_NAME, SCHEMA_VERSION,EVENT_ID,XML)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importCollectionData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> COLLECTION_TABLE = schema.getTable("COLLECTION");
          final Field<UUID>   UUID    = field(name("UUID"),    UUID.class);
          final Field<String> NAME    = field(name("NAME"),    String.class);
          final Field<String> VERSION = field(name("VERSION"), String.class);
          final Field<String> XML     = field(name("XML"),     String.class);

          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(COLLECTION_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, NAME, VERSION,XML)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importPropertyData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> ITEM_PROPERTY_TABLE = schema.getTable("ITEM_PROPERTY");

          final Field<UUID>    UUID    = field(name("UUID"),    UUID.class);
          final Field<String>  NAME    = field(name("NAME"),    String.class);
          final Field<String>  VALUE   = field(name("VALUE"),   String.class);
          final Field<Boolean> MUTABLE = field(name("MUTABLE"), Boolean.class);

          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(ITEM_PROPERTY_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, NAME, VALUE, MUTABLE)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importJobData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> JOB_TABLE = schema.getTable("JOB");

          final Field<UUID>      UUID              = field(name("UUID"),              UUID.class);
          final Field<Integer>   ID                = field(name("ID"),                Integer.class);
          final Field<UUID>      DELEGATE_UUID     = field(name("DELEGATE_UUID"),     UUID.class);
          final Field<UUID>      ITEM_UUID         = field(name("ITEM_UUID"),         UUID.class);
          final Field<String>    STEP_NAME         = field(name("STEP_NAME"),         String.class);
          final Field<String>    STEP_PATH         = field(name("STEP_PATH"),         String.class);
          final Field<String>    STEP_TYPE         = field(name("STEP_TYPE"),         String.class);
          final Field<String>    TRANSITION        = field(name("TRANSITION"),        String.class);
          final Field<String>    ORIGIN_STATE_NAME = field(name("ORIGIN_STATE_NAME"), String.class);
          final Field<String>    TARGET_STATE_NAME = field(name("TARGET_STATE_NAME"), String.class);
          final Field<String>    AGENT_ROLE        = field(name("AGENT_ROLE"),        String.class);
          final Field<String>    ACT_PROPERTIES    = field(name("ACT_PROPERTIES"),    String.class);
          final Field<Timestamp> CREATION_TS       = field(name("CREATION_TS"),       Timestamp.class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(JOB_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, ID, DELEGATE_UUID, ITEM_UUID, STEP_NAME, STEP_PATH, STEP_TYPE, TRANSITION, 
                   ORIGIN_STATE_NAME, TARGET_STATE_NAME, AGENT_ROLE, ACT_PROPERTIES, CREATION_TS)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importLifeCycleData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> LIFECYCLE_TABLE = schema.getTable("LIFECYCLE");

          final Field<UUID>   UUID = field(name("UUID"), UUID.class);
          final Field<String> NAME = field(name("NAME"), String.class);
          final Field<String> XML  = field(name("XML"),  String.class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(LIFECYCLE_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, NAME, XML)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importAttachmentData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          Table<?> OUTCOME_ATTACHMENT_TABLE = schema.getTable("ATTACHMENT");
          final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
          final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
          final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
          final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);
          final Field<byte[]>  ATTACHMENT      = field(name("ATTACHMENT"),     byte[].class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(OUTCOME_ATTACHMENT_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, SCHEMA_NAME, SCHEMA_VERSION,EVENT_ID,ATTACHMENT)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importViewPointData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> VIEWPOINT_TABLE = schema.getTable("VIEWPOINT");

          final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
          final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
          final Field<String>  NAME            = field(name("NAME"),           String.class);
          final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
          final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(VIEWPOINT_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, SCHEMA_NAME, NAME,SCHEMA_VERSION,EVENT_ID)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importHistoryData(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> EVENT_TABLE = schema.getTable("EVENT");

          final Field<UUID>      UUID                  = field(name("UUID"),                 UUID.class);
          final Field<Integer>   ID                    = field(name("ID"),                   Integer.class);
          final Field<UUID>      AGENT_UUID            = field(name("AGENT_UUID"),           UUID.class);
          final Field<UUID>      DELEGATE_UUID         = field(name("DELEGATE_UUID"),        UUID.class);
          final Field<String>    AGENT_ROLE            = field(name("AGENT_ROLE"),           String.class);
          final Field<String>    SCHEMA_NAME           = field(name("SCHEMA_NAME"),          String.class);
          final Field<Integer>   SCHEMA_VERSION        = field(name("SCHEMA_VERSION"),       Integer.class);
          final Field<String>    STATEMACHINE_NAME     = field(name("STATEMACHINE_NAME"),    String.class);
          final Field<Integer>   STATEMACHINE_VERSION  = field(name("STATEMACHINE_VERSION"), Integer.class);
          final Field<String>    STEP_NAME             = field(name("STEP_NAME"),            String.class);
          final Field<String>    STEP_PATH             = field(name("STEP_PATH"),            String.class);
          final Field<String>    STEP_TYPE             = field(name("STEP_TYPE"),            String.class);
          final Field<Integer>   ORIGIN_STATE_ID       = field(name("ORIGIN_STATE_ID"),      Integer.class);
          final Field<Integer>   TARGET_STATE_ID       = field(name("TARGET_STATE_ID"),      Integer.class);
          final Field<Integer>   TRANSITION_ID         = field(name("TRANSITION_ID"),        Integer.class);
          final Field<String>    VIEW_NAME             = field(name("VIEW_NAME"),            String.class);
          final Field<Timestamp> TIMESTAMP             = field(name("TIMESTAMP"),            Timestamp.class);
          
          InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(EVENT_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, ID, AGENT_UUID,DELEGATE_UUID,AGENT_ROLE,SCHEMA_NAME,SCHEMA_VERSION,STATEMACHINE_NAME, STATEMACHINE_VERSION,
                   STEP_NAME,STEP_PATH,STEP_TYPE, ORIGIN_STATE_ID, TARGET_STATE_ID,TRANSITION_ID,VIEW_NAME,TIMESTAMP)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    public void importItem(DSLContext context, File file) throws IOException, ObjectNotFoundException {  
        
      Schema schema =  getPublicSchema(context);
      if(!Objects.isNull(schema)){
          final Table<?> ITEM_TABLE = schema.getTable("ITEM");

          final Field<UUID>      UUID                  = field(name("UUID"),                 UUID.class);
          final Field<String>    IOR                   = field(name("IOR"),                  String.class);
          final Field<Boolean>   IS_AGENT              = field(name("IS_AGENT"),             Boolean.class);
          final Field<String>    PASSWORD              = field(name("PASSWORD"),             String.class);
          final Field<Boolean>   IS_PASSWORD_TEMPORARY = field(name("IS_PASSWORD_TEMPORARY"),Boolean.class);
          
          
         InputStream inputStream = new FileInputStream(file);
          
         context.loadInto(ITEM_TABLE)
           .onDuplicateKeyUpdate()
           .commitAfter(50)
           .loadCSV(inputStream)
           .fields(UUID, IOR, IS_AGENT, IS_PASSWORD_TEMPORARY, PASSWORD)
           .execute(); 
      } else {
          throw new ObjectNotFoundException("No Schema found");
      }
    }
    
    public Schema getPublicSchema(DSLContext context){
        return context.meta().getSchemas().stream().filter( s -> s.getName().equals("public")).findFirst().get();
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
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        // TODO Auto-generated method stub
        return null;
    }
    

    
    
}

