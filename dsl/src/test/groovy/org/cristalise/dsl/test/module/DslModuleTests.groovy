package org.cristalise.dsl.test.module

import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class DslModuleTests implements CristalTestSetup {

    
    @Before
    public void setup() {
        def props = new Properties()
        props.put('Resource.moduleUseFileNameWithVersion', 'dsl')
        inMemoryServer(-1, props)
    }

    @After
    public void cleanup() {
        cristalCleanup()
    }

    @Test
    public void checkItemsDslTestModule() {
        def dslUser = Gateway.getProxyManager().getAgentProxy('dsl')
        dslUser.getItem('/desc/AgentDesc/dsl/dsl')

        dslUser.getItem('/desc/ItemDesc/dsl/TestAgentFactory')
        dslUser.getItem('/desc/ItemDesc/dsl/TestAgentUseConstructorFactory')
        dslUser.getItem('/desc/ItemDesc/dsl/TestItemFactory')
        dslUser.getItem('/desc/ItemDesc/dsl/TestItemGeneratedNameFactory')
        dslUser.getItem('/desc/ItemDesc/dsl/TestItemUseConstructorFactory')
        dslUser.getItem('/desc/ItemDesc/dsl/TestItemUseConstructorGeneratedNameFactory')
        dslUser.getItem('/dsl/TestAgentFactory')
        dslUser.getItem('/dsl/TestAgentUseConstructorFactory')
        dslUser.getItem('/dsl/TestItemFactory')
        dslUser.getItem('/dsl/TestItemGeneratedNameFactory')
        dslUser.getItem('/dsl/TestItemUseConstructorFactory')
        dslUser.getItem('/dsl/TestItemUseConstructorGeneratedNameFactory')
    }
}
