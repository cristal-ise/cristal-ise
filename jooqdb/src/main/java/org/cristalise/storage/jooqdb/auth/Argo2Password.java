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
