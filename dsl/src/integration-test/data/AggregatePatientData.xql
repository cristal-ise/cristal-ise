xquery version "3.0";

declare function local:convertTimeStamp($ts as node()) as xs:dateTime {
    let $Y  := $ts/TimeStamp/@Y/string()
    let $Mo := format-number(xs:integer($ts/TimeStamp/@Mo/string()),  '00')
    let $D  := format-number(xs:integer($ts/TimeStamp/@D/string()),   '00')
    let $H  := format-number(xs:integer($ts/TimeStamp/@H/string()),   '00')
    let $Mi := format-number(xs:integer($ts/TimeStamp/@Mi/string()),  '00')
    let $S  := format-number(xs:integer($ts/TimeStamp/@S/string()),   '00')

    (: Compute timezone shift converting miliseconds to hours :)
    (: let $hours := format-number((xs:integer($ts/TimeStamp/@O/string()) div 3600000), '00') :)
    (: return xs:dateTime(concat($Y,'-',$Mo,'-',$D,'T',$H,':',$Mi,':',$S,'+-',$zoneShift)) :)

    return xs:dateTime(concat($Y,'-',$Mo,'-',$D,'T',$H,':',$Mi,':',$S))
};

declare function local:resolveViewpoint($root as xs:string, $uuid as xs:string, $schema as xs:string, $view as xs:string) as node() {
    let $wp :=  doc(concat($root, '/', $uuid, '/ViewPoint.', $schema, '.', $view))
    return $wp
};

declare function local:resolveViewpointEventID($root as xs:string, $uuid as xs:string, $schema as xs:string, $view as xs:string) as node() {
    let $n := doc(concat($root, '/', $uuid, '/ViewPoint.', $schema, '.', $view))/Viewpoint/@Last
    return $n
};

declare function local:resolveViewpointVersion($root as xs:string, $uuid as xs:string, $schema as xs:string, $view as xs:string) as node() {
    let $n := doc(concat($root, '/', $uuid, '/ViewPoint.', $schema, '.', $view))/Viewpoint/@SchemaVersion
    return $n
};

declare function local:resolveViewpointOutcome($root as xs:string, $uuid as xs:string, $schema as xs:string, $view as xs:string) as node() {
    let $eventId := local:resolveViewpointEventID($root, $uuid, $schema, $view)/string()
    let $version := local:resolveViewpointVersion($root, $uuid, $schema, $view)/string()

    return doc(concat($root, '/', $uuid, '/Outcome.', $schema, '.', $version, '.', $eventId))
};

declare function local:resolveViewpointEvent($root as xs:string, $uuid as xs:string, $schema as xs:string, $view as xs:string) as node() {
    let $eventId := local:resolveViewpointEventID($root, $uuid, $schema, $view)/string()

    return doc(concat($root, '/', $uuid, '/AuditTrail.', $eventId))
};

let $patientDetails := local:resolveViewpointOutcome($rootCont, $itemUUID, concat('PatientDetails-', $postFix), 'last')/PatientDetails
let $urinSample     := local:resolveViewpointOutcome($rootCont, $itemUUID, concat('UrinSample-',     $postFix), 'last')/UrinSample

return
<AggregatedPatientData>
    {$patientDetails/@InsuranceNumber}
    {$patientDetails/DateOfBirth}
    {$patientDetails/Gender}
    {$patientDetails/Weight }
    {$urinSample/Transparency}
    {$urinSample/Color}
</AggregatedPatientData>
