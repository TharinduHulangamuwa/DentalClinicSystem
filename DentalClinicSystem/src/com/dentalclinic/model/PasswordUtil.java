package com.dentalclinic.model;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes passwords and session tokens with SHA-256, so neither is ever
 * stored in readable form.
 *
 * The seed data was inserted with MySQL's SHA2(value, 256), which produces
 * the identical 64-character lowercase hex digest this class produces. That
 * is why sign-in works against the seeded accounts.
 *
 * For the report: a production system would add a random per-user salt and
 * a deliberately slow algorithm such as bcrypt or PBKDF2, which resist
 * brute-force attack. SHA-256 is used here because it is in the standard
 * library with no extra dependency, which suits the scope of this work.
 *
 * @author [Your Name]
 */
public final class PasswordUtil {

    private PasswordUtil() { }

    /** @return 64-character lowercase hexadecimal SHA-256 digest */
    public static String hash(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("Value to hash must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plainText.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
