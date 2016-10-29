xquery version "3.0";

let $pdSchema       := concat('PatientDetails-', $postFix)
let $pdEventID      := doc(concat($itemUUID, '/ViewPoint.', $pdSchema, '.last'))/Viewpoint/@Last/string()
let $pdEventVer     := doc(concat($itemUUID, '/ViewPoint.', $pdSchema, '.last'))/Viewpoint/@SchemaVersion/string()
let $patientDetails := doc(concat($itemUUID, '/Outcome.',   $pdSchema, '.', $pdEventVer, '.', $pdEventID))/PatientDetails

let $usSchema   := concat('UrinSample-', $postFix)
let $usEventID  := doc(concat($itemUUID, '/ViewPoint.', $usSchema, '.last'))/Viewpoint/@Last/string()
let $usEventVer := doc(concat($itemUUID, '/ViewPoint.', $usSchema, '.last'))/Viewpoint/@SchemaVersion/string()
let $urinSample := doc(concat($itemUUID, '/Outcome.',   $usSchema, '.', $usEventVer, '.', $usEventID))/UrinSample

return
<AggregatedPatientData>
    {$patientDetails/@InsuranceNumber}
    {$patientDetails/DateOfBirth}
    {$patientDetails/Gender}
    {$patientDetails/Weight }
    {$urinSample/Transparency}
    {$urinSample/Color}
</AggregatedPatientData>
