package org.cristalise.devtest.testAgentUseConstructor.script

import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.MarkupBuilder
import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger(this.class)

def properties = [new Property('Type', 'TestAgentUseConstructor'), new Property('State', 'ACTIVE')]

def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 0)
TestAgentUseConstructorMap = [:]

for (DomainPath dp: result.rows) {
  TestAgentUseConstructorMap.put(dp.name, dp.itemPath.UUID)
}

return TestAgentUseConstructorMap
