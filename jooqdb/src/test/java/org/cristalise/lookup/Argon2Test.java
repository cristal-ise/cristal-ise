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

import org.cristalise.storage.jooqdb.auth.Argon2Password;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Argon2Test {
    char[] pwd = "12345".toCharArray();
    Argon2Password paswordHasher;

    @BeforeClass
    public static void beforeClass() {
    }

    @Before
    public void setUp() throws Exception {
        paswordHasher = new Argon2Password();
    }

    @Test @Ignore
    public void checkPasswordHash() throws Exception {
        Assert.assertTrue( paswordHasher.checkPassword(paswordHasher.hashPassword(pwd), pwd) );
    }

    @Test
    public void testSamePasswordHash() throws Exception {
        String hash1 = paswordHasher.hashPassword(pwd);
        String hash2 = paswordHasher.hashPassword(pwd);

        log.info("hash1: "+hash1);
        log.info("hash2: "+hash2);

        Assert.assertNotEquals(hash1, hash2);
        //Assert.assertTrue(paswordHasher.checkPassword(hash1, pwd));
        Assert.assertTrue(paswordHasher.checkPassword(hash2, pwd));
    }
}
