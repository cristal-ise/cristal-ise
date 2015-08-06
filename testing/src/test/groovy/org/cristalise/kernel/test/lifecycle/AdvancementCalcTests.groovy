package org.cristalise.kernel.test.lifecycle;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.AdvancementCalculator
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.junit.Test


@CompileStatic
class AdvancementCalcTests extends WorkflowTestBase {
    
    @Test
    public void advancementCalculate() {
        CompositeActivity ca = new CompositeActivity()
        Activity act = (Activity)ca.newChild(Types.Atomic, "", true, (GraphPoint)null)

        AdvancementCalculator advCalc = new AdvancementCalculator()

        assert advCalc.getNbActLeftWithActive() == 0
        assert act.getActive() == false

        advCalc.calculate(ca)

        assert advCalc.getNbActLeftWithActive() == 1
        assert act.getActive() == false
    }
}
