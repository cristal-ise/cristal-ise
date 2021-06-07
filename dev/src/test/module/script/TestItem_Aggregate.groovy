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

def detailsSchema = 'TestItem_Details'

def name  = item.getName()
def state = item.getProperty('State')


Outcome details = null

if (item.checkViewpoint(detailsSchema, 'last')) {
    details = item.getOutcome(item.getViewpoint(detailsSchema, 'last'))
}

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml.TestItem {
    Name(  name  )
    State( state )


    if (details) {
        details.getRecord().each {field, value ->
          if (field != 'Name') "$field"(value)
        }
    }
}

//check if this variable was defined as output
TestItemXML = writer.toString()
