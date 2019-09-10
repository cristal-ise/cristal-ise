package org.cristalise.kernel.test.lifecycle.instance

import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.graph.model.GraphModel
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.renderer.LifecycleRenderer

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.common.InvalidTransitionException
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


class CAExecutionSpecs extends Specification implements CristalTestSetup {

    WorkflowTestBuilder util

    def setup() {
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8)

        util = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(util.wf)
        cristalCleanup()
    }

    def 'Execute ElemAct using Done transition'() {
        given: "Workflow containing single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute ElemAct using Start/Complete transition'() {
        given: "Workflow containing single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Start transition"
        util.requestAction( 'first', "Start")

        then: "ElemAct state is Started"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('first',   [state: "Started", active: true])

        when: "requesting ElemAct Complete transition"
        util.requestAction( 'first', "Complete" )

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute sequence of ElemActs using Done transition'() {
        given: "Workflow containing sequence of two ElemAct"
        util.buildAndInitWf { ElemAct('first'); ElemAct('second') }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "first ElemAct state is Finished and second is still Waiting"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "first ElemAct state is Finished and second is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in sequence are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in a sequence"
        util.buildAndInitWf {
            CompAct('ca') {
                ElemAct('first')
                ElemAct('second')
            }
        }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct 'first' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in AndSplit are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in AndSplit"
        util.buildAndInitWf {
            CompAct('ca') {
                AndSplit {
                    Block { ElemAct('left')  }
                    Block { ElemAct('right') }
                }
            }
        }
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Waiting",  active: true])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting left ElemAct Done transition"
        util.requestAction('left', "Done")

        then: "ElemAct 'left' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting right ElemAct Done transition"
        util.requestAction('right', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in AndSplit with Loops are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in AndSplit"
        Workflow wf = util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                AndSplit {
                     Block {
                        Loop(RoutingScriptName: 'javascript:\"false\";') {
                            ElemAct('left')
                        }
                    }
                    Block {
                        Loop(RoutingScriptName: 'javascript:\"false\";') {
                           ElemAct('right')
                        }
                   }
                }
            }
            ElemAct('last')
        }
        when: "requesting 'left' ElemAct Done transition"
        util.requestAction('left', "Done")

        then: "ElemAct 'left' should be finished and inactive, ElemAct 'right' should be waiting and active, ElemAct 'last' should be inactive"
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Waiting", active: true])
        util.checkActStatus('last',   [state: "Waiting", active: false])

        when: "requesting 'right' ElemAct Done transition"
        util.requestAction('right', "Done")

        then: "ElemAct 'left' and right should be FInished and inactive, ElemAct 'last' should be Waiting and active"
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Finished", active: false])
        util.checkActStatus('last',   [state: "Waiting", active: true])

        //Print images of workflow to debug easily
        GraphModel wfGraphModel = wf.search("workflow/domain").getChildrenGraphModel()
        println Gateway.getMarshaller().marshall(wfGraphModel)
        DefaultGraphLayoutGenerator.layoutGraph(wfGraphModel)
        BufferedImage imgWf = new LifecycleRenderer(wfGraphModel, false).getWorkFlowModelImage(1920, 1080)
        ImageIO.write(imgWf, "png", new File("target/workflowTest.png"))

        GraphModel caGraphModel = wf.search("workflow/domain/ca").getChildrenGraphModel()
        DefaultGraphLayoutGenerator.layoutGraph(caGraphModel)
        BufferedImage imgCa = new LifecycleRenderer(caGraphModel, false).getWorkFlowModelImage(1920, 1080)
        ImageIO.write(imgCa, "png", new File("target/CATest.png"))
    }

    def 'CompAct is automatically finished when all Activities in Loop are finished'() {
        given: "Workflow containing CompAct containing 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop(RoutingScriptName: 'javascript:\"false\";') { //loop shall finish automatically
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('one',    [state: "Finished", active: false])
        util.checkActStatus('ca',     [state: "Finished", active: false])
    }

    def 'CompAct with infinitive Loop never finishes'() {
        given: "Workflow containing CompAct containing 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop { //by default the DSL creates infinitive Loop
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct are still active"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('one',    [state: "Waiting", active: true])
        util.checkActStatus('ca',     [state: "Started", active: true])
    }

    def 'Empty Wf cannot be executed'() {
        given: "empty Workflow"
        util.buildAndInitWf {
            //checks before Wf.init()
            util.checkActStatus('rootCA', [state: "Waiting", active: false])
        }

        when: "requesting Root CompAct Complete transition"
        util.requestAction('rootCA', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    public void 'Cannot execute Wf containing empty CompAct'() {
        given: "empty Workflow containing empty CompAct"
        util.buildAndInitWf {
            CompAct('ca') {}

            //checks before Wf.init()
            util.checkActStatus('rootCA', [state: "Waiting", active: false])
            util.checkActStatus('ca',     [state: "Waiting", active: false])
        }
        //checks after Wf.init()
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('ca',   [state: "Waiting", active: true])

        when: "requesting CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Cannot complete Compact without finishing all ElemActs'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                ElemAct('first')
            }
        }

        when: "requesting CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Compact can be finished with active children if Abortable'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                ElemAct('first')
            }
        }
        util.checkActStatus('ca',   [state: "Started", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "CompAct is finished, child is inactive but Waiting"
        util.checkActStatus('ca',    [state: "Finished", active: false])
        util.checkActStatus('first', [state: "Waiting",  active: false])
    }

    def 'Cannot Complete Root Compact without finishing all CompActs'() {
        given: "Workflow containing single and empty CompAct"
        util.buildAndInitWf { CompAct{} }

        when: "requesting Root CompAct Complete transition"
        util.requestAction('rootCA', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }
}
