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
