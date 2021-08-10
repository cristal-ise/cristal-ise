import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.utils.CastorHashMap

Outcome outcome = job.getOutcome()
def memberName = outcome.getField('MemberName')

def dep = new Dependency('Cars')
dep.addMember(agent.getItem('/devtest/Cars/'+memberName).getPath(), new CastorHashMap(), '', null);

outcome.appendXmlFragment('//AddMembersToCollection', agent.marshall(dep))
