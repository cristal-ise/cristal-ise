package org.cristalise.lookup.test

import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.utils.Logger
import org.cristalise.storage.InMemoryPropertyClusterStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

//@CompileStatic
class PropertyClusterStorageTests {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)

    InMemoryPropertyClusterStorage propClusterStore

    ItemPath itemPath0, itemPath1
    Property propType, propStyle

    @Before
    public void init() {
        Logger.addLogStream(System.out, 9);

        propClusterStore = new InMemoryPropertyClusterStorage()

        itemPath0 = new ItemPath(uuid0.toString())
        itemPath1 = new ItemPath(uuid1.toString())

        propType  = new Property("Type",  "dummy", false)
        propStyle = new Property("Style", "fluffy", false)
        
        propClusterStore.put(itemPath0, propType)
        propClusterStore.put(itemPath1, propType)
        propClusterStore.put(itemPath1, propStyle)
        propClusterStore.add(new DomainPath("toto/item0", itemPath0))
        propClusterStore.add(new DomainPath("toto/item1", itemPath1))
    }

    @After
    public void tearDown() {
        propClusterStore.close()
        Logger.removeLogStream(System.out);
    }

    @Test
    public void getProperty() {
        def propPath = ClusterStorage.getPath(propType)
        assert propPath == "Property/Type"
        Property p = (Property) propClusterStore.get(itemPath0, propPath)

        assert p
        assert p.name == "Type"
        assert p.value == "dummy"
    }

    @Test
    public void searchByProperty() {
        CompareUtils.comparePathLists(
            [new DomainPath("toto/item0"), new DomainPath("toto/item1")],
            propClusterStore.search(new DomainPath("toto"), propType))
    }

    @Test
    public void searchByTwoProperties() {
        CompareUtils.comparePathLists(
            [new DomainPath("toto/item1")],
            propClusterStore.search(new DomainPath("toto"), propType, propStyle))
    }
    
    @Test
    public void searchByOneProperties_NothingFound() {
        CompareUtils.comparePathLists(
            [],
            propClusterStore.search(new DomainPath("toto"), new Property("Style", "curly", false)))
    }
}
