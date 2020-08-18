import org.cristalise.kernel.persistency.outcome.Outcome

import groovy.xml.MarkupBuilder

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

def detailsSchema = 'TestItemUseConstructorGeneratedName_Details'

def name  = item.getName()
def state = item.getProperty('State')

def id = item.getProperty('ID')


Outcome details = null

if (item.checkViewpoint(detailsSchema, 'last')) {
    details = item.getOutcome(item.getViewpoint(detailsSchema, 'last'))
}

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml.TestItemUseConstructorGeneratedName {
    Name(  name  )
    State( state )

    ID(    id    )


    if (details) {
        details.getRecord().each {field, value ->
          "$field"(value)
        }
    }
}

//check if this variable was defined as output
TestItemUseConstructorGeneratedNameXML = writer.toString()
