package vn.edu.fpt.myfschool.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CredentialGenerator {
    private static final int LENGTH = 14;
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#%";
    private static final String ALPHABET = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private final SecureRandom random = new SecureRandom();

    public String temporaryPassword() {
        char[] value = new char[LENGTH];
        value[0] = random(UPPERCASE);
        value[1] = random(LOWERCASE);
        value[2] = random(DIGITS);
        value[3] = random(SPECIAL);
        for (int index = 4; index < value.length; index++) value[index] = random(ALPHABET);
        for (int index = value.length - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            char current = value[index];
            value[index] = value[swap];
            value[swap] = current;
        }
        return new String(value);
    }

    private char random(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
