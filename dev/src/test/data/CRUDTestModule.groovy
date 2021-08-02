Module(namespace: 'test') {
    Item(name: 'Car') {
        field(name: 'RegistrationPlate')
    }

    Item(name: 'ClubMember') {
        field(name: 'Email')

        dependency(to: 'Car', type: 'Bidirectional', cardinality: 'OneToMany')
    }
}
