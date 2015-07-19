package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.apache.commons.lang.reflect.FieldUtils
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.cristalise.lookup.lite.InMemoryLookupManager
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test


@CompileStatic
class LookupSearchTests {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)
    
    LookupManager lookup
    
    @BeforeClass
    public static void init() {
        Logger.addLogStream(new PrintStream(System.out), 8);
    }

    @Before
    public void setUp() throws Exception {
        lookup = new InMemoryLookupManager()
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup", lookup, true)
        lookup.open(null)
        lookup.initializeDirectory()

        lookup.add new ItemPath(uuid0.toString())
        lookup.add new AgentPath(uuid1.toString())
        lookup.add new DomainPath("empty/nothing")
        lookup.add new DomainPath("empty/something/uuid0", lookup.getItemPath(uuid0.toString()))
        lookup.add new RolePath()
    }

    @After
    public void tearDown() {
        lookup.close()
    }

    @Test
    public void search() {
        lookup.search(new DomainPath("empty"), "").each {
            println it.class
            assert it == new DomainPath("empty/nothing") 
            //new DomainPath("empty/something/uuid0")
        }
    }
}
