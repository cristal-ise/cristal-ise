package org.cristalise.storage.jooqdb;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
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

        outcome = new Outcome(1, "<xml/>", new Schema("Schema", 0, "<xs:schema/>"));
        assert jooq.put(context, uuid, outcome) == 1;
    }

    @Test
    public void fetchOutcome() throws Exception {
        Outcome outcomePrime = (Outcome)jooq.fetch(context, uuid, "Schema", "0", "1");
        assert outcomePrime != null;
        assert "<xml/>".equals(outcomePrime.getData());
    }

    @Test(expected=IllegalArgumentException.class)
    public void updateOutcome_exception() throws Exception {
        jooq.put(context, uuid, outcome);
    }
}
