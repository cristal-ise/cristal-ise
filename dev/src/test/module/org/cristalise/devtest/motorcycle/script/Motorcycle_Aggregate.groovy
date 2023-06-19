package org.cristalise.devtest.motorcycle.script

import org.cristalise.kernel.persistency.outcome.Outcome

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.MarkupBuilder
import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger(this.class)

def detailsSchema = 'Motorcycle_Details'

def name  = item.getName()
def state = item.getProperty('State')
def type  = item.getType()

Outcome details = null

if (item.checkViewpoint(detailsSchema, 'last')) {
    details = item.getOutcome(item.getViewpoint(detailsSchema, 'last'))
}

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml."$type" {
    Name(  name  )
    State( state )

    if (details) {
        details.getRecord().each {field, value ->
          if (field != 'Name') "$field"(value)
        }
    }
}

//check if this variable was defined as output
MotorcycleXML = writer.toString()
