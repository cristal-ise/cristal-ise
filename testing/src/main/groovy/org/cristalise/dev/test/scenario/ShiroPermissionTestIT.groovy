package org.cristalise.dev.test.scenario

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 *
 *
 */
class ShiroPermissionTestIT extends KernelScenarioTestBase {
    
    @Test
    public void 'Job is only given to the Agent with the proper Permission'() {
        def oper1 = Agent("oper1-$timeStamp") {
            Roles {
                Role(name: 'oper') {
                    Permission('test:EA1')
                }
            }
        }

        def clerk1 = Agent("clerk1-$timeStamp") {
            Roles {
                Role(name: 'clerk') {
                    Permission('test:EA2')
                }
            }
        }

        def dummyItem = Item(name: "dummyItem-$timeStamp", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1')
            }
        }
        
        def dummyItemP = agent.getItem(dummyItem.getItemPath())

        dummyItemP.getJobList(oper1.getItemPath(),  true).size() == 2;
        dummyItemP.getJobList(clerk1.getItemPath(), true).size() == 0;
    }
}
