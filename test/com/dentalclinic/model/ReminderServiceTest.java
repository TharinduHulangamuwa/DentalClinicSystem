package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the patient reminder feature.
 *
 * A stub DAO returns fixed rows, so these run with no database and no server.
 * That is possible only because ReminderService takes an AppointmentDAO in
 * its constructor rather than creating one internally - a small design choice
 * made specifically for testability.
 *
 * @author [Your Name]
 */
public class ReminderServiceTest {

    /** Returns whatever rows the test asks for, instead of querying MySQL. */
    private static class StubDAO extends AppointmentDAO {
        private final List<Appointment> rows;
        StubDAO(List<Appointment> rows) { this.rows = rows; }
        @Override public List<Appointment> findByDate(String date) { return rows; }
    }

    private ReminderService service;
    private Appointment appointment;

    @Before
    public void setUp() {
        service = new ReminderService(new StubDAO(new ArrayList<Appointment>()));
        appointment = new Appointment("APT1001", "Kamal Silva",
                "12 Galle Road", "0771234567",
                "Dr. Fernando", "Filling", "2026-09-01", "10:30");
    }

    @Test public void testMessageNamesTheClinic() {
        assertTrue(service.buildSmsMessage(appointment).contains("SUNRISE DENTAL"));
    }
    @Test public void testMessageNamesThePatient() {
        assertTrue(service.buildSmsMessage(appointment).contains("Kamal Silva"));
    }
    @Test public void testMessageNamesTheDentist() {
        assertTrue(service.buildSmsMessage(appointment).contains("Dr. Fernando"));
    }
    @Test public void testMessageShowsTheDate() {
        assertTrue(service.buildSmsMessage(appointment).contains("2026-09-01"));
    }
    @Test public void testMessageShowsTheTime() {
        assertTrue(service.buildSmsMessage(appointment).contains("10:30"));
    }
    @Test public void testMessageShowsTheReference() {
        assertTrue(service.buildSmsMessage(appointment).contains("APT1001"));
    }
    @Test public void testMessageShowsTheClinicTelephone() {
        assertTrue(service.buildSmsMessage(appointment).contains("011-2345678"));
    }
    @Test public void testShortNameMessageNamesTheTreatment() {
        assertTrue("The full template includes the treatment",
                   service.buildSmsMessage(appointment).contains("Filling"));
    }

    @Test public void testStandardMessageFitsOneSmsSegment() {
        assertTrue(service.buildSmsMessage(appointment).length()
                   <= ReminderService.SMS_LIMIT);
    }
    @Test public void testLongNamesStillFitOneSegment() {
        // This case caused a real defect: the first version produced a
        // 166-character message, which a provider bills as two segments.
        Appointment longNames = new Appointment("APT9999",
                "Wickramasinghe Mudiyanselage Bandaranayake Rajapaksa",
                "address", "0771234567", "Dr. Wickramasinghe",
                "Scaling & Polishing", "2026-09-01", "14:00");
        assertTrue(service.buildSmsMessage(longNames).length()
                   <= ReminderService.SMS_LIMIT);
    }
    @Test public void testCompactTemplateKeepsTheReference() {
        Appointment longNames = new Appointment("APT9999",
                "Wickramasinghe Mudiyanselage Bandaranayake Rajapaksa",
                "address", "0771234567", "Dr. Wickramasinghe",
                "Scaling & Polishing", "2026-09-01", "14:00");
        assertTrue("The reference must survive truncation",
                   service.buildSmsMessage(longNames).contains("APT9999"));
    }
    @Test public void testExtremeNameIsTruncatedNotOverflowed() {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            name.append("A");
        }
        Appointment extreme = new Appointment("APT1000", name.toString(),
                "a", "0771234567", "Dr. X", "Filling", "2026-09-01", "10:00");
        assertTrue(service.buildSmsMessage(extreme).length()
                   <= ReminderService.SMS_LIMIT);
    }
    @Test public void testSmsLimitIsOneHundredAndSixty() {
        assertEquals(160, ReminderService.SMS_LIMIT);
    }

    @Test public void testEmptyDiaryProducesNoMessages() throws Exception {
        assertEquals(0, service.generateTomorrowReminders().size());
    }
    @Test public void testOneAppointmentProducesOneMessage() throws Exception {
        ReminderService s = new ReminderService(
                new StubDAO(Arrays.asList(appointment)));
        assertEquals(1, s.generateTomorrowReminders().size());
    }
    @Test public void testThreeAppointmentsProduceThreeMessages() throws Exception {
        ReminderService s = new ReminderService(new StubDAO(
                Arrays.asList(appointment, appointment, appointment)));
        assertEquals(3, s.generateTomorrowReminders().size());
    }
    @Test public void testGeneratedMessagesMatchTheBuilder() throws Exception {
        ReminderService s = new ReminderService(
                new StubDAO(Arrays.asList(appointment)));
        assertEquals(s.buildSmsMessage(appointment),
                     s.generateTomorrowReminders().get(0));
    }
    @Test public void testOutputFolderIsNamedReminders() {
        assertEquals("reminders", ReminderService.OUTPUT_FOLDER);
    }
}
