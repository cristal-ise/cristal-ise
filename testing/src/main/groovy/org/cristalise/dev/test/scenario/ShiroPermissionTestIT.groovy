package org.cristalise.dev.test.scenario

import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 *
 *
 */
class ShiroPermissionTestIT extends KernelScenarioTestBase {
    
    @Test
    public void 'Job is only given to the Agent with the proper Permission'() {
        Roles {
            Role(name: 'oper') {
                Permission('test:EA1')
            }
            Role(name: 'clerk') {
                Permission('test:EA2')
            }
        }

        def oper1 = Agent("oper1-$timeStamp") {
            Roles {
                Role(name: 'oper')
            }
        }

        def clerk1 = Agent("clerk1-$timeStamp") {
            Roles {
                Role(name: 'clerk')
            }
        }

        def dummyItem = Item(name: "dummyItem-$timeStamp", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1')
            }
        }
        
        def dummyItemP = agent.getItem(dummyItem.getItemPath())

        assert dummyItemP.getJobList(oper1.getItemPath(),  true).size() == 2;
        assert dummyItemP.getJobList(clerk1.getItemPath(), true).size() == 0;
    }

    @Test
    public void 'Agent can execute activities with the proper Permission'() {
        Roles {
            Role(name: 'oper') {
                Permission('test:first,left,last')
            }
            Role(name: 'clerk') {
                Permission('test:right,last')
            }
        }

        def oper1 = Agent("oper1-$timeStamp") {
            Roles {
                Role(name: 'oper')
            }
        }.agentPath

        def clerk1 = Agent("clerk1-$timeStamp") {
            Roles {
                Role(name: 'clerk')
            }
        }.agentPath

        def dummy = Item(name: "dummyItem-$timeStamp", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('first')
                AndSplit {
                   Block { ElemAct("left") }
                   Block { ElemAct("right") }
               }
               EA('last')
            }
        }

        def dummyItem = agent.getItem(dummy.getItemPath())

        checkJobs(dummyItem, oper1, [[stepName: "first", agentRole: null, transitionName: "Start"],
                                               [stepName: "first", agentRole: null, transitionName: "Done"]])
        checkJobs(dummyItem, clerk1, [])

        def oper1Job = dummyItem.getJobByName('first', oper1)
        assert oper1Job
        def clerk1Job = dummyItem.getJobByName('first', clerk1)
        assert !clerk1Job
        dummyItem.requestAction(oper1Job)

        assert dummyItem.getJobList(oper1,  true).size() == 2;
        assert dummyItem.getJobList(clerk1, true).size() == 2;

        oper1Job = dummyItem.getJobByName('left', oper1)
        assert oper1Job
        dummyItem.requestAction(oper1Job)

        clerk1Job = dummyItem.getJobByName('right', clerk1)
        assert clerk1Job
        dummyItem.requestAction(clerk1Job)

        assert dummyItem.getJobList(oper1,  true).size() == 2;
        assert dummyItem.getJobList(clerk1, true).size() == 2;
    }
}
