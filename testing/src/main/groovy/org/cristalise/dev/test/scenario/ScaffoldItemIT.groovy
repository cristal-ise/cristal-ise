package org.cristalise.dev.test.scenario

import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.After
import org.junit.Test

import groovy.transform.CompileStatic
import spock.util.concurrent.PollingConditions


/**
 *
 *
 */
@CompileStatic
class ScaffoldItemIT extends KernelScenarioTestBase {

    ItemProxy item

    @After
    public void after() {
        //agent.execute(item, Erase.class)
        super.after()
    }

    @Test
    public void 'Create Item using Constructor'() {
        item = createItemWithConstructorAndCheck(
            Name: "ItemUsingConstructor-$timeStamp",
            Description: 'ItemUsingConstructor description',
            "/$folder/TestItemUseConstructorFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Item using Update'() {
        item = createItemWithUpdateAndCheck(
            Name: "ItemUsingUpdate-$timeStamp",
            Description: 'ItemUsingUpdate description',
            "/$folder/TestItemFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Item using Update and Generated Name'() {
        item = createItemWithUpdateAndCheck(
            Description: 'ItemUsingUpdateGenretedName description',
            "/$folder/TestItemGeneratedNameFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Agent using Constructor'() {
        item = createItemWithConstructorAndCheck(
            Name: "AgentUsingConstructor-$timeStamp",
            Description: 'AgentUsingConstructor description',
            "/$folder/TestAgentUseConstructorFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }

    @Test
    public void 'Create Agent using Update'() {
        item = createItemWithUpdateAndCheck(
            Name: "AgentUsingUpdate-$timeStamp",
            Description: 'AgentUsingUpdate description',
            "/$folder/TestAgentFactory")

        assert item.getMasterSchema()
        assert item.getAggregateScript()
    }
}
