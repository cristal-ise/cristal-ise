package org.cristalise.storage.jooqdb;

import java.util.UUID;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqOutcomeTest extends JooqTestBase {

    Outcome outcome;
    JooqOutcomeHandler jooq;

    @Before
    public void before() throws Exception {
        super.before();

        jooq = new JooqOutcomeHandler();
        jooq.createTables(context);

        outcome = new Outcome(0, "<xml/>", new Schema("Schema", 0, "<xs:schema/>"));
        assert jooq.put(context, uuid, outcome) == 1;
    }

    @Test
    public void fetchOutcome() throws Exception {
        Outcome outcomePrime = (Outcome)jooq.fetch(context, uuid, "Schema", "0", "0");
        assert outcomePrime != null;
        assert "<xml/>".equals(outcomePrime.getData());
    }

    @Test(expected=IllegalArgumentException.class)
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
