Module(namespace: 'devtest', rootPackage: 'org.cristalise.devtest', webuiConfigs: true) {
    Item(name: 'Car') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'Motorcycle') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'ClubMember') {
        field(name: 'Email', pattern: '[^@]+@[^\\\\.]+\\\\..+')

        field(name: 'FavoriteCar', multiplicity: '0..1') { reference(itemType: 'Car') }

        dependency(to: 'Car',        type: 'Bidirectional', cardinality: 'OneToMany')
        dependency(to: 'Motorcycle', type: 'Bidirectional', cardinality: 'OneToMany')
    }
}
