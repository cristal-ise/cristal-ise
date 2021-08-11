import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.graph.model.BuiltInVertexProperties
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.utils.CastorHashMap

Outcome outcome = job.getOutcome()
def memberPath = outcome.getField('MemberPath')
def memberName = outcome.getField('MemberName')

if (!memberPath && !memberName) throw new InvalidDataException('Please provide MemberPath or MemberPath')

def depName = job.getActProp(BuiltInVertexProperties.DEPENDENCY_NAME)
def dep = new Dependency(depName)

if (memberPath) {
    dep.addMember(agent.getItem(memberPath).getPath(), new CastorHashMap(), '', null);
}
else {
    // find the item in the 'default' location eg. /integTest/Patients/kovax1
    def root = job.getActProp('Root')
    dep.addMember(agent.getItem("$root/$depName/$memberName").getPath(), new CastorHashMap(), '', null);
}

outcome.appendXmlFragment('//AddMembersToCollection', agent.marshall(dep))
