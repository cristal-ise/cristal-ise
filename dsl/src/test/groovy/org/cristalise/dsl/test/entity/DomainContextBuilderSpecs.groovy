package org.cristalise.dsl.test.entity

import org.cristalise.dsl.entity.DomainContextBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification

class DomainContextBuilderSpecs extends Specification implements CristalTestSetup {
    
    def setup()   {}
    def cleanup() {}

    def "Build a list of DomainContext with namespace"() {
        when:
        def dcList = DomainContextBuilder.build('ttt') {
            DomainContext('/desc/PropertyDesc/ttt')
            DomainContext('/ttt/Doctor', 2)
        }

        then:
        dcList[0].name == "DescPropertyDescTttContext"
        dcList[0].namespace == "ttt"
        dcList[0].version == 0

        dcList[1].name == "TttDoctorContext"
        dcList[1].namespace == "ttt"
        dcList[1].version == 2
    }
}
