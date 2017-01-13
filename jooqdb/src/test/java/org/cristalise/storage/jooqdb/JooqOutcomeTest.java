package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;

public class JooqOutcomeTest {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();
    Outcome    outcome;

    JooqOutcome jooq;

    @Before
    public void before() throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);

        jooq = new JooqOutcome();
        jooq.createTables(context);

        outcome = new Outcome(1, "<xml/>", new Schema("SchemaName", 0, "<xs:schema/>"));
        assert jooq.put(context, uuid, outcome) == 1;
    }

    @Test
    public void fetchOutcome() throws Exception {
        Outcome outcomePrime = jooq.fetch(context, uuid, "SchemaName", 0, 1);
        assert outcomePrime != null;
        assert "<xml/>".equals(outcomePrime.getData());
    }

    @Test(expected=IllegalArgumentException.class)
    public void updateOutcome_exception() throws Exception {
        jooq.put(context, uuid, outcome);
    }
}
