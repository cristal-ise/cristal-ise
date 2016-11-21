package org.cristalise.dev.test.scenario;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test



/**
 * 
 *
 */
@CompileStatic
class BasicDevLookupStructureIT extends KernelScenarioTestBase {

    @Test
    public void checkPath() {
        //Code to experiment with Lookup and LDAP based in dev module data

        println "------------------------------------------------------------------------"
        Gateway.getLookup().getChildren(new DomainPath("/")).each {
            println "Domain /: ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().getChildren(new RolePath()).each {
            println "Role /: ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().search(new DomainPath("desc"), "dev").each {
            println "Domain search : ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        Gateway.getLookup().searchAliases(new ItemPath("00000000-0000-0000-0000-00000000000c")).each {
            println "Item alias search: ${it.getClass().getName()} - $it"
        }

        println "------------------------------------------------------------------------"
        def path = ""
        ["desc","OutcomeDesc","system","dev"].each {
            path += "/$it"
            assert Gateway.getLookup().exists(new DomainPath(path))
//            assert Gateway.getLookup().resolvePath(new DomainPath(path))
        }

        assert Gateway.getLookup().getAgentPath("system")

        println "------------------------------------------------------------------------"
        Gateway.getLookup().search(new DomainPath("dev")).each {
            println it.dump()
        }
    }
}
