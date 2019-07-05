```xsd
<xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xs:element name="PatientDetails">
        <xs:complexType>
            <xs:sequence>
                <xs:element minOccurs="1" maxOccurs="1" name="InsuranceNumber" type="xs:string"/>
                <xs:element minOccurs="1" maxOccurs="1" name="Gender" type="xs:string"/>
                <xs:element minOccurs="1" maxOccurs="1" name="DateOfBirth" type="xs:string"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>
```