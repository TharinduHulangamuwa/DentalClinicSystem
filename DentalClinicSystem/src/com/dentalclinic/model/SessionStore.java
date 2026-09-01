package com.dentalclinic.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores the session token on the local machine.
 *
 * THIS IS THE DESKTOP EQUIVALENT OF A PERSISTENT BROWSER COOKIE.
 *
 * A browser keeps a cookie file and sends it automatically on the next visit.
 * A desktop application must do the same thing deliberately: write the token
 * at sign-in, read it at startup, delete it at sign-out.
 *
 * The file lives in the user's home directory rather than the program folder,
 * so each Windows account keeps its own token - two staff sharing one PC do
 * not inherit each other's session.
 *
 * LIMITATION, stated honestly for the report: the file is plain text. Anyone
 * with read access to that Windows account could copy it and resume the
 * session. A production system would encrypt it with a key from the operating
 * system credential store (DPAPI on Windows).
 *
 * What IS mitigated: the token is random rather than derived from the
 * password, it expires, and it can be revoked from the database without the
 * user changing their password.
 *
 * @author [Your Name]
 */
public final class SessionStore {

    private static final String FOLDER   = ".sunrise-dental";
    private static final String FILENAME = "session.token";

    private SessionStore() { }

    private static Path tokenPath() {
        return new File(System.getProperty("user.home"),
                        FOLDER + File.separator + FILENAME).toPath();
    }

    /** Writes the token so the next launch can resume this session. */
    public static void save(String token) {
        try {
            Path path = tokenPath();
            Files.createDirectories(path.getParent());
            Files.write(path, token.getBytes(StandardCharsets.UTF_8));
            System.out.println("[SESSION] token saved to " + path);
        } catch (IOException e) {
            // Failing to remember is an inconvenience, not an error: the user
            // simply signs in again next time.
            System.err.println("[SESSION] could not save token: " + e.getMessage());
        }
    }

    /** @return the stored token, or null when there is none */
    public static String load() {
        try {
            Path path = tokenPath();
            if (!Files.exists(path)) {
                return null;
            }
            String token = new String(Files.readAllBytes(path),
                                      StandardCharsets.UTF_8).trim();
            return token.isEmpty() ? null : token;
        } catch (IOException e) {
            return null;
        }
    }

    /** Deletes the token. Called at sign-out and when validation fails. */
    public static void clear() {
        try {
            if (Files.deleteIfExists(tokenPath())) {
                System.out.println("[SESSION] stored token cleared");
            }
        } catch (IOException e) {
            System.err.println("[SESSION] could not clear token: " + e.getMessage());
        }
    }

    public static boolean exists() {
        return load() != null;
    }

    /** Shown on the sessions screen so staff know where the file lives. */
    public static String location() {
        return tokenPath().toString();
    }
}
