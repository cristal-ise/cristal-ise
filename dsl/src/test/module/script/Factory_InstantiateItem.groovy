import static org.apache.commons.lang3.StringUtils.leftPad
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION

import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.ObjectAlreadyExistsException
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

final Logger log = LoggerFactory.getLogger("org.cristalise.dsl.test.scripts.Factory.InstantiateItem")

//--------------------------------------------------
// item, agent and job are injected by the CRISTAL-iSE Script class automatically
// so these declaration are only needed to write the script with code completion.
// COMMENT OUT before you run the module generators otherwise the script will fail with NPE
//--------------------------------------------------
//ItemProxy item
//AgentProxy agent
//Job job
//--------------------------------------------------

Outcome outcome = job.getOutcome()

log.debug 'Factory_NewInstanceDetails:{}', outcome.getData()

String   itemName    = getItemName(item, outcome)
String   root        = item.getProperty('Root')
Boolean  createAgent = new Boolean(item.getProperty('CreateAgent', 'false'))

String   initaliseOutcomeXML = null

if (item.checkContent(ClusterType.COLLECTION.name, BuiltInCollections.SCHEMA_INITIALISE.name)) {
    initaliseOutcomeXML = getInitaliseOutcomeXML(item, outcome, itemName)
    log.debug 'initaliseOutcomeXML:{}', initaliseOutcomeXML
}

String[] params = null
Class<?> predefStep = null

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

try {
    agent.execute(item, predefStep, params)
}
catch (ObjectAlreadyExistsException ex) {
    throw new ObjectAlreadyExistsException("[errorMessage]Item already exists: $itemName[/errorMessage]")
}

if (createAgent) {
    params = new String[1]

    params[0] = 'password'

    ItemProxy employee = agent.getItem(root + '/' + itemName)
    agent.execute(employee, SetAgentPassword, params)
}

@CompileStatic
String getInitaliseOutcomeXML(ItemProxy item, Outcome outcome, String itemName) {
    def initSchemaCollection = item.getCollection(BuiltInCollections.SCHEMA_INITIALISE)
    DependencyMember member = initSchemaCollection.getMembers().list[0]

    def updateSchemaUUID = member.getChildUUID()
    def updateSchemaVersion = member.getProperties().getBuiltInProperty(VERSION)
    if (updateSchemaVersion instanceof String) updateSchemaVersion = Integer.parseInt(updateSchemaVersion)

    def updateSchema = LocalObjectLoader.getSchema(updateSchemaUUID, (Integer)updateSchemaVersion).getName()

    outcome.setFieldByXPath("Factory_NewInstanceDetails/SchemaInitialise/$updateSchema/Name", itemName)
    def initialiseNode = outcome.getNodeByXPath("/Factory_NewInstanceDetails/SchemaInitialise/$updateSchema")

    if (initialiseNode) {
        return Outcome.serialize(initialiseNode, true)
    }
    else {
        throw new InvalidDataException("Script.Factory_InstantiateItem - invalid path:/Factory_NewInstanceDetails/SchemaInitialise/$updateSchema")
    }
}

@CompileStatic
String getItemName(ItemProxy item, Outcome outcome) {
    String itemName = null

    if (item.checkProperty('IDPrefix')) {
        //Name is generated
        String prefix  = item.getProperty('IDPrefix')
        int    padSize = new Integer(item.getProperty('LeftPadSize'))

        if (!prefix)  throw new InvalidDataException("Script.InstantiateItem - Activity property IDPrefix must contain value")
        if (!padSize) throw new InvalidDataException("Script.InstantiateItem - Activity property LeftPadSize must contain value")

        Integer lastCount = 0

        if (item.checkViewpoint('Factory_NewInstanceDetails', 'last')) {
            def lastOutcome = item.getOutcome(item.getViewpoint('Factory_NewInstanceDetails', 'last'))
            lastCount = new Integer(lastOutcome.getField("LastCount"))
        }

        lastCount++

        outcome.setFieldByXPath('Factory_NewInstanceDetails/LastCount', lastCount.toString());

        itemName = prefix + leftPad((lastCount).toString(), padSize, "0")
    }
    else {
        //Name was provided by the user/agent
        itemName = outcome.getField('Name')

        if (!itemName || itemName == 'string' || itemName == 'null') {
            throw new InvalidDataException("Script.InstantiateItem - Name must be provided")
        }
    }

    outcome.setFieldByXPath('Factory_NewInstanceDetails/Name', itemName);

    return itemName
}
