Module(namespace: 'devtest') {
    Item(name: 'Car') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'Motorcycle') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'ClubMember') {
        field(name: 'Email')

        dependency(to: 'Car',        type: 'Bidirectional', cardinality: 'OneToMany')
        dependency(to: 'Motorcycle', type: 'Bidirectional', cardinality: 'OneToMany')
    }
}
