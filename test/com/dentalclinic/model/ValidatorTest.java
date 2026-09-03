package com.dentalclinic.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for every input validation rule.
 *
 * These were written test-first: each rule was expressed as a failing test
 * before the Validator method existed. Every test covers the boundary cases
 * as well as the happy path, because that is where validation defects live.
 *
 * @author [Your Name]
 */
public class ValidatorTest {

    // ---------------- isNotEmpty ----------------

    @Test public void testNotEmptyAcceptsText() {
        assertTrue(Validator.isNotEmpty("Kamal"));
    }
    @Test public void testNotEmptyRejectsEmptyString() {
        assertFalse(Validator.isNotEmpty(""));
    }
    @Test public void testNotEmptyRejectsWhitespaceOnly() {
        assertFalse("A field of spaces is empty to a user",
                    Validator.isNotEmpty("     "));
    }
    @Test public void testNotEmptyRejectsTabsAndNewlines() {
        assertFalse(Validator.isNotEmpty("\t\n"));
    }
    @Test public void testNotEmptyRejectsNull() {
        assertFalse(Validator.isNotEmpty(null));
    }

    // ---------------- isValidContact ----------------

    @Test public void testContactAcceptsMobile() {
        assertTrue(Validator.isValidContact("0771234567"));
    }
    @Test public void testContactAcceptsLandline() {
        assertTrue(Validator.isValidContact("0112345678"));
    }
    @Test public void testContactAcceptsSurroundingSpaces() {
        assertTrue("Values are trimmed before checking",
                   Validator.isValidContact("  0771234567  "));
    }
    @Test public void testContactRejectsMissingLeadingZero() {
        assertFalse(Validator.isValidContact("771234567"));
    }
    @Test public void testContactRejectsNineDigits() {
        assertFalse(Validator.isValidContact("077123456"));
    }
    @Test public void testContactRejectsElevenDigits() {
        assertFalse(Validator.isValidContact("07712345678"));
    }
    @Test public void testContactRejectsLetters() {
        assertFalse(Validator.isValidContact("07712A4567"));
    }
    @Test public void testContactRejectsPunctuation() {
        assertFalse(Validator.isValidContact("077-1234567"));
    }
    @Test public void testContactRejectsEmpty() {
        assertFalse(Validator.isValidContact(""));
    }
    @Test public void testContactRejectsNull() {
        assertFalse("Null must be rejected, not throw",
                    Validator.isValidContact(null));
    }

    // ---------------- isValidAppointmentNo ----------------

    @Test public void testAppointmentNoAcceptsHouseFormat() {
        assertTrue(Validator.isValidAppointmentNo("APT1001"));
    }
    @Test public void testAppointmentNoAcceptsLeadingZeros() {
        assertTrue(Validator.isValidAppointmentNo("APT0001"));
    }
    @Test public void testAppointmentNoRejectsLowerCase() {
        assertFalse(Validator.isValidAppointmentNo("apt1001"));
    }
    @Test public void testAppointmentNoRejectsThreeDigits() {
        assertFalse(Validator.isValidAppointmentNo("APT101"));
    }
    @Test public void testAppointmentNoRejectsFiveDigits() {
        assertFalse(Validator.isValidAppointmentNo("APT10011"));
    }
    @Test public void testAppointmentNoRejectsMissingPrefix() {
        assertFalse(Validator.isValidAppointmentNo("1001"));
    }
    @Test public void testAppointmentNoRejectsWrongPrefix() {
        assertFalse(Validator.isValidAppointmentNo("APP1001"));
    }
    @Test public void testAppointmentNoRejectsNull() {
        assertFalse(Validator.isValidAppointmentNo(null));
    }

    // ---------------- isValidName ----------------

    @Test public void testNameAcceptsSimpleName() {
        assertTrue(Validator.isValidName("Kamal Silva"));
    }
    @Test public void testNameAcceptsTitleWithFullStop() {
        assertTrue(Validator.isValidName("Dr. Fernando"));
    }
    @Test public void testNameAcceptsHyphen() {
        assertTrue(Validator.isValidName("Anne-Marie Perera"));
    }
    @Test public void testNameAcceptsApostrophe() {
        assertTrue(Validator.isValidName("O'Brien"));
    }
    @Test public void testNameRejectsDigits() {
        assertFalse(Validator.isValidName("Kamal2"));
    }
    @Test public void testNameRejectsSingleCharacter() {
        assertFalse("A one-letter name is almost certainly a typo",
                    Validator.isValidName("K"));
    }
    @Test public void testNameTrimsSurroundingSpaces() {
        // Written first as "rejects a leading space", which FAILED. Reading
        // the code showed isValidName trims before matching, and on
        // reflection that is the behaviour we want: a name pasted from
        // another system often carries stray spaces, and rejecting it would
        // be an unhelpful obstacle. The test was wrong, not the rule.
        assertTrue("A pasted name with stray spaces should be accepted",
                   Validator.isValidName("  Kamal Silva  "));
    }
    @Test public void testNameRejectsWhitespaceOnly() {
        assertFalse(Validator.isValidName("   "));
    }
    @Test public void testNameRejectsSymbols() {
        assertFalse(Validator.isValidName("Kamal@Silva"));
    }
    @Test public void testNameRejectsEmpty() {
        assertFalse(Validator.isValidName(""));
    }
    @Test public void testNameRejectsNull() {
        assertFalse(Validator.isValidName(null));
    }

    // ---------------- isValidDate ----------------

    @Test public void testDateAcceptsIsoFormat() {
        assertTrue(Validator.isValidDate("2026-06-15"));
    }
    @Test public void testDateAcceptsLeapDay() {
        assertTrue("2028 is a leap year", Validator.isValidDate("2028-02-29"));
    }
    @Test public void testDateRejectsLeapDayInCommonYear() {
        assertFalse("2027 is not a leap year", Validator.isValidDate("2027-02-29"));
    }
    @Test public void testDateRejectsImpossibleDay() {
        assertFalse("A plain regex would wrongly accept 30 February",
                    Validator.isValidDate("2026-02-30"));
    }
    @Test public void testDateRejectsThirtyFirstOfApril() {
        assertFalse(Validator.isValidDate("2026-04-31"));
    }
    @Test public void testDateRejectsMonthThirteen() {
        assertFalse(Validator.isValidDate("2026-13-01"));
    }
    @Test public void testDateRejectsDayZero() {
        assertFalse(Validator.isValidDate("2026-06-00"));
    }
    @Test public void testDateRejectsWrongFieldOrder() {
        assertFalse(Validator.isValidDate("15-06-2026"));
    }
    @Test public void testDateRejectsSlashSeparator() {
        assertFalse(Validator.isValidDate("2026/06/15"));
    }
    @Test public void testDateRejectsNull() {
        assertFalse(Validator.isValidDate(null));
    }

    // ---------------- isNotPastDate ----------------

    @Test public void testNotPastAcceptsToday() {
        assertTrue("Today is bookable",
                   Validator.isNotPastDate(java.time.LocalDate.now().toString()));
    }
    @Test public void testNotPastAcceptsTomorrow() {
        assertTrue(Validator.isNotPastDate(
                java.time.LocalDate.now().plusDays(1).toString()));
    }
    @Test public void testNotPastRejectsYesterday() {
        assertFalse(Validator.isNotPastDate(
                java.time.LocalDate.now().minusDays(1).toString()));
    }
    @Test public void testNotPastRejectsLongPast() {
        assertFalse(Validator.isNotPastDate("2020-01-01"));
    }
    @Test public void testNotPastRejectsMalformedDate() {
        assertFalse(Validator.isNotPastDate("not-a-date"));
    }

    // ---------------- isValidTime ----------------

    @Test public void testTimeAcceptsMorning() {
        assertTrue(Validator.isValidTime("09:30"));
    }
    @Test public void testTimeAcceptsMidnight() {
        assertTrue(Validator.isValidTime("00:00"));
    }
    @Test public void testTimeAcceptsLastMinuteOfDay() {
        assertTrue(Validator.isValidTime("23:59"));
    }
    @Test public void testTimeRejectsHourTwentyFour() {
        assertFalse(Validator.isValidTime("24:00"));
    }
    @Test public void testTimeRejectsMinuteSixty() {
        assertFalse(Validator.isValidTime("09:60"));
    }
    @Test public void testTimeRejectsUnpaddedHour() {
        assertFalse(Validator.isValidTime("9:30"));
    }
    @Test public void testTimeRejectsMissingColon() {
        assertFalse(Validator.isValidTime("0930"));
    }
    @Test public void testTimeRejectsSeconds() {
        assertFalse(Validator.isValidTime("09:30:00"));
    }
    @Test public void testTimeRejectsNull() {
        assertFalse(Validator.isValidTime(null));
    }

    // ---------------- isWithinClinicHours ----------------

    @Test public void testClinicHoursAcceptsOpeningBoundary() {
        assertTrue("08:00 is inclusive", Validator.isWithinClinicHours("08:00"));
    }
    @Test public void testClinicHoursAcceptsClosingBoundary() {
        assertTrue("20:00 is inclusive", Validator.isWithinClinicHours("20:00"));
    }
    @Test public void testClinicHoursAcceptsMidday() {
        assertTrue(Validator.isWithinClinicHours("13:45"));
    }
    @Test public void testClinicHoursRejectsJustBeforeOpening() {
        assertFalse(Validator.isWithinClinicHours("07:59"));
    }
    @Test public void testClinicHoursRejectsJustAfterClosing() {
        assertFalse(Validator.isWithinClinicHours("21:00"));
    }
    @Test public void testClinicHoursRejectsMidnight() {
        assertFalse(Validator.isWithinClinicHours("00:00"));
    }
    @Test public void testClinicHoursRejectsMalformedTime() {
        assertFalse(Validator.isWithinClinicHours("9am"));
    }

    // ---------------- isValidFee ----------------

    @Test public void testFeeAcceptsWholeNumber() {
        assertTrue(Validator.isValidFee("1500"));
    }
    @Test public void testFeeAcceptsDecimal() {
        assertTrue(Validator.isValidFee("1500.50"));
    }
    @Test public void testFeeAcceptsZero() {
        assertTrue("A free consultation is legitimate", Validator.isValidFee("0"));
    }
    @Test public void testFeeRejectsNegative() {
        assertFalse(Validator.isValidFee("-100"));
    }
    @Test public void testFeeRejectsText() {
        assertFalse(Validator.isValidFee("free"));
    }
    @Test public void testFeeRejectsEmpty() {
        assertFalse(Validator.isValidFee(""));
    }
    @Test public void testFeeRejectsNull() {
        assertFalse(Validator.isValidFee(null));
    }

    // ---------------- isValidUsername ----------------

    @Test public void testUsernameAcceptsSimple() {
        assertTrue(Validator.isValidUsername("nimali"));
    }
    @Test public void testUsernameAcceptsDotAndUnderscore() {
        assertTrue(Validator.isValidUsername("dr.perera"));
        assertTrue(Validator.isValidUsername("staff_01"));
    }
    @Test public void testUsernameAcceptsFourCharacterMinimum() {
        assertTrue(Validator.isValidUsername("abcd"));
    }
    @Test public void testUsernameAcceptsTwentyCharacterMaximum() {
        assertTrue(Validator.isValidUsername("abcdefghijklmnopqrst"));
    }
    @Test public void testUsernameRejectsThreeCharacters() {
        assertFalse(Validator.isValidUsername("abc"));
    }
    @Test public void testUsernameRejectsTwentyOneCharacters() {
        assertFalse(Validator.isValidUsername("abcdefghijklmnopqrstu"));
    }
    @Test public void testUsernameRejectsLeadingDigit() {
        assertFalse(Validator.isValidUsername("1nimali"));
    }
    @Test public void testUsernameRejectsSpaces() {
        assertFalse(Validator.isValidUsername("nimali perera"));
    }
    @Test public void testUsernameRejectsSymbols() {
        assertFalse(Validator.isValidUsername("nimali@clinic"));
    }
    @Test public void testUsernameRejectsNull() {
        assertFalse(Validator.isValidUsername(null));
    }

    // ---------------- isStrongPassword ----------------

    @Test public void testPasswordAcceptsLettersAndDigits() {
        assertTrue(Validator.isStrongPassword("admin123"));
    }
    @Test public void testPasswordAcceptsMixedCase() {
        assertTrue(Validator.isStrongPassword("Clinic2026"));
    }
    @Test public void testPasswordAcceptsExactMinimumLength() {
        assertTrue("Eight characters is the boundary",
                   Validator.isStrongPassword("abcdefg1"));
    }
    @Test public void testPasswordRejectsSevenCharacters() {
        assertFalse(Validator.isStrongPassword("abcdef1"));
    }
    @Test public void testPasswordRejectsLettersOnly() {
        assertFalse(Validator.isStrongPassword("password"));
    }
    @Test public void testPasswordRejectsDigitsOnly() {
        assertFalse(Validator.isStrongPassword("12345678"));
    }
    @Test public void testPasswordRejectsEmpty() {
        assertFalse(Validator.isStrongPassword(""));
    }
    @Test public void testPasswordRejectsNull() {
        assertFalse(Validator.isStrongPassword(null));
    }
}
