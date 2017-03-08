/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.lookup.test

import org.apache.commons.lang3.reflect.FieldUtils
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.property.Property
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager
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
            [new DomainPath("toto/item0", itemPath0), new DomainPath("toto/item1", itemPath1)],
            lookup.search(new DomainPath("toto"), propType))
    }

    @Test
    public void searchByTwoProperties() {
        CompareUtils.comparePathLists(
            [new DomainPath("toto/item1", itemPath1)],
            lookup.search(new DomainPath("toto"), propType, propStyle))
    }

    @Test
    public void searchByOneProperties_NothingFound() {
        CompareUtils.comparePathLists(
            [],
            lookup.search(new DomainPath("toto"), new Property("Style", "curly", false)))
    }
}
