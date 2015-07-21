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
import org.junit.BeforeClass


@CompileStatic
class LookupTestBase {
    private static beforeClass = false

    LookupManager lookup
    
    @BeforeClass
    public static void init() {
        if(!beforeClass) {
            Logger.addLogStream(new PrintStream(System.out), 9);
            beforeClass = true
        }
    }

    @Before
    public void setUp() throws Exception {
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
