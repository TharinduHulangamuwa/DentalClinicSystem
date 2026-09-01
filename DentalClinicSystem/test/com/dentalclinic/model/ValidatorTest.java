package com.dentalclinic.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for the input validation rules.
 *
 * These tests were written BEFORE the Swing forms existed. Each rule was
 * expressed as a failing test first, then the Validator method was written
 * until the test passed - test-driven development.
 *
 * Every test exercises the boundary cases as well as the happy path,
 * because that is where validation defects actually live.
 *
 * @author [Your Name]
 */
public class ValidatorTest {

    // -----------------------------------------------------------------
    // TC-01  Contact number rule
    // Rule: exactly ten digits, first digit must be 0
    // -----------------------------------------------------------------
    @Test
    public void testContactNumberValidation() {

        // valid Sri Lankan mobile and landline numbers
        assertTrue("Standard mobile number should be accepted",
                   Validator.isValidContact("0771234567"));
        assertTrue("Colombo landline should be accepted",
                   Validator.isValidContact("0112345678"));

        // boundary and invalid cases
        assertFalse("Nine digits without leading zero must be rejected",
                    Validator.isValidContact("771234567"));
        assertFalse("Eleven digits must be rejected",
                    Validator.isValidContact("07712345678"));
        assertFalse("Must start with zero",
                    Validator.isValidContact("1771234567"));
        assertFalse("Letters must be rejected",
                    Validator.isValidContact("07712A4567"));
        assertFalse("Empty string must be rejected",
                    Validator.isValidContact(""));
        assertFalse("Null must be rejected without throwing",
                    Validator.isValidContact(null));
    }

    // -----------------------------------------------------------------
    // TC-02  Appointment number rule
    // Rule: literal APT followed by exactly four digits
    // -----------------------------------------------------------------
    @Test
    public void testAppointmentNumberValidation() {

        assertTrue(Validator.isValidAppointmentNo("APT1001"));
        assertTrue(Validator.isValidAppointmentNo("APT0001"));

        assertFalse("Lower case prefix must be rejected",
                    Validator.isValidAppointmentNo("apt1001"));
        assertFalse("Three digits must be rejected",
                    Validator.isValidAppointmentNo("APT101"));
        assertFalse("Five digits must be rejected",
                    Validator.isValidAppointmentNo("APT10011"));
        assertFalse("Missing prefix must be rejected",
                    Validator.isValidAppointmentNo("1001"));
        assertFalse(Validator.isValidAppointmentNo(""));
        assertFalse(Validator.isValidAppointmentNo(null));
    }

    // -----------------------------------------------------------------
    // TC-03  Appointment time rule
    // Rule: HH:mm on a 24-hour clock, and within clinic hours 08:00-20:00
    // -----------------------------------------------------------------
    @Test
    public void testTimeValidation() {

        // format rule
        assertTrue(Validator.isValidTime("09:30"));
        assertTrue("Midnight is a valid time value", Validator.isValidTime("00:00"));
        assertTrue("One minute to midnight is valid", Validator.isValidTime("23:59"));

        assertFalse("Hour 24 does not exist",     Validator.isValidTime("24:00"));
        assertFalse("Minute 60 does not exist",   Validator.isValidTime("09:60"));
        assertFalse("Hour must be zero padded",   Validator.isValidTime("9:30"));
        assertFalse("Colon separator is required", Validator.isValidTime("0930"));
        assertFalse(Validator.isValidTime(null));

        // clinic opening hours rule - boundaries matter most
        assertTrue("Opening time is inclusive", Validator.isWithinClinicHours("08:00"));
        assertTrue("Closing time is inclusive", Validator.isWithinClinicHours("20:00"));
        assertFalse("Before opening", Validator.isWithinClinicHours("07:59"));
        assertFalse("After closing",  Validator.isWithinClinicHours("21:00"));
    }

    // -----------------------------------------------------------------
    // TC-04  Calendar-aware date rule
    // Rule: yyyy-MM-dd, and the date must genuinely exist
    // -----------------------------------------------------------------
    @Test
    public void testDateValidation() {

        assertTrue(Validator.isValidDate("2026-06-15"));
        assertTrue("2028 is a leap year", Validator.isValidDate("2028-02-29"));

        assertFalse("30 February never exists - a simple regex would wrongly accept this",
                    Validator.isValidDate("2026-02-30"));
        assertFalse("Month 13 does not exist", Validator.isValidDate("2026-13-01"));
        assertFalse("Wrong field order",       Validator.isValidDate("15-06-2026"));
        assertFalse("Wrong separator",         Validator.isValidDate("2026/06/15"));
        assertFalse(Validator.isValidDate(null));
    }

    // -----------------------------------------------------------------
    // TC-23  Username rule for new staff accounts
    // -----------------------------------------------------------------
    @Test
    public void testUsernameValidation() {

        assertTrue(Validator.isValidUsername("nimali"));
        assertTrue(Validator.isValidUsername("dr.perera"));
        assertTrue(Validator.isValidUsername("staff_01"));

        assertFalse("Must be at least four characters",
                    Validator.isValidUsername("abc"));
        assertFalse("Must start with a letter",
                    Validator.isValidUsername("1nimali"));
        assertFalse("Spaces are not allowed",
                    Validator.isValidUsername("nimali perera"));
        assertFalse("Symbols are not allowed",
                    Validator.isValidUsername("nimali@clinic"));
        assertFalse("Twenty-one characters is too long",
                    Validator.isValidUsername("abcdefghijklmnopqrstu"));
        assertFalse(Validator.isValidUsername(""));
        assertFalse(Validator.isValidUsername(null));
    }

    // -----------------------------------------------------------------
    // TC-24  Password rule for new staff accounts
    // -----------------------------------------------------------------
    @Test
    public void testPasswordStrength() {

        assertTrue(Validator.isStrongPassword("admin123"));
        assertTrue(Validator.isStrongPassword("Clinic2026"));

        assertFalse("Seven characters is too short",
                    Validator.isStrongPassword("abc1234"));
        assertFalse("Letters only must be rejected",
                    Validator.isStrongPassword("password"));
        assertFalse("Digits only must be rejected",
                    Validator.isStrongPassword("12345678"));
        assertFalse(Validator.isStrongPassword(""));
        assertFalse(Validator.isStrongPassword(null));
    }
}
