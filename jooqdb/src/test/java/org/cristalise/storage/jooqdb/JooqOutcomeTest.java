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

import static org.cristalise.JooqTestConfigurationBase.DBModes.MYSQL;
import static org.cristalise.JooqTestConfigurationBase.DBModes.PostgreSQL;

import java.util.UUID;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.storage.jooqdb.clusterStore.JooqOutcomeHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqOutcomeTest extends StorageTestBase {

    Outcome outcome;
    JooqOutcomeHandler jooq;

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        jooq = new JooqOutcomeHandler();
        jooq.createTables(context);

        outcome = new Outcome(0, "<xml/>", new Schema("Schema", 0, "<xs:schema/>"));
        assert jooq.put(context, uuid, outcome) == 1;
    }

    @After
    public void after() throws Exception {
        jooq.delete(context, uuid);
        
        if (dbType == MYSQL || dbType == PostgreSQL) jooq.dropTables(context);
    }

    @Test
    public void fetchOutcome() throws Exception {
        Outcome outcomePrime = (Outcome)jooq.fetch(context, uuid, "Schema", "0", "0");
        assert outcomePrime != null;
        assert "<xml/>".equals(outcomePrime.getData());
    }

    @Test(expected=PersistencyException.class)
    public void updateOutcome_exception() throws Exception {
        jooq.put(context, uuid, outcome);
    }

    @Test
    public void getNextSchemaNames() throws Exception {
        assert jooq.put(context, uuid, new Outcome(1, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;
        assert jooq.put(context, uuid, new Outcome(2, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("Schema",  keys[0]);
        Assert.assertEquals("Schema2", keys[1]);
    }

    @Test
    public void delete() throws Exception {
        assert jooq.put(context, uuid, new Outcome(1, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;
        assert jooq.put(context, uuid, new Outcome(2, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;

        UUID uuid2 = UUID.randomUUID();

        assert jooq.put(context, uuid2, new Outcome(1, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;
        assert jooq.put(context, uuid2, new Outcome(2, "<xml/>", new Schema("Schema2", 0, "<xs:schema/>"))) == 1;

        assert jooq.delete(context, uuid) == 3;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("Schema2",  keys[0]);
    }
}
