Schema('RoleDesc_Details', 0) {
    struct(name: 'RoleDesc_Details', useSequence: true) {
        field(name: 'Name', type: 'string',  multiplicity: '1..1')
        field(name: 'JobList', type: 'boolean', default: 'false', multiplicity: '1..1')
    }
}

Script('PropertyDescription_QueryList', 0) {
    output('PropertyDescriptionMap', 'java.util.Map')
    script('groovy', "src/main/kernel-module/script/PropertyDescription_QueryList.groovy")
}

Script('RoleDesc_EditPermission', 0) {
    script('groovy', "src/main/kernel-module/script/RoleDesc_EditPermission.groovy")
}

Script('RoleDesc_EditPermissionUpdateScript', 0) {
    output("outputXML", "java.lang.String")
    script('groovy', "src/main/kernel-module/script/RoleDesc_EditPermissionUpdateScript.groovy")
}

Script('RoleDesc_CreateNewVersion', 0) {
    script('groovy', "src/main/kernel-module/script/RoleDesc_CreateNewVersion.groovy")
}

Script('RoleDesc_UseCurrentVersion', 0) {
    script('groovy', "src/main/kernel-module/script/RoleDesc_UseCurrentVersion.groovy")
}

Schema('Permission', 0) {
    struct(name:'Permission', useSequence: true) {
        field(name: 'Domains', type: 'string', multiplicity: '1..1') { //cannot use *
            listOfValues(scriptRef: $propertyDescription_QueryList_Script)
            dynamicForms(label: 'Type')
        }
        field(name: 'Actions', type: 'string', default: '*', multiplicity: '0..1') { //ActNames from the type listed in Domains
            dynamicForms(label: 'Activities', multiple: true, updateScriptRef: $roleDesc_EditPermissionUpdateScript_Script)
        }
        field(name: 'Targets', type: 'string', default: '*', multiplicity: '0..1') { //Instances from the type listed in Domains
            dynamicForms(label: 'Instances', multiple: true, updateScriptRef: $roleDesc_EditPermissionUpdateScript_Script)
        }
    }
}

Schema('RoleDesc', 0) {
    struct(name: 'Role', useSequence: true) {
        field(name: 'Name', type: 'string',  multiplicity: '1..1')
        field(name: 'Version', type: 'integer', multiplicity: '0..1')
        field(name: 'Id', type: 'string',  multiplicity: '0..1', pattern: patternUuid, length: 36) {
            dynamicForms(disabled: true)
        }
        field(name: 'JobList', type: 'boolean', default: 'false', multiplicity: '1..1')

        struct(name:'Permission', multiplicity: '0..*', useSequence: true) {
            field(name: 'Domains', type: 'string', multiplicity: '1..1')
            field(name: 'Actions', type: 'string', multiplicity: '1..1')
            field(name: 'Targets', type: 'string', multiplicity: '1..1')
        }
    }
}

Activity('RoleDesc_Edit', 0) {
    Property(Description: 'Edit the Role Description')

    Schema($roleDesc_Details_Schema)
}

Activity('RoleDesc_EditPermission', 0) {
    Property(Description: 'Edit the Permission for one Item type')
    Property(Viewpoint: 'xpath:/Permission/Domains')

    Schema($permission_Schema)
}

Activity('RoleDesc_CreateNewVersion', 0) {
    Property(Description: '')

    Schema($roleDesc_Schema)
    Script($roleDesc_CreateNewVersion_Script)
}

Activity('RoleDesc_UseCurrentVersion', 0) {
    Property(Description: '')

    Schema($roleDesc_Schema)
    Script($roleDesc_UseCurrentVersion_Script)
}
