package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.SystemKey
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.junit.Test


@CompileStatic
class LookupAddPathTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    SystemKey sysKey0 = new SystemKey(0,0)

    @Test
    public void addItemPath() {
        Path p = new ItemPath(uuid0.toString())
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)

        assert lookup.getItemPath(uuid0.toString())
    }

    @Test
    public void addAgentPath() {
        Path p = new AgentPath(new ItemPath(uuid0.toString()), "toto")
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)

        assert lookup.getAgentPath("toto")
    }

    @Test
    public void addDomainPath() {
        Path p = new DomainPath("empty/toto")
        assert p.string == "/domain/empty/toto"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.exists(new DomainPath("empty"))
        assert ! lookup.exists(new DomainPath("toto"))
    }

    @Test
    public void addRolePath() {
        Path p = new RolePath(new RolePath(), "User")
        assert p.string == "/domain/agent/User"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getRolePath("User")
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
