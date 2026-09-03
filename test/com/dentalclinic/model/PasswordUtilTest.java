package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for password and token hashing.
 *
 * The expected digests below were produced by MySQL's SHA2(value, 256). If
 * these tests pass, a password hashed in Java matches one hashed in SQL,
 * which is what lets the seeded accounts sign in.
 *
 * @author [Your Name]
 */
public class PasswordUtilTest {

    @Test public void testDigestIsSixtyFourHexCharacters() {
        assertEquals(64, PasswordUtil.hash("admin123").length());
    }
    @Test public void testDigestIsLowerCaseHex() {
        assertTrue(PasswordUtil.hash("admin123").matches("[0-9a-f]{64}"));
    }
    @Test public void testDigestMatchesMySqlSha2ForAdmin() {
        assertEquals("A Java hash must equal MySQL SHA2(value,256)",
                "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
                PasswordUtil.hash("admin123"));
    }
    @Test public void testHashingIsDeterministic() {
        assertEquals(PasswordUtil.hash("secret1"), PasswordUtil.hash("secret1"));
    }
    @Test public void testDifferentInputsGiveDifferentDigests() {
        assertFalse(PasswordUtil.hash("secret1").equals(PasswordUtil.hash("secret2")));
    }
    @Test public void testSingleCharacterChangeChangesDigestCompletely() {
        String a = PasswordUtil.hash("password1");
        String b = PasswordUtil.hash("password2");
        int same = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                same++;
            }
        }
        assertTrue("An avalanche effect means few characters coincide", same < 20);
    }
    @Test public void testDigestNeverEqualsTheInput() {
        assertFalse(PasswordUtil.hash("admin123").equals("admin123"));
    }
    @Test public void testEmptyStringHashesWithoutError() {
        assertEquals(64, PasswordUtil.hash("").length());
    }
    @Test public void testLongInputHashes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("x");
        }
        assertEquals(64, PasswordUtil.hash(sb.toString()).length());
    }
    @Test public void testUnicodeInputHashes() {
        assertEquals(64, PasswordUtil.hash("පාස්වර්ඩ්").length());
    }
    @Test public void testCaseSensitiveHashing() {
        assertFalse(PasswordUtil.hash("Admin123").equals(PasswordUtil.hash("admin123")));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testNullInputThrowsClearly() {
        PasswordUtil.hash(null);
    }
}
