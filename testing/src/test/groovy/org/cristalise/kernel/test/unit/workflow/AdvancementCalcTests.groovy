package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.AdvancementCalculator
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before
import org.junit.Test


@CompileStatic
class AdvancementCalcTests {
    
    @Before
    public void init() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()
    }

    @After
    public void tearDown() {
        Gateway.close()
    }
    
    
    @Test
    public void advancementCalculate() {
        CompositeActivity ca = new CompositeActivity()
        Activity act = new Activity()

        ca.addChild(act, new GraphPoint())
        ca.getChildrenGraphModel().setStartVertexId(act.getID())

        AdvancementCalculator advCalc = new AdvancementCalculator()

        assert advCalc.getNbActLeftWithActive() == 0
        assert act.getActive() == false

        advCalc.calculate(ca)

        assert advCalc.getNbActLeftWithActive() == 1
        assert act.getActive() == false
    }
}
