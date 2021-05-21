package org.cristalise.kernel.test.scenario

import java.time.LocalDateTime

import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.events.History
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class EditDescItemSpecs extends Specification implements CristalTestSetup {
    static AgentProxy agent = null
    static String timeStamp = null

    PollingConditions pollingWait = new PollingConditions(timeout: 2, initialDelay: 0.2, factor: 1)
    
    def setupSpec() {
        cristalInit(8, 'src/main/bin/client.conf', 'src/main/bin/integTest.clc')
        agent = Gateway.connect('user', 'test')
        timeStamp = LocalDateTime.now().format("yyyy-MM-dd_HH-mm-ss_SSS")
    }

    def cleanupSpec() { cristalCleanup() }

    def 'Agent can be updated using UpdateAgent activity of AgentDesc resource Item'() {
        given:
        def mainUser = agent.getItem('/desc/AgentDesc/integTest/mainUser')
        
        def mainUserHistory = mainUser.getHistory()
        def lastEventId = mainUserHistory.getLastId()

        when:
        def job = mainUser.getJobByTransitionName('UpdateAgent', 'Done', agent)
        agent.execute(job)

        then:
        pollingWait.eventually { mainUserHistory.getLastId() == lastEventId + 1 }
        mainUserHistory.getEvent(lastEventId + 1).stepName == 'UpdateAgent'
    }

    def 'Item, i.e. Factory, can be updated using UpdateItem activity of ItemDesc resource Item'() {
        given:
        def testItemGeneratedNameFactory = agent.getItem('/desc/ItemDesc/integTest/PatientFactory')
        def testItemGeneratedNameFactoryHistory = testItemGeneratedNameFactory.getHistory()
        def lastEventId = testItemGeneratedNameFactoryHistory.getLastId()

        when:
        def job = testItemGeneratedNameFactory.getJobByTransitionName('UpdateItem', 'Done', agent)
        agent.execute(job)

        then:
        pollingWait.eventually { testItemGeneratedNameFactoryHistory.getLastId() == lastEventId + 1 }
        testItemGeneratedNameFactoryHistory.getEvent(lastEventId + 1).stepName == 'UpdateItem'
    }
    
    def 'Role can be updated using UpdateRole activity of RoleDesc resource Item'() {
        given:
        def userRole = agent.getItem('/domain/desc/RoleDesc/integTest/User')
        def userRoleHistory = userRole.getHistory()
        def lastEventId = userRoleHistory.getLastId()

        when:
        def job = userRole.getJobByTransitionName('UpdateRole', 'Done', agent)
        agent.execute(job)

        then:
        pollingWait.eventually { userRoleHistory.getLastId() == lastEventId + 2 }
        userRoleHistory.getEvent(lastEventId + 2).stepName == 'UpdateRole'
    }
}
