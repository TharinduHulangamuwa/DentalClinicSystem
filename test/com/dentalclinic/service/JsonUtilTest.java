package com.dentalclinic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.User;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for JSON serialisation and parsing.
 *
 * This is the wire format between the two processes, so a defect here breaks
 * the whole distributed system. The round-trip tests matter most: whatever
 * the client sends must arrive at the server unchanged.
 *
 * @author [Your Name]
 */
public class JsonUtilTest {

    private Appointment appointment;

    @Before
    public void setUp() {
        appointment = new Appointment("APT1001", "Kamal Silva",
                "12 Galle Road, Colombo 03", "0771234567",
                "Dr. Fernando", "Filling", "2026-09-01", "10:30");
    }

    // ---------------- escaping ----------------

    @Test public void testEscapeLeavesPlainTextAlone() {
        assertEquals("Kamal Silva", JsonUtil.escape("Kamal Silva"));
    }
    @Test public void testEscapeHandlesDoubleQuote() {
        assertEquals("say \\\"hi\\\"", JsonUtil.escape("say \"hi\""));
    }
    @Test public void testEscapeHandlesBackslash() {
        assertEquals("C:\\\\Users", JsonUtil.escape("C:\\Users"));
    }
    @Test public void testEscapeHandlesNewline() {
        assertEquals("a\\nb", JsonUtil.escape("a\nb"));
    }
    @Test public void testEscapeHandlesTab() {
        assertEquals("a\\tb", JsonUtil.escape("a\tb"));
    }
    @Test public void testEscapeHandlesNullAsEmpty() {
        assertEquals("", JsonUtil.escape(null));
    }

    // ---------------- writing ----------------

    @Test public void testAppointmentJsonIsAnObject() {
        String json = JsonUtil.toJson(appointment);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }
    @Test public void testAppointmentJsonContainsEveryField() {
        String json = JsonUtil.toJson(appointment);
        assertTrue(json.contains("\"appointmentNo\":\"APT1001\""));
        assertTrue(json.contains("\"patientName\":\"Kamal Silva\""));
        assertTrue(json.contains("\"contactNo\":\"0771234567\""));
        assertTrue(json.contains("\"appointmentTime\":\"10:30\""));
    }
    @Test public void testUserJsonNeverContainsAPassword() {
        String json = JsonUtil.toJson(new User(1, "admin", "Administrator", "ADMIN"));
        assertFalse("A password must never cross the wire in a user object",
                    json.toLowerCase().contains("password"));
    }
    @Test public void testUserJsonContainsIdentityFields() {
        String json = JsonUtil.toJson(new User(1, "admin", "Administrator", "ADMIN"));
        assertTrue(json.contains("\"username\":\"admin\""));
        assertTrue(json.contains("\"role\":\"ADMIN\""));
    }
    @Test public void testMapJsonLeavesNumbersUnquoted() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", 4);
        assertTrue(JsonUtil.toJson(m).contains("\"count\":4"));
    }
    @Test public void testMapJsonLeavesBooleansUnquoted() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taken", true);
        assertEquals("{\"taken\":true}", JsonUtil.toJson(m));
    }
    @Test public void testMapJsonQuotesStrings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        assertEquals("{\"status\":\"ok\"}", JsonUtil.toJson(m));
    }
    @Test public void testMapJsonWritesNullAsEmptyString() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("address", null);
        assertEquals("{\"address\":\"\"}", JsonUtil.toJson(m));
    }
    @Test public void testMessageBuildsASingleField() {
        assertEquals("{\"error\":\"not found\"}",
                     JsonUtil.message("error", "not found"));
    }
    @Test public void testMessageEscapesItsValue() {
        assertTrue(JsonUtil.message("error", "say \"no\"").contains("\\\""));
    }
    @Test public void testAppointmentListBecomesAnArray() {
        String json = JsonUtil.appointmentsToJson(
                Arrays.asList(appointment, appointment));
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }
    @Test public void testEmptyListBecomesEmptyArray() {
        assertEquals("[]", JsonUtil.appointmentsToJson(
                new java.util.ArrayList<Appointment>()));
    }
    @Test public void testRowsToJsonMapsColumnsToKeys() {
        List<String[]> rows = java.util.Collections.singletonList(
                new String[]{"2026-09-01", "Dr. Silva"});
        String json = JsonUtil.rowsToJson(new String[]{"date", "dentist"}, rows);
        assertTrue(json.contains("\"date\":\"2026-09-01\""));
        assertTrue(json.contains("\"dentist\":\"Dr. Silva\""));
    }
    @Test public void testStringsToJsonQuotesEachEntry() {
        assertEquals("[\"a\",\"b\"]", JsonUtil.stringsToJson(Arrays.asList("a", "b")));
    }

    // ---------------- parsing ----------------

    @Test public void testParseObjectReadsStringValues() {
        Map<String, String> m = JsonUtil.parseObject("{\"a\":\"1\",\"b\":\"two\"}");
        assertEquals("1", m.get("a"));
        assertEquals("two", m.get("b"));
    }
    @Test public void testParseObjectReadsUnquotedNumbers() {
        assertEquals("42", JsonUtil.parseObject("{\"n\":42}").get("n"));
    }
    @Test public void testParseObjectReadsBooleans() {
        assertEquals("true", JsonUtil.parseObject("{\"taken\":true}").get("taken"));
    }
    @Test public void testParseObjectHandlesEscapedQuotes() {
        Map<String, String> m =
                JsonUtil.parseObject("{\"msg\":\"say \\\"hi\\\"\"}");
        assertEquals("say \"hi\"", m.get("msg"));
    }
    @Test public void testParseObjectHandlesCommasInsideValues() {
        Map<String, String> m =
                JsonUtil.parseObject("{\"address\":\"12 Galle Road, Colombo 03\"}");
        assertEquals("A comma inside a value must not split the field",
                     "12 Galle Road, Colombo 03", m.get("address"));
    }
    @Test public void testParseObjectHandlesWhitespace() {
        assertEquals("1", JsonUtil.parseObject("{ \"a\" : \"1\" }").get("a"));
    }
    @Test public void testParseObjectReturnsEmptyForNull() {
        assertTrue(JsonUtil.parseObject(null).isEmpty());
    }
    @Test public void testParseObjectReturnsEmptyForMalformedInput() {
        assertTrue(JsonUtil.parseObject("not json").isEmpty());
    }
    @Test public void testParseArrayCountsObjects() {
        assertEquals(2, JsonUtil.parseArray("[{\"a\":\"1\"},{\"a\":\"2\"}]").size());
    }
    @Test public void testParseArrayReadsEachObject() {
        List<Map<String, String>> list =
                JsonUtil.parseArray("[{\"a\":\"1\"},{\"a\":\"2\"}]");
        assertEquals("1", list.get(0).get("a"));
        assertEquals("2", list.get(1).get("a"));
    }
    @Test public void testParseArrayHandlesBracesInsideStrings() {
        List<Map<String, String>> list =
                JsonUtil.parseArray("[{\"a\":\"a}b{c\"}]");
        assertEquals("A brace inside a string must not end the object",
                     "a}b{c", list.get(0).get("a"));
    }
    @Test public void testParseArrayReturnsEmptyForEmptyArray() {
        assertTrue(JsonUtil.parseArray("[]").isEmpty());
    }
    @Test public void testParseArrayReturnsEmptyForNull() {
        assertTrue(JsonUtil.parseArray(null).isEmpty());
    }
    @Test public void testParseStringArrayReadsPlainStrings() {
        List<String> list = JsonUtil.parseStringArray("[\"one\",\"two\"]");
        assertEquals(2, list.size());
        assertEquals("one", list.get(0));
    }
    @Test public void testParseStringArrayHandlesEscapedQuotes() {
        List<String> list = JsonUtil.parseStringArray("[\"say \\\"hi\\\"\"]");
        assertEquals("say \"hi\"", list.get(0));
    }
    @Test public void testParseStringArrayHandlesCommasInsideStrings() {
        List<String> list = JsonUtil.parseStringArray("[\"a,b\",\"c\"]");
        assertEquals("A naive split would wrongly find three entries",
                     2, list.size());
        assertEquals("a,b", list.get(0));
    }
    @Test public void testParseStringArrayReturnsEmptyForNull() {
        assertTrue(JsonUtil.parseStringArray(null).isEmpty());
    }

    // ---------------- round trips ----------------

    @Test public void testAppointmentSurvivesARoundTrip() {
        Appointment back = JsonUtil.toAppointment(
                JsonUtil.parseObject(JsonUtil.toJson(appointment)));
        assertEquals(appointment.getAppointmentNo(), back.getAppointmentNo());
        assertEquals(appointment.getPatientName(),   back.getPatientName());
        assertEquals(appointment.getAddress(),       back.getAddress());
        assertEquals(appointment.getContactNo(),     back.getContactNo());
        assertEquals(appointment.getDentistName(),   back.getDentistName());
        assertEquals(appointment.getTreatmentType(), back.getTreatmentType());
        assertEquals(appointment.getAppointmentDate(), back.getAppointmentDate());
        assertEquals(appointment.getAppointmentTime(), back.getAppointmentTime());
    }
    @Test public void testQuotesInAPatientNameSurviveARoundTrip() {
        appointment.setPatientName("Kamal \"KJ\" Silva");
        Appointment back = JsonUtil.toAppointment(
                JsonUtil.parseObject(JsonUtil.toJson(appointment)));
        assertEquals("Kamal \"KJ\" Silva", back.getPatientName());
    }
    @Test public void testAmpersandInATreatmentSurvivesARoundTrip() {
        appointment.setTreatmentType("Scaling & Polishing");
        Appointment back = JsonUtil.toAppointment(
                JsonUtil.parseObject(JsonUtil.toJson(appointment)));
        assertEquals("Scaling & Polishing", back.getTreatmentType());
    }
    @Test public void testUserSurvivesARoundTrip() {
        User original = new User(7, "nimali", "Nimali Perera", "STAFF");
        User back = JsonUtil.toUser(JsonUtil.parseObject(JsonUtil.toJson(original)));
        assertEquals(7, back.getUserId());
        assertEquals("nimali", back.getUsername());
        assertEquals("Nimali Perera", back.getFullName());
        assertEquals("STAFF", back.getRole());
    }
    @Test public void testUserWithMalformedIdDefaultsToZero() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("userId", "not-a-number");
        m.put("username", "x");
        assertEquals("A bad id must not throw", 0, JsonUtil.toUser(m).getUserId());
    }
    @Test public void testAppointmentListSurvivesARoundTrip() {
        String json = JsonUtil.appointmentsToJson(
                Arrays.asList(appointment, appointment));
        assertEquals(2, JsonUtil.parseArray(json).size());
    }
}
