package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
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
    public void addDeleteItemPath() {
        Path p = new ItemPath(uuid0.toString())
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getItemPath(uuid0.toString()).getUUID() == uuid0
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteAgentPath() {
        Path p = new AgentPath(new ItemPath(uuid0.toString()), "toto")
        assert p.string == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getAgentPath("toto")
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteDomainPath() {
        Path p = new DomainPath("empty/toto")
        assert p.string == "/domain/empty/toto"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.exists(new DomainPath("empty"))
        assert ! lookup.exists(new DomainPath("toto"))
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteRolePath() {
        Path p = new RolePath(new RolePath(), "User")
        assert p.string == "/role/User"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getRolePath("User")
        lookup.delete(p)
        assert ! lookup.exists(p)
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
    
    @Test
    public void ObjectIsNotALeafException() {
        lookup.add(new DomainPath("empty/toto"))

        try {
            lookup.delete(new DomainPath("empty"))
            fail("Should throw ObjectCannotBeUpdated(Path 'domain/empty' is not a leaf)")
        }
        catch (ObjectCannotBeUpdated e) {
            assert e.message.contains("Path /domain/empty is not a leaf")
        }
    }
}
