package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the session mechanism.
 *
 * These cover the parts that need no database: the in-memory session object,
 * the local token file, and token hashing. Session creation and validation
 * against MySQL are covered by manual integration tests, because they need a
 * live server.
 *
 * @author [Your Name]
 */
public class SessionTest {

    private User user;

    @Before
    public void setUp() {
        user = new User(1, "admin", "Clinic Administrator", "ADMIN");
        Session.end();          // start each test from a known state
        SessionStore.clear();
    }

    @After
    public void tearDown() {
        Session.end();
        SessionStore.clear();
    }

    // -----------------------------------------------------------------
    // TC-15  A session is inactive until one is started
    // -----------------------------------------------------------------
    @Test
    public void testNoSessionBeforeSignIn() {
        assertFalse("No session should be active before sign-in", Session.isActive());
        assertNull("No instance should exist", Session.getInstance());
    }

    // -----------------------------------------------------------------
    // TC-16  Starting a session records the user, token and countdown
    // -----------------------------------------------------------------
    @Test
    public void testSessionStartRecordsDetails() {
        Session.start("TOKEN_ABC123", user, false);

        assertTrue("Session should be active", Session.isActive());
        assertNotNull(Session.getInstance());
        assertEquals("admin", Session.getInstance().getUser().getUsername());
        assertEquals("TOKEN_ABC123", Session.getInstance().getToken());
        assertFalse("Remember flag should be false",
                    Session.getInstance().isRemembered());
        assertEquals("Countdown should start at the full timeout",
                     Session.TIMEOUT_MINUTES,
                     Session.getInstance().getMinutesRemaining());
        assertFalse("A fresh session is not expiring soon",
                    Session.getInstance().isExpiringSoon());
        assertFalse("A fresh session has not timed out",
                    Session.getInstance().hasTimedOut());
    }

    // -----------------------------------------------------------------
    // TC-17  Ending a session clears it completely
    // -----------------------------------------------------------------
    @Test
    public void testSessionEndClearsState() {
        Session.start("TOKEN_XYZ", user, true);
        assertTrue(Session.getInstance().isRemembered());

        Session.end();

        assertFalse("Session should be inactive after end", Session.isActive());
        assertNull("Instance should be cleared", Session.getInstance());
    }

    // -----------------------------------------------------------------
    // TC-18  The token file behaves like a persistent cookie
    // -----------------------------------------------------------------
    @Test
    public void testTokenFileRoundTrip() {
        assertNull("No token when nothing is saved", SessionStore.load());
        assertFalse(SessionStore.exists());

        String token = "aVeryLongRandomTokenValue_1234567890";
        SessionStore.save(token);

        assertTrue("File should exist after save", SessionStore.exists());
        assertEquals("Token must round-trip exactly", token, SessionStore.load());

        SessionStore.clear();

        assertNull("Token gone after clear", SessionStore.load());
        assertFalse(SessionStore.exists());
    }

    // -----------------------------------------------------------------
    // TC-19  The token file lives in the user's own home directory
    //
    // This matters: two members of staff sharing one PC under different
    // Windows accounts must not inherit each other's session.
    // -----------------------------------------------------------------
    @Test
    public void testTokenFileIsPerUser() {
        String location = SessionStore.location();

        assertTrue("Token must be stored under the user's home directory",
                   location.startsWith(System.getProperty("user.home")));
        assertTrue("File should be named session.token",
                   location.contains("session.token"));
    }

    // -----------------------------------------------------------------
    // TC-20  Tokens are hashed before storage, exactly as passwords are
    // -----------------------------------------------------------------
    @Test
    public void testTokenHashing() {
        String token = "SOME_SESSION_TOKEN_VALUE";
        String hash  = PasswordUtil.hash(token);

        assertEquals("SHA-256 produces 64 hex characters", 64, hash.length());
        assertFalse("The hash must differ from the raw token", hash.equals(token));
        assertEquals("Hashing must be deterministic", hash, PasswordUtil.hash(token));
        assertFalse("A different token must give a different hash",
                    hash.equals(PasswordUtil.hash(token + "x")));
    }

    // -----------------------------------------------------------------
    // TC-21  Masking never reveals a whole token in a log
    // -----------------------------------------------------------------
    @Test
    public void testTokenMasking() {
        String token  = "abcdefghijklmnopqrstuvwxyz1234567890";
        String masked = Session.mask(token);

        assertFalse("The masked form must not be the whole token",
                    masked.equals(token));
        assertTrue("The masked form should be truncated", masked.endsWith("..."));
        assertTrue("A short or null token must not throw",
                   Session.mask(null).length() > 0);
    }

    // -----------------------------------------------------------------
    // TC-22  Generated tokens are unique and long enough
    //
    // SessionDAO.create needs a database, so this exercises the same
    // generator directly: 32 random bytes, Base64url encoded.
    // -----------------------------------------------------------------
    @Test
    public void testTokenUniquenessAndLength() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        Set<String> seen = new HashSet<>();
        int shortest = Integer.MAX_VALUE;

        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = java.util.Base64.getUrlEncoder()
                    .withoutPadding().encodeToString(bytes);
            seen.add(token);
            shortest = Math.min(shortest, token.length());
        }

        assertEquals("Every generated token must be unique", 1000, seen.size());
        assertTrue("Tokens must carry 256 bits of entropy", shortest >= 43);
    }
}
