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
package org.cristalise.storage.jooqdb.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Constants;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

public class Argo2Password {

    private Argon2 argon2 = null;

    final Argon2Types type;
    final int         saltLenght;
    final int         hashLenght;
    
    private final int iterations;
    private final int memory;
    private final int parallelism;

    public Argo2Password() {
        type       = Argon2Types.ARGON2i;
        saltLenght = Argon2Constants.DEFAULT_SALT_LENGTH;
        hashLenght = Argon2Constants.DEFAULT_HASH_LENGTH;

        argon2 = Argon2Factory.create(type, saltLenght, hashLenght);

        iterations = 2;
        memory = 65536;
        parallelism = 1;
    }

    public boolean checkPassword(final String hash, final char[] password) {
        return argon2.verify(hash, password);
    }

    public String hashPassword(final char[] password) {
        try {
            return argon2.hash(iterations, memory, parallelism, password);
        }
        finally {
            argon2.wipeArray(password);
        }
    }
}
