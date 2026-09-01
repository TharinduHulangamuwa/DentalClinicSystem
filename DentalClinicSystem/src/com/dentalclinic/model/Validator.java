package com.dentalclinic.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Every input validation rule in the system.
 *
 * DESIGN DECISION worth stating in the report: this class depends on neither
 * Swing nor JDBC. Every method is pure - the same input always gives the same
 * output, with no side effects. That is what makes the JUnit tests possible
 * without starting WampServer or opening a window, and it is why these rules
 * could be written before the user interface existed.
 *
 * Validation logic buried inside a button handler cannot be tested at all.
 *
 * @author [Your Name]
 */
public final class Validator {

    /** Clinic opening hours, used by isWithinClinicHours. */
    public static final int OPENING_HOUR = 8;
    public static final int CLOSING_HOUR = 20;

    private Validator() { }

    /** Rejects null, empty and whitespace-only values. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Sri Lankan contact number: exactly ten digits beginning with zero.
     * Accepts 0771234567 and 0112345678.
     */
    public static boolean isValidContact(String contact) {
        return contact != null && contact.trim().matches("0\\d{9}");
    }

    /**
     * House format for appointment numbers: APT followed by exactly four
     * digits. Accepts APT1001; rejects apt1001, APT101, APT10011.
     */
    public static boolean isValidAppointmentNo(String no) {
        return no != null && no.trim().matches("APT\\d{4}");
    }

    /** A person's name: letters, spaces, full stops, apostrophes, hyphens. */
    public static boolean isValidName(String name) {
        return name != null && name.trim().matches("[A-Za-z][A-Za-z .'-]{1,99}");
    }

    /**
     * ISO date, yyyy-MM-dd.
     * Uses LocalDate rather than a regular expression, so an impossible date
     * such as 2026-02-30 is correctly rejected.
     */
    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }
        try {
            LocalDate.parse(date.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** An appointment may not be booked in the past. */
    public static boolean isNotPastDate(String date) {
        return isValidDate(date)
            && !LocalDate.parse(date.trim()).isBefore(LocalDate.now());
    }

    /** 24-hour clock, HH:mm. Accepts 00:00 to 23:59; rejects 24:00 and 9:30. */
    public static boolean isValidTime(String time) {
        return time != null && time.trim().matches("([01]\\d|2[0-3]):[0-5]\\d");
    }

    /** The clinic is open from 08:00 to 20:00 inclusive. */
    public static boolean isWithinClinicHours(String time) {
        if (!isValidTime(time)) {
            return false;
        }
        int hour = Integer.parseInt(time.trim().substring(0, 2));
        return hour >= OPENING_HOUR && hour <= CLOSING_HOUR;
    }

    /** A consultation fee must parse as a number of zero or more. */
    public static boolean isValidFee(String fee) {
        if (fee == null) {
            return false;
        }
        try {
            return Double.parseDouble(fee.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
