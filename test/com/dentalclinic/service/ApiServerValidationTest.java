package com.dentalclinic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.dentalclinic.model.Appointment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the SERVER-SIDE validation and helpers.
 *
 * These matter more than they first appear. The Swing client validates too,
 * but a server must never trust a client: another client could be written
 * tomorrow, or a request could be sent by hand with curl. These tests prove
 * the server refuses bad data on its own.
 *
 * @author [Your Name]
 */
public class ApiServerValidationTest {

    private Appointment valid;

    @Before
    public void setUp() {
        valid = new Appointment("APT1001", "Kamal Silva", "12 Galle Road",
                "0771234567", "Dr. Fernando", "Filling",
                java.time.LocalDate.now().plusDays(1).toString(), "10:30");
    }

    // ---------------- appointment validation ----------------

    @Test public void testValidAppointmentIsAccepted() {
        assertNull("A complete valid appointment returns no error",
                   ApiServer.validate(valid));
    }
    @Test public void testServerRejectsBadAppointmentNumber() {
        valid.setAppointmentNo("XYZ");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsMissingPatientName() {
        valid.setPatientName("");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsNumericPatientName() {
        valid.setPatientName("12345");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsBadContactNumber() {
        valid.setContactNo("123");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsMissingDentist() {
        valid.setDentistName("");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsBadDateFormat() {
        valid.setAppointmentDate("15/06/2026");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsImpossibleDate() {
        valid.setAppointmentDate("2026-02-30");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsBadTimeFormat() {
        valid.setAppointmentTime("9am");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testServerRejectsTimeOutsideClinicHours() {
        valid.setAppointmentTime("22:00");
        assertNotNull(ApiServer.validate(valid));
    }
    @Test public void testErrorMessageNamesTheProblemField() {
        valid.setContactNo("123");
        assertTrue(ApiServer.validate(valid).toLowerCase().contains("contact"));
    }

    // ---------------- staff account validation ----------------

    private Map<String, String> account(String user, String name,
                                        String password, String role) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("username", user);
        m.put("fullName", name);
        m.put("password", password);
        m.put("role", role);
        return m;
    }

    @Test public void testValidAccountIsAccepted() {
        assertNull(ApiServer.validateUser(
                account("nimali", "Nimali Perera", "clinic123", "STAFF")));
    }
    @Test public void testValidAdminAccountIsAccepted() {
        assertNull(ApiServer.validateUser(
                account("kasun", "Kasun Rathnayake", "clinic123", "ADMIN")));
    }
    @Test public void testServerRejectsShortUsername() {
        assertNotNull(ApiServer.validateUser(
                account("ab", "Nimali Perera", "clinic123", "STAFF")));
    }
    @Test public void testServerRejectsUsernameStartingWithADigit() {
        assertNotNull(ApiServer.validateUser(
                account("1nimali", "Nimali Perera", "clinic123", "STAFF")));
    }
    @Test public void testServerRejectsBadFullName() {
        assertNotNull(ApiServer.validateUser(
                account("nimali", "N1mali", "clinic123", "STAFF")));
    }
    @Test public void testServerRejectsWeakPassword() {
        assertNotNull(ApiServer.validateUser(
                account("nimali", "Nimali Perera", "abc", "STAFF")));
    }
    @Test public void testServerRejectsPasswordWithoutADigit() {
        assertNotNull(ApiServer.validateUser(
                account("nimali", "Nimali Perera", "password", "STAFF")));
    }
    @Test public void testServerRejectsUnknownRole() {
        assertNotNull("Only ADMIN and STAFF exist", ApiServer.validateUser(
                account("nimali", "Nimali Perera", "clinic123", "MANAGER")));
    }
    @Test public void testServerRejectsMissingRole() {
        assertNotNull(ApiServer.validateUser(
                account("nimali", "Nimali Perera", "clinic123", null)));
    }

    // ---------------- query string parsing ----------------

    @Test public void testQueryParsesASinglePair() {
        assertEquals("Dr. Silva",
                ApiServer.parseQuery("dentist=Dr.+Silva").get("dentist"));
    }
    @Test public void testQueryParsesSeveralPairs() {
        Map<String, String> q =
                ApiServer.parseQuery("dentist=Dr.+Silva&date=2026-09-01&time=14%3A00");
        assertEquals("Dr. Silva",  q.get("dentist"));
        assertEquals("2026-09-01", q.get("date"));
        assertEquals("14:00", q.get("time"));
    }
    @Test public void testQueryDecodesPercentEncoding() {
        assertEquals("Scaling & Polishing",
                ApiServer.parseQuery("t=Scaling+%26+Polishing").get("t"));
    }
    @Test public void testQueryReturnsEmptyForNull() {
        assertTrue(ApiServer.parseQuery(null).isEmpty());
    }
    @Test public void testQueryReturnsEmptyForEmptyString() {
        assertTrue(ApiServer.parseQuery("").isEmpty());
    }
    @Test public void testQuerySkipsMalformedPairs() {
        Map<String, String> q = ApiServer.parseQuery("good=1&broken&also=2");
        assertEquals("1", q.get("good"));
        assertEquals("2", q.get("also"));
        assertEquals("A malformed pair is skipped, not fatal", 2, q.size());
    }
}
