package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the in-memory session.
 *
 * A Swing application has no browser and therefore no JSESSIONID cookie.
 * Session holds the same state explicitly: who is signed in, since when, and
 * how long is left before an idle timeout.
 *
 * @author [Your Name]
 */
public class SessionTest {

    private User user;

    @Before
    public void setUp() {
        user = new User(1, "admin", "Clinic Administrator", "ADMIN");
        Session.end();
    }

    @After
    public void tearDown() {
        Session.end();
    }

    @Test public void testNoSessionBeforeSignIn() {
        assertFalse(Session.isActive());
        assertNull(Session.getInstance());
    }
    @Test public void testStartMakesSessionActive() {
        Session.start("TOKEN_ABC123", user, false);
        assertTrue(Session.isActive());
        assertNotNull(Session.getInstance());
    }
    @Test public void testStartRecordsTheUser() {
        Session.start("TOKEN_ABC123", user, false);
        assertEquals("admin", Session.getInstance().getUser().getUsername());
    }
    @Test public void testStartRecordsTheToken() {
        Session.start("TOKEN_ABC123", user, false);
        assertEquals("TOKEN_ABC123", Session.getInstance().getToken());
    }
    @Test public void testRememberFlagFalseByDefault() {
        Session.start("T", user, false);
        assertFalse(Session.getInstance().isRemembered());
    }
    @Test public void testRememberFlagStoredWhenTrue() {
        Session.start("T", user, true);
        assertTrue(Session.getInstance().isRemembered());
    }
    @Test public void testCountdownStartsAtFullTimeout() {
        Session.start("T", user, false);
        assertEquals(Session.TIMEOUT_MINUTES,
                     Session.getInstance().getMinutesRemaining());
    }
    @Test public void testFreshSessionHasNotTimedOut() {
        Session.start("T", user, false);
        assertFalse(Session.getInstance().hasTimedOut());
    }
    @Test public void testFreshSessionIsNotExpiringSoon() {
        Session.start("T", user, false);
        assertFalse(Session.getInstance().isExpiringSoon());
    }
    @Test public void testIdleMinutesStartAtZero() {
        Session.start("T", user, false);
        assertEquals(0, Session.getInstance().getIdleMinutes());
    }
    @Test public void testTouchKeepsIdleAtZero() {
        Session.start("T", user, false);
        Session.getInstance().touch();
        assertEquals(0, Session.getInstance().getIdleMinutes());
    }
    @Test public void testLoginTimeIsFormattedAsHoursAndMinutes() {
        Session.start("T", user, false);
        assertTrue(Session.getInstance().getLoginTimeText().matches("\\d{2}:\\d{2}"));
    }
    @Test public void testDurationStartsAtZeroMinutes() {
        Session.start("T", user, false);
        assertEquals(0, Session.getInstance().getDurationMinutes());
    }
    @Test public void testEndClearsTheInstance() {
        Session.start("T", user, false);
        Session.end();
        assertNull(Session.getInstance());
        assertFalse(Session.isActive());
    }
    @Test public void testStartingAgainReplacesThePreviousSession() {
        Session.start("FIRST", user, false);
        Session.start("SECOND", user, true);
        assertEquals("SECOND", Session.getInstance().getToken());
        assertTrue(Session.getInstance().isRemembered());
    }
    @Test public void testEndIsSafeWhenNoSessionExists() {
        Session.end();
        Session.end();
        assertFalse(Session.isActive());
    }
    @Test public void testMaskTruncatesTheToken() {
        String token = "abcdefghijklmnopqrstuvwxyz1234567890";
        String masked = Session.mask(token);
        assertFalse(masked.equals(token));
        assertTrue(masked.endsWith("..."));
    }
    @Test public void testMaskKeepsAShortPrefix() {
        assertTrue(Session.mask("abcdefghijklmnop").startsWith("abcdefghij"));
    }
    @Test public void testMaskHandlesNullSafely() {
        assertEquals("????", Session.mask(null));
    }
    @Test public void testMaskHandlesShortTokenSafely() {
        assertEquals("????", Session.mask("abc"));
    }
    @Test public void testTimeoutConstantIsThirtyMinutes() {
        assertEquals(30, Session.TIMEOUT_MINUTES);
    }
    @Test public void testRememberPeriodIsSevenDays() {
        assertEquals(7, Session.REMEMBER_DAYS);
    }
    @Test public void testWarningThresholdIsBelowTimeout() {
        assertTrue(Session.WARN_MINUTES < Session.TIMEOUT_MINUTES);
    }
}
