
// this is defined in CrudState.groovy of the dev module
states = ['ACTIVE', 'INACTIVE']

 
PropertyDescriptionList('Car', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Car')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('ClubMember', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'ClubMember')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('Motorcycle', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Motorcycle')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestAgent', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgent')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestAgentUseConstructor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgentUseConstructor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestItem', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItem')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestItemExcel', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemExcel')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestItemGeneratedName', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemGeneratedName')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestItemUseConstructor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
} 
PropertyDescriptionList('TestItemUseConstructorGeneratedName', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructorGeneratedName')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}
