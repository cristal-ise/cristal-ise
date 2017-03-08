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
package org.cristalise.lookup.test;

import static org.junit.Assert.*

import org.apache.commons.lang3.reflect.FieldUtils
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger
import org.cristalise.kernel.utils.ObjectProperties
import org.cristalise.storage.JooqClusterStorage
import org.cristalise.storage.jooqdb.JooqHandler
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager
import org.junit.After
import org.junit.Before
import org.springframework.cache.config.CacheAdviceParser.Props

import groovy.transform.CompileStatic


@CompileStatic
class LookupTestBase {

    JooqLookupManager lookup

    @Before
    public void setUp() throws Exception {
        Logger.addLogStream(System.out, 8)

        lookup = new JooqLookupManager()

        ObjectProperties c2kProps = new ObjectProperties()

        c2kProps.put(JooqHandler.JOOQ_URI,      "jdbc:h2:mem:")
        c2kProps.put(JooqHandler.JOOQ_USER,     "sa")
        c2kProps.put(JooqHandler.JOOQ_PASSWORD, "sa")
        c2kProps.put(JooqHandler.JOOQ_DIALECT,  "H2")

        Gateway.init(c2kProps)

        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", lookup, true)
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookup",        lookup, true)
//        FieldUtils.writeDeclaredStaticField(Gateway.class, "mC2KProps",      c2kProps, true)

        lookup.open(null)
        lookup.initializeDirectory()
    }

    @After
    public void tearDown() {
        lookup.close()
        Logger.removeLogStream(System.out);
    }
}
