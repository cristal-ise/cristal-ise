package org.cristalise.dev.test.scaffold

import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class DevScaffoldedModuleTests implements CristalTestSetup {

    
    @Before
    public void setup() {
        def props = new Properties()
        props.put('Resource.moduleUseFileNameWithVersion', 'dev,devtest')
        inMemoryServer(-1, props)
    }

    @After
    public void cleanup() {
        cristalCleanup()
    }

    @Test
    public void checkItemsdevtestTestModule() {
        def devtestUser = Gateway.getProxyManager().getAgentProxy('devtest')
        devtestUser.getItem('/desc/AgentDesc/devtest/devtest')

        devtestUser.getItem('/desc/ItemDesc/devtest/TestAgentFactory')
        devtestUser.getItem('/desc/ItemDesc/devtest/TestAgentUseConstructorFactory')
        devtestUser.getItem('/desc/ItemDesc/devtest/TestItemFactory')
        devtestUser.getItem('/desc/ItemDesc/devtest/TestItemGeneratedNameFactory')
        devtestUser.getItem('/desc/ItemDesc/devtest/TestItemUseConstructorFactory')
        devtestUser.getItem('/desc/ItemDesc/devtest/TestItemUseConstructorGeneratedNameFactory')
        devtestUser.getItem('/devtest/TestAgentFactory')
        devtestUser.getItem('/devtest/TestAgentUseConstructorFactory')
        devtestUser.getItem('/devtest/TestItemFactory')
        devtestUser.getItem('/devtest/TestItemGeneratedNameFactory')
        devtestUser.getItem('/devtest/TestItemUseConstructorFactory')
        devtestUser.getItem('/devtest/TestItemUseConstructorGeneratedNameFactory')
    }
}
