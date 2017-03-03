package org.cristalise.lookup.test

import org.apache.commons.lang3.reflect.FieldUtils
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.property.Property
import org.cristalise.storage.JooqLookupManager
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler
import org.jooq.DSLContext
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class LookupPropertySearchTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)

    ItemPath itemPath0, itemPath1
    Property propType, propStyle

    @Before
    public void setUp() throws Exception {
        super.setUp()

        itemPath0 = new ItemPath(uuid0.toString())
        itemPath1 = new ItemPath(uuid1.toString())

        propType  = new Property("Type",  "dummy", false)
        propStyle = new Property("Style", "fluffy", false)

        lookup.add(itemPath0)
        lookup.add(itemPath1)
        lookup.add(new DomainPath("toto/item0", itemPath0))
        lookup.add(new DomainPath("toto/item1", itemPath1))

        JooqItemPropertyHandler lookupPropertiesField = (JooqItemPropertyHandler)FieldUtils.getField(JooqLookupManager.class, "properties", true).get(lookup);
        DSLContext lookupContextField                 = (DSLContext)             FieldUtils.getField(JooqLookupManager.class, "context",    true).get(lookup);

        lookupPropertiesField.put(lookupContextField, uuid0, propType)
        lookupPropertiesField.put(lookupContextField, uuid1, propType)
        lookupPropertiesField.put(lookupContextField, uuid1, propStyle)
    }

    @Test
    public void searchByProperty() {
        CompareUtils.comparePathLists(
            [new DomainPath("toto/item0"), new DomainPath("toto/item1")],
            lookup.search(new DomainPath("toto"), propType))
    }

    @Test
    public void searchByTwoProperties() {
        CompareUtils.comparePathLists(
            [new DomainPath("toto/item1")],
            lookup.search(new DomainPath("toto"), propType, propStyle))
    }

    @Test
    public void searchByOneProperties_NothingFound() {
        CompareUtils.comparePathLists(
            [],
            lookup.search(new DomainPath("toto"), new Property("Style", "curly", false)))
    }
}
