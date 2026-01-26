package org.amalitech.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean check(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }
}
