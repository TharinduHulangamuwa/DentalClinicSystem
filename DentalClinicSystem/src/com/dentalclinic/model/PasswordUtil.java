package com.dentalclinic.model;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes passwords with SHA-256 so plain text is never stored in the
 * database and never compared in memory.
 *
 * The MySQL seed data was inserted using SHA2(password, 256), which produces
 * exactly the same 64-character lowercase hex digest this class produces.
 * That is why login works against the seeded accounts.
 *
 * Note for the report: a production system would additionally use a random
 * per-user salt and a slow algorithm such as bcrypt or PBKDF2. SHA-256 is
 * used here because it is available in the standard library with no extra
 * dependency, which suits the scope of this assessment.
 */
public class PasswordUtil {

    private PasswordUtil() { }

    /**
     * @param plainText the password typed by the user
     * @return 64-character lowercase hexadecimal SHA-256 digest
     */
    public static String hash(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plainText.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
