Module(namespace: 'devtest', rootPackage: 'org.cristalise.devtest', webuiConfigs: true) {
    Item(name: 'Car') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'Motorcycle') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'ClubMember') {
        field(name: 'MyCars', multiplicity: '0..*') { reference(itemType: 'Car') }
        field(name: 'Email', pattern: '[^@]+@[^\\\\.]+\\\\..+')

        dependency(to: 'Car',        type: 'Bidirectional', cardinality: 'OneToMany')
        dependency(to: 'Motorcycle', type: 'Bidirectional', cardinality: 'OneToMany')
    }
}
