package org.cristalise.lookup.test;

import static org.junit.Assert.*

import org.apache.commons.lang3.reflect.FieldUtils
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.cristalise.kernel.utils.ObjectProperties
import org.cristalise.storage.jooqdb.JOOQClusterStorage
import org.cristalise.storage.jooqdb.JOOQLookupManager
import org.junit.After
import org.junit.Before
import org.springframework.cache.config.CacheAdviceParser.Props

import groovy.transform.CompileStatic


@CompileStatic
class LookupTestBase {

    LookupManager lookup

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);

        lookup = new JOOQLookupManager()

        ObjectProperties c2kProps = new ObjectProperties();

        c2kProps.put(JOOQClusterStorage.JOOQ_URI, "jdbc:h2:mem:");
        c2kProps.put(JOOQClusterStorage.JOOQ_USER, "sa");
        c2kProps.put(JOOQClusterStorage.JOOQ_PASSWORD, "sa");
        c2kProps.put(JOOQClusterStorage.JOOQ_DIALECT, "H2");

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup",        lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mC2KProps",      c2kProps, true)
        
        lookup.open(null)
        lookup.initializeDirectory()
    }

    @After
    public void tearDown() {
        lookup.close()
        Logger.removeLogStream(System.out);
    }
}
