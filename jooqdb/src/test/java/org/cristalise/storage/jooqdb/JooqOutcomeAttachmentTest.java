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

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.storage.jooqdb.clusterStore.JooqOutcomeAttachmentHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqOutcomeAttachmentTest extends StorageTestBase {

    OutcomeAttachment            outcome;
    JooqOutcomeAttachmentHandler jooqHandler;
    ItemPath                     item;
    Schema                       schema;
    byte[]                       binaryData;

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        item = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");

        schema = new Schema("Schema", 0, item, "Attachment");

        binaryData = item.getName().getBytes();

        jooqHandler = new JooqOutcomeAttachmentHandler();
        jooqHandler.createTables(context);
       
        outcome = new OutcomeAttachment(item, schema.getName(), schema.getVersion(), 0, null, binaryData);
        assert jooqHandler.put(context, uuid, outcome) == 1;
    }

    @Test
    public void fetchOutcome() throws Exception {
        OutcomeAttachment outcomeAttachment = (OutcomeAttachment) jooqHandler.fetch(context, uuid, "Schema", "0");
        assert outcomeAttachment != null;
        assert Arrays.equals(item.getName().getBytes(), outcomeAttachment.getBinaryData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateOutcome_exception() throws Exception {
        jooqHandler.put(context, uuid, outcome);
    }

    @Test
    public void getNextSchemaNames() throws Exception {
      
        item = new ItemPath("fcecd22-40eb-421c-a648-edc1d74f339b");
        schema = new Schema("Schema2", 0, item, "Attachment");
        binaryData = item.getName().getBytes();
        
        outcome = new OutcomeAttachment(item, schema.getName(), schema.getVersion(), 0, null, binaryData);
        assert  jooqHandler.put(context, uuid, outcome) == 1;
        String[] keys = jooqHandler.getNextPrimaryKeys(context, uuid);

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("Schema", keys[0]);
        Assert.assertEquals("Schema2", keys[1]);
    }
    
    
    @Test
    public void delete() throws Exception {
       
        item = new ItemPath("fcecd22-40eb-421c-a648-edc1d74f339b");
        schema = new Schema("Schema2", 0, item, "Attachment");
        binaryData = item.getName().getBytes();
        
        outcome = new OutcomeAttachment(item, schema.getName(), schema.getVersion(), 0, null, binaryData);
        assert  jooqHandler.put(context, uuid, outcome) == 1;
        
        item = new ItemPath("adecd02-40eb-421c-a648-edc1d74f449b");
        schema = new Schema("Schema2", 0, item, "Attachment");
        binaryData = item.getName().getBytes();
        
        UUID uuid2 = UUID.randomUUID();
        
        outcome = new OutcomeAttachment(item, schema.getName(), schema.getVersion(), 0, null, binaryData);
        assert  jooqHandler.put(context, uuid2, outcome) == 1;


        assert jooqHandler.delete(context, uuid) == 2;

        String[] keys = jooqHandler.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("Schema2",  keys[0]);
    }

}
