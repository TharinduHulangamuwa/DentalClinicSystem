package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the local token file - the desktop equivalent of a
 * persistent browser cookie.
 *
 * @author [Your Name]
 */
public class SessionStoreTest {

    @Before
    public void setUp() {
        SessionStore.clear();
    }

    @After
    public void tearDown() {
        SessionStore.clear();
    }

    @Test public void testLoadReturnsNullWhenNothingSaved() {
        assertNull(SessionStore.load());
    }
    @Test public void testExistsFalseWhenNothingSaved() {
        assertFalse(SessionStore.exists());
    }
    @Test public void testSavedTokenIsReadBackExactly() {
        String token = "aVeryLongRandomTokenValue_1234567890";
        SessionStore.save(token);
        assertEquals(token, SessionStore.load());
    }
    @Test public void testExistsTrueAfterSave() {
        SessionStore.save("abc123");
        assertTrue(SessionStore.exists());
    }
    @Test public void testSavingAgainReplacesTheToken() {
        SessionStore.save("FIRST_TOKEN");
        SessionStore.save("SECOND_TOKEN");
        assertEquals("SECOND_TOKEN", SessionStore.load());
    }
    @Test public void testClearRemovesTheToken() {
        SessionStore.save("abc123");
        SessionStore.clear();
        assertNull(SessionStore.load());
        assertFalse(SessionStore.exists());
    }
    @Test public void testClearIsSafeWhenNoFileExists() {
        SessionStore.clear();
        SessionStore.clear();
        assertFalse(SessionStore.exists());
    }
    @Test public void testBase64TokenSurvivesTheRoundTrip() {
        String token = "Xk9mP2vLqR8w-_abcDEF1234567890xyzABCDEFGH";
        SessionStore.save(token);
        assertEquals("URL-safe Base64 characters must not be mangled",
                     token, SessionStore.load());
    }
    @Test public void testFileIsNamedSessionToken() {
        assertTrue(SessionStore.location().contains("session.token"));
    }
    @Test public void testFileLivesInTheUsersHomeDirectory() {
        assertTrue("Two staff on one PC must not share a token",
                   SessionStore.location().startsWith(System.getProperty("user.home")));
    }
    @Test public void testFileIsInsideAHiddenFolder() {
        assertTrue(SessionStore.location().contains(".sunrise-dental"));
    }
}
