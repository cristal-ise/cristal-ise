package org.cristalise.lookup.test;

import org.apache.commons.lang.reflect.FieldUtils
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.process.Gateway
import org.cristalise.lookup.lite.InMemoryLookupManager
import org.junit.After
import org.junit.Before

import groovy.transform.CompileStatic


@CompileStatic
class LookupTestBase {
    LookupManager lookup
    
    @Before
    public void setUp() throws Exception {
        lookup = InMemoryLookupManager.instance

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup", lookup, true)

        lookup.open(null)
        lookup.initializeDirectory(null)
    }

    @After
    public void tearDown() {
        lookup.close()
    }
}
