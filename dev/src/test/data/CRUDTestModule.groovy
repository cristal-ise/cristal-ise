Module(namespace: 'devtest', rootPackage: 'org.cristalise.devtest', webuiConfigs: true) {
    Item(name: 'Car') {
        field(name: 'RegistrationPlate')
        field(name: 'Make', values: ['BMW', 'Audi', 'Mercedes'])
    }

    Item(name: 'Motorcycle') {
        field(name: 'RegistrationPlate')
        field(name: 'Make', values:['BMW', 'Suzuki'])
    }

    Item(name: 'ClubMember') {
        field(name: 'Email', pattern: '[^@]+@[^\\\\.]+\\\\..+')

        field(name: 'FavoriteCar',        multiplicity: '0..1') { reference(itemType: 'Car') }
        field(name: 'FavoriteMotorcycle', multiplicity: '0..1') { reference(itemType: 'Motorcycle') }

        dependency(to: 'Car',        type: 'Bidirectional', cardinality: 'OneToMany')
        dependency(to: 'Motorcycle', type: 'Bidirectional', cardinality: 'OneToMany')
    }
}
