import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property

//--------------------------------------------------
// item, agent and job are injected by the Script class
// automatically so these declaration are only needed
// to write the script with code completion.
// COMMENT OUT before you run the module generators
//--------------------------------------------------
// ItemProxy item
// AgentProxy agent
// Job job
//--------------------------------------------------

def properties = [new Property('Type', 'TestItem'), new Property('State', 'ACTIVE')]

def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 100)
TestItemMap = [];

for (DomainPath dp: result.rows) {
    TestItemMap.put(dp.name, dp.itemPath.UUID)
}

return TestItemMap
