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

import static de.mkammerer.argon2.Argon2Factory.Argon2Types.ARGON2d;
import static de.mkammerer.argon2.Argon2Factory.Argon2Types.ARGON2i;
import static de.mkammerer.argon2.Argon2Factory.Argon2Types.ARGON2id;

import org.cristalise.kernel.process.Gateway;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Constants;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

/**
 * System properties to configure argon2:
 * 
 * <li>JooqAuth.Argon2.type - default: ARGON2id
 * <li>JooqAuth.Argon2.iterations - default: 2
 * <li>JooqAuth.Argon2.memory - default: 65536
 * <li>JooqAuth.Argon2.parallelism - default: 1
 */
public class Argon2Password {

    /**
     * Argon2 instance used only for hashing
     */
    private final Argon2 argon2;
    private final Argon2Types argon2Type;

    private final int saltLenght;
    private final int hashLenght;

    private final int iterations;
    private final int memory;
    private final int parallelism;

    public Argon2Password() {
        //TODO make argon2 setup configurable
        saltLenght = Argon2Constants.DEFAULT_SALT_LENGTH;
        hashLenght = Argon2Constants.DEFAULT_HASH_LENGTH;

        argon2Type  = Argon2Types.valueOf(Gateway.getProperties().getString("JooqAuth.Argon2.type", "ARGON2id"));
        iterations  = Gateway.getProperties().getInt("JooqAuth.Argon2.iterations", 2);
        memory      = Gateway.getProperties().getInt("JooqAuth.Argon2.memory", 65536);
        parallelism = Gateway.getProperties().getInt("JooqAuth.Argon2.parallelism", 1);

        argon2 = Argon2Factory.create(argon2Type, saltLenght, hashLenght);
    }

    /**
     * Check if the given password string produces the same hash. It is possible that there are passwords 
     * stored with different argon type if JooqAuth.Argon2 configurations were changed. 
     * 
     * @param hash the hashed password retrieved from database
     * @param password the password string
     * @return true, if the verification was successful otherwise false
     */
    public boolean checkPassword(final String hash, final char[] password) {
        Argon2Types currentType = null;

        if      (hash.startsWith("$argon2i$")) currentType = ARGON2i;
        else if (hash.startsWith("$argon2d$")) currentType = ARGON2d;
        else                                   currentType = ARGON2id;

        try {
            if (currentType == argon2Type) {
                return argon2.verify(hash, password);
            }
            else {
                return Argon2Factory.create(currentType, saltLenght, hashLenght).verify(hash, password);
            }
        }
        finally {
            argon2.wipeArray(password);
        }
    }

    /**
     * Create the hash from the password string
     * 
     * @param password the password string
     * @return the hashed password
     */
    public String hashPassword(final char[] password) {
        try {
            return argon2.hash(iterations, memory, parallelism, password);
        }
        finally {
            argon2.wipeArray(password);
        }
    }
}
