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
package org.cristalise.lookup;

import static org.junit.Assert.assertEquals;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.property.Property;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;

public class LookupPropertySearchTest extends LookupTestBase {

    UUID uuid0 = new UUID(0,0);
    UUID uuid1 = new UUID(0,1);

    ItemPath itemPath0, itemPath1;
    Property propType, propStyle;

    JooqItemPropertyHandler lookupPropertiesField;
    DSLContext lookupContextField;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        itemPath0 = new ItemPath(uuid0.toString());
        itemPath1 = new ItemPath(uuid1.toString());

        propType  = new Property("Type",  "dummy", false);
        propStyle = new Property("Style", "fluffy", false);

        lookup.add(itemPath0);
        lookup.add(itemPath1);
        lookup.add(new DomainPath("toto/item0", itemPath0));
        lookup.add(new DomainPath("toto/item1", itemPath1));

        lookupPropertiesField = (JooqItemPropertyHandler)FieldUtils.getField(JooqLookupManager.class, "properties", true).get(lookup);
        lookupContextField    = JooqHandler.connect();
        lookupPropertiesField.put(lookupContextField, uuid0, propType);
        lookupPropertiesField.put(lookupContextField, uuid1, propType);
        lookupPropertiesField.put(lookupContextField, uuid1, propStyle);
    }

    @Test
    public void searchByProperty() {
        compare( Arrays.asList(new DomainPath("toto/item0", itemPath0), new DomainPath("toto/item1", itemPath1)),
                lookup.search(new DomainPath("toto"), propType));
    }

    @Test
    public void searchByTwoProperties() {
        compare( Arrays.asList(new DomainPath("toto/item1", itemPath1)),
                lookup.search(new DomainPath("toto"), propType, propStyle));
    }

    @Test
    public void searchByOneProperties_NothingFound() {
        assert ! lookup.search(new DomainPath("toto"), new Property("Style", "curly", false)).hasNext();
    }

    @Test
    public void searchByProperty_paged() throws Exception {
        List<DomainPath> expecteds = new ArrayList<>();

        for (int i = 0; i < 35; i++) {
            ItemPath ip = new ItemPath(UUID.randomUUID());
            DomainPath dp = new DomainPath("paged/item" + StringUtils.leftPad(""+i, 2, "0"), ip);

            lookup.add(ip);
            lookup.add(dp);
            lookupPropertiesField.put(lookupContextField, ip.getUUID(), propType);

            expecteds.add(dp);
        }

        PagedResult actuals;

        for (int i = 0; i < 3; i++) {
            actuals = lookup.search(new DomainPath("paged"), Arrays.asList(propType), i*10, 10);

            assertEquals(35, actuals.maxRows);
            assertReflectionEquals(expecteds.subList(i*10, i*10+10), actuals.rows, LENIENT_ORDER);
        }

        actuals = lookup.search(new DomainPath("paged"), Arrays.asList(propType), 0, 10);

        assertEquals(35, actuals.maxRows);
        assertReflectionEquals(expecteds.subList(0, 10), actuals.rows, LENIENT_ORDER);
    }
}
