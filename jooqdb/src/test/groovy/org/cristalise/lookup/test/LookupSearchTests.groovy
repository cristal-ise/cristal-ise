package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.RolePath
import org.junit.Before
import org.junit.Test


@CompileStatic
class LookupSearchTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)
    UUID uuid2 = new UUID(0,2)
    
    @Before
    public void setUp() throws Exception {
        super.setUp()

        lookup.add( new ItemPath(uuid0) )
        lookup.add( new AgentPath(uuid1, "Jim") )
        lookup.add( new AgentPath(uuid2, "John") )
        lookup.add( new DomainPath("empty/nothing") )
        lookup.add( new DomainPath("empty/something/uuid0", (ItemPath)lookup.getItemPath(uuid0.toString())) )
        lookup.add( new RolePath() )
    }

    @Test
    public void search() {
        def expected = [new DomainPath("empty"), 
                        new DomainPath("empty/nothing"), 
                        new DomainPath("empty/something"), 
                        new DomainPath("empty/something/uuid0")]

        CompareUtils.comparePathLists(expected, lookup.search(new DomainPath("empty"), ""))

        expected = [new DomainPath("empty/something/uuid0")]

        CompareUtils.comparePathLists(expected, lookup.search(new DomainPath("empty"), "uuid0"))
    }

    @Test
    public void getChildren() {
        CompareUtils.comparePathLists(
            [new DomainPath("empty/nothing"),  new DomainPath("empty/something")], 
            lookup.getChildren(new DomainPath("empty")))
    }
    
    @Test
    public void resolvePath() {
        assert lookup.resolvePath(new DomainPath("empty/something/uuid0"))
    }

    @Test
    public void getAgentName() {
        assert lookup.getAgentName(new AgentPath(uuid1)) == "Jim"
        assert lookup.getAgentName(new AgentPath(uuid2)) == "John"
    }
}
