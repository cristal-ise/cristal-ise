package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.predefined.item.ItemPredefinedStepContainer
import org.cristalise.kernel.utils.Logger
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@CompileStatic
class PredefinedStepTests {

    @Before
    public void init() {
        Logger.addLogStream(System.out, 8)
//        Gateway.init(null)
//        Gateway.connect()
    }

    @Test
    @Ignore("Not yet implemented") 
    public void addDomainPath() {
        def wf = new Workflow( new CompositeActivity(), new ItemPredefinedStepContainer() )

        wf.requestAction(null, "workflow/predefined/AddDomainPath", null, 0, "kovax")

        fail("Not yet implemented")
    }
}
