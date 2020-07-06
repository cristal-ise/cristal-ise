import static org.apache.commons.lang3.StringUtils.leftPad
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION

import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.CreateAgentFromDescription
import org.cristalise.kernel.lifecycle.instance.predefined.agent.SetAgentPassword
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.property.PropertyArrayList
import org.cristalise.kernel.utils.LocalObjectLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.transform.Field

//--------------------------------------------------
// item, agent and job are injected by the CRISTAL-iSE Script class automatically
// so these declaration are only needed to write the script with code completion.
// COMMENT OUT before you run the module generators otherwise the script will fail with NPE
//--------------------------------------------------
//ItemProxy item
//AgentProxy agent
//Job job
//--------------------------------------------------

@Field final Logger log = LoggerFactory.getLogger('org.cristalise.dev.scripts.CrudFactory.InstantiateItem')

@Field String[] params      = null
@Field Class<?> predefStep  = null
@Field Outcome  outcome     = job.getOutcome()
@Field String   itemName    = getItemName(item, outcome)
@Field String   root        = getDomainRoot(item, job)
@Field Boolean  createAgent = new Boolean(item.getProperty('CreateAgent', 'false'))
@Field String   initaliseOutcomeXML = null

log.debug 'CrudFactory_NewInstanceDetails:{}', outcome.getData()

if (item.checkContent(ClusterType.COLLECTION.name, BuiltInCollections.SCHEMA_INITIALISE.name)) {
    initaliseOutcomeXML = getInitaliseOutcomeXML(item, outcome, itemName)
    log.debug 'initaliseOutcomeXML:{}', initaliseOutcomeXML
}

setParamsAndPredefStep(item, agent)
agent.execute(item, predefStep, params)

if (createAgent) {
    params = new String[1]

    params[0] = 'password'

    ItemProxy newAgent = agent.getItem(root + '/' + itemName)
    agent.execute(newAgent, SetAgentPassword.class, params)
}

@CompileStatic
String getInitaliseOutcomeXML(ItemProxy item, Outcome outcome, String itemName) {
    def initSchemaCollection = item.getCollection(BuiltInCollections.SCHEMA_INITIALISE)
    DependencyMember member = initSchemaCollection.getMembers().list[0]

    def updateSchemaUUID = member.getChildUUID()
    def updateSchemaVersion = member.getProperties().getBuiltInProperty(VERSION)
    if (updateSchemaVersion instanceof String) updateSchemaVersion = Integer.parseInt(updateSchemaVersion)

    def updateSchema = LocalObjectLoader.getSchema(updateSchemaUUID, (Integer)updateSchemaVersion).getName()

    outcome.setFieldByXPath("CrudFactory_NewInstanceDetails/SchemaInitialise/$updateSchema/Name", itemName)
    def initialiseNode = outcome.getNodeByXPath("/CrudFactory_NewInstanceDetails/SchemaInitialise/$updateSchema")

    if (initialiseNode) {
        return Outcome.serialize(initialiseNode, true)
    }
    else {
        throw new InvalidDataException("CrudFactory.InstantiateItem - invalid path:/CrudFactory_NewInstanceDetails/SchemaInitialise/$updateSchema")
    }
}

@CompileStatic
String getItemName(ItemProxy item, Outcome outcome) {
    String itemName = null

    if (item.checkProperty('IDPrefix')) {
        //Name is generated
        String  prefix  = item.getProperty('IDPrefix')
        Integer padSize = new Integer(item.getProperty('LeftPadSize', (String)null))

        log.debug 'getItemName() - generating name prefix:{}, padSize:{}', prefix, padSize

        if (!prefix)  throw new InvalidDataException("CrudFactory.InstantiateItem - Activity property IDPrefix must contain value")
        if (padSize == null) throw new InvalidDataException("CrudFactory.InstantiateItem - Activity property LeftPadSize must contain value")

        Integer lastCount = 0

        if (item.checkViewpoint('CrudFactory_NewInstanceDetails', 'last')) {
            def lastOutcome = item.getOutcome(item.getViewpoint('CrudFactory_NewInstanceDetails', 'last'))
            lastCount = new Integer(lastOutcome.getField("LastCount"))
        }

        lastCount++

        outcome.setFieldByXPath('CrudFactory_NewInstanceDetails/LastCount', lastCount.toString());

        itemName = prefix + leftPad((lastCount).toString(), padSize, "0")
    }
    else {
        //Name was provided by the user/agent
        itemName = outcome.getField('Name')

        if (!itemName || itemName == 'string' || itemName == 'null') {
            throw new InvalidDataException("CrudFactory.InstantiateItem - Name must be provided")
        }
    }

    outcome.setFieldByXPath('CrudFactory_NewInstanceDetails/Name', itemName);

    return itemName
}

@CompileStatic
String getDomainRoot(ItemProxy item, Job job) {
    String root = job.getActPropString('Root');
    if (!root) root = item.getProperty('Root')

    if (!root) throw new InvalidDataException("CrudFactory.InstantiateItem - Define property:'Root' for either Activity or for Item")

    return root
}

@CompileStatic
void setParamsAndPredefStep(ItemProxy item, AgentProxy agent) {
    if (createAgent) {
        predefStep = CreateAgentFromDescription.class

        params = new String[initaliseOutcomeXML ? 7 : 4]

        params[0] = itemName
        params[1] = root
        params[2] = item.getProperty('DefaultRoles')
        params[3] = 'password'
    }
    else {
        predefStep = CreateItemFromDescription.class

        params = new String[initaliseOutcomeXML ? 5 : 2]

        params[0] = itemName
        params[1] = root
    }

    if (initaliseOutcomeXML) {
        params[params.length-3] = 'last'
        params[params.length-2] = agent.marshall(new PropertyArrayList())
        params[params.length-1] = initaliseOutcomeXML
    }
}
