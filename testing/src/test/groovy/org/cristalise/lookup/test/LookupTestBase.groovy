package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.apache.commons.lang.reflect.FieldUtils
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.process.Gateway
import org.cristalise.lookup.lite.InMemoryLookupManager
import org.cristalise.test.CristalTestSetup
import org.junit.After
import org.junit.Before


@CompileStatic
class LookupTestBase implements CristalTestSetup {
    LookupManager lookup
    
    @Before
    public void setUp() throws Exception {
        loggerSetup(9)

        lookup = new InMemoryLookupManager()

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup", lookup, true)

        lookup.open(null)
        lookup.initializeDirectory()
    }

    @After
    public void tearDown() {
        lookup.close()
    }
}
