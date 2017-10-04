import org.cristalise.kernel.persistency.outcome.Outcome

import groovy.xml.MarkupBuilder

def postFix = job.getActProp("postFix")

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

job.setOutcome(writer.toString())
