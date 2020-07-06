import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.Field

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

@Field final Logger log = LoggerFactory.getLogger('org.cristalise.dev.scripts.CrudEntity.ChangeName')

def outcome = job.getOutcome()
def currentName = item.getName()
def newName = outcome.getField('Name')

if (!newName) {
    throw new InvalidDataException('')
}
else if (newName != currentName) {
    def params = new String[2]

    params[0] = currentName
    params[1] = newName

    agent.execute(item, ChangeName.getClass(), params)
}
else
    log.warn 'New name is equal to the current one. Nothing done!'
