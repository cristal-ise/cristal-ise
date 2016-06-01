package org.cristalise.lookup.test

import groovy.transform.CompileStatic

import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.property.Property
import org.cristalise.storage.InMemoryPropertyClusterStorage
import org.junit.Before
import org.junit.Test

@CompileStatic
class PropertyClusterStorageTests {
    
    InMemoryPropertyClusterStorage propClusterStore
    
    ItemPath itemPath
    Property prop

    @Before
    public void init() {
        propClusterStore = new InMemoryPropertyClusterStorage()
        
        itemPath = new ItemPath()
        prop = new Property("Type", "dummy", false)

        propClusterStore.put(itemPath, prop)
    }

    @Test
    public void getTests() {
        def propPath = ClusterStorage.getPath(prop)
        assert propPath == "/Property/Type"
        assert prop == propClusterStore.get(itemPath, propPath)
    }

}
