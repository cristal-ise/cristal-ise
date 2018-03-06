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

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.JooqTestBase;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.ObjectProperties;
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager;
import org.junit.After;
import org.junit.Before;

public class LookupTestBase extends JooqTestBase {

    protected JooqLookupManager lookup;

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8);

        lookup = new JooqLookupManager();

        ObjectProperties c2kProps = new ObjectProperties();

        setUpH2(c2kProps, H2Modes.PostgreSQL);

        Gateway.init(c2kProps);

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup",        lookup, true);

        lookup.open(null);
        lookup.initializeDirectory();
    }

    @After
    public void tearDown() {
        lookup.close();
        Logger.removeLogStream(System.out);
    }

    public void compare(List<Path> expecteds, Iterator<Path> actualsIter) {
        List<Path> actuals = new ArrayList<>();
        actualsIter.forEachRemaining(actuals::add);
        assertReflectionEquals(expecteds, actuals, LENIENT_ORDER);
    }

    public void compare(List<Path> expecteds, List<Path> actuals) {
        assertReflectionEquals(expecteds, actuals, LENIENT_ORDER);
    }
    public void compare(List<Path> expecteds, Path...actuals) {
        assertReflectionEquals(expecteds, actuals, LENIENT_ORDER);
    }
}
