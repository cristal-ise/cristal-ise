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

import org.apache.shiro.authc.credential.PasswordService;

public class Argon2PasswordService implements PasswordService {

    private Argon2Password paswordHasher;
    
    public Argon2PasswordService() {
        paswordHasher = new Argon2Password();
    }

    @Override
    public String encryptPassword(Object plaintextPassword) throws IllegalArgumentException {
        if (plaintextPassword instanceof char[]) {
            return paswordHasher.hashPassword((char[])plaintextPassword);
        }
        else if (plaintextPassword instanceof String) {
            return paswordHasher.hashPassword(((String)plaintextPassword).toCharArray());
        }
        else {
            throw new IllegalArgumentException("Unsupported password type: " + plaintextPassword.getClass().getName());
        }
    }

    @Override
    public boolean passwordsMatch(Object submittedPlaintext, String encrypted) {
        return paswordHasher.checkPassword(encrypted, (char[])submittedPlaintext);
    }
}
