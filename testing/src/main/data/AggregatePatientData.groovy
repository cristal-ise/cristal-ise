import groovy.transform.Field
import groovy.xml.MarkupBuilder

import org.cristalise.kernel.persistency.outcome.Outcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field Logger log = LoggerFactory.getLogger("org.cristalise.testing.scripts.Patient.Aggregate")

log.info('item:{} starting', item)

if (binding.hasVariable('job')) postFix = job.getActProp("postFix")

Outcome patientDetails = item.getViewpoint("PatientDetails-$postFix", 'last').getOutcome()
Outcome urinSample     = item.getViewpoint("UrinSample-$postFix", 'last').getOutcome()

def writer = new StringWriter()
def xml    = new MarkupBuilder(writer)

xml.AggregatedPatientData(InsuranceNumber: patientDetails.getAttribute("InsuranceNumber")) {
    DateOfBirth(  patientDetails.getField("DateOfBirth") )
    Gender(       patientDetails.getField("Gender")      )
    Weight(       patientDetails.getField("Weight")      )
    Transparency( urinSample.getField("Transparency")    )
    Color(        urinSample.getField("Color")           )
}

if (binding.hasVariable('job')) {
    job.setOutcome(writer.toString())
}
else {
    def milisec = new Random().nextInt(6) * 100
    log.info('item:{} sleeping:{} ms', item, milisec)
    Thread.sleep(new Random().nextInt(6)*100)
    PatientXML = writer.toString()
    log.info('item:{} returning', item)
}
