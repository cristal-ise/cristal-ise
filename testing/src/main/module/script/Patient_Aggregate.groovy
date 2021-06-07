import groovy.transform.Field
import groovy.xml.MarkupBuilder

import org.cristalise.kernel.persistency.outcome.Outcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field Logger log = LoggerFactory.getLogger("org.cristalise.testing.scripts.Patient.Aggregate")

log.info('item:{} starting', item)

if (binding.hasVariable('job')) postFix = job.getActProp("postFix")

Outcome patientDetails
Outcome urinSample

if (binding.hasVariable('postFix') && postFix && postFix != 'string') {
    patientDetails = item.getViewpoint("PatientDetails-$postFix", 'last').getOutcome()
    urinSample     = item.getViewpoint("UrinSample-$postFix", 'last').getOutcome()
}
else {
    patientDetails = item.getViewpoint("Patient_Details", 'last').getOutcome()
    urinSample     = item.getViewpoint("Patient_UrinSample", 'last').getOutcome()
}

def writer = new StringWriter()
def xml    = new MarkupBuilder(writer)

xml.Patient(InsuranceNumber: patientDetails.getAttribute("InsuranceNumber")) {
    DateOfBirth(  patientDetails.getField("DateOfBirth") )
    Gender(       patientDetails.getField("Gender")      )
    Weight(       patientDetails.getField("Weight")      )
    Transparency( urinSample.getField("Transparency")    )
    Color(        urinSample.getField("Color")           )
}

if (binding.hasVariable('job')) {
    job.setOutcome(writer.toString())
    log.debug('item:{} job outcome was set:{}', item, writer)
}
else {
    PatientXML = writer.toString()
    log.debug('item:{} returning:{}', item, PatientXML)
}
