package org.cristalise.dev.test.scenario

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test


/**
 *
 *
 */
//@CompileStatic
class ShiroPermissionTestIT extends KernelScenarioTestBase {
    
    @Test
    public void 'Job is only given to the Agent with the proper Permission'() {
        Role('oper') {
            Permission('test:EA1')
        }
        Role('clerk') {
            Permission('test:EA2')
        }

        def oper1 = Agent("oper1-$timeStamp") {
            Roles {
                Role(name: 'oper')
            }
        }.proxy

        def clerk1 = Agent("clerk1-$timeStamp") {
            Roles {
                Role(name: 'clerk')
            }
        }.proxy

        def dummyItem = Item(name: "dummyItem-$timeStamp", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1')
            }
        }.proxy

        assert dummyItem.getJobs(oper1).size() == 2;
        assert dummyItem.getJobs(clerk1).size() == 0;
    }

    @Test
    public void 'Agent can execute activities with the proper Permission'() {
        Role('oper') {
            Permission('test:first,left,last')
        }
        Role('clerk') {
            Permission('test:right,last')
        }

        def oper1 = Agent("oper1-$timeStamp") {
            Roles {
                Role(name: 'oper')
            }
        }.proxy

        def clerk1 = Agent("clerk1-$timeStamp") {
            Roles {
                Role(name: 'clerk')
            }
        }.proxy

        // this workflow cannot be generated with CompiletStatic
        def dummyItem = Item(name: "dummyItem-$timeStamp", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('first')
                AndSplit {
                   Block { EA('left') }
                   Block { EA('right') }
               }
               EA('last')
            }
        }.proxy

        checkJobs(dummyItem, oper1.path, [[stepName: "first", agentRole: null, transitionName: "Start"],
                                          [stepName: "first", agentRole: null, transitionName: "Done"]])
        checkJobs(dummyItem, clerk1.path, [])

        def oper1Job = dummyItem.getJobByName('first', oper1)
        assert oper1Job
        def clerk1Job = dummyItem.getJobByName('first', clerk1)
        assert !clerk1Job

        oper1.execute(oper1Job)

        assert dummyItem.getJobs(oper1.path).size() == 2;
        assert dummyItem.getJobs(clerk1.path).size() == 2;

        oper1Job = dummyItem.getJobByName('left', oper1)
        assert oper1Job

        oper1.execute(oper1Job)

        clerk1Job = dummyItem.getJobByName('right', clerk1)
        assert clerk1Job

        clerk1.execute(clerk1Job)

        assert dummyItem.getJobs(oper1).size() == 2;
        assert dummyItem.getJobs(clerk1).size() == 2;
    }
}
