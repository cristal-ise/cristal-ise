package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.apache.commons.lang.reflect.FieldUtils
import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.SystemKey
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.cristalise.lookup.lite.InMemoryLookupManager
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test


@CompileStatic
class LookupAddPathTests {

    UUID uuid0 = new UUID(0,0)
    SystemKey sysKey0 = new SystemKey(0,0)
    
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
    }

    @After
    public void tearDown() {
        lookup.close()
    }
    
    
    @Test
    public void addItemPath() {
        Path p = new ItemPath(uuid0.toString())
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
    }

    @Test
    public void addAgentPath() {
        Path p = new AgentPath(uuid0.toString())
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
    }

    @Test
    public void addDomainPath() {
        Path p = new DomainPath("emtpy/toto")
        assert p.string == "/domain/emtpy/toto"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.exists(new DomainPath("emtpy"))
        assert ! lookup.exists(new DomainPath("toto"))
    }

    @Test
    public void addRolePath() {
        Path p = new RolePath()
        assert p.string == "/domain/agent"
        lookup.add(p)
        assert lookup.exists(p)
    }
    
    @Test
    public void ObjectAlreadyExistsException() {
        Path p = new DomainPath("emtpy")
        lookup.add(p)
        assert lookup.exists(p)

        try {
            lookup.add(p)
            fail("second add() shall throw ObjectAlreadyExistsException")
        }
        catch (ObjectAlreadyExistsException e) {}
    }
}
