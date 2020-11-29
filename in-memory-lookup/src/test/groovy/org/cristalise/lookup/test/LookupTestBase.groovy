package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.apache.commons.lang.reflect.FieldUtils
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.cristalise.lookup.lite.InMemoryLookupManager
import org.junit.After
import org.junit.Before


@CompileStatic
class LookupTestBase {
    LookupManager lookup
    
    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);

        lookup = InMemoryLookupManager.instance

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup", lookup, true)

        lookup.open(null)
        lookup.initializeDirectory(null)
    }

    @After
    public void tearDown() {
        lookup.close()
        Logger.removeLogStream(System.out);
    }
}
