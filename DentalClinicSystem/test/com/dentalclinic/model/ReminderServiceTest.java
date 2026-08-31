package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the patient reminder feature.
 *
 * A stub DAO is used so these tests run with no database and no WAMP server.
 * Overriding findByDate is possible because ReminderService depends on the
 * AppointmentDAO type rather than creating one internally - a small design
 * choice that makes the class testable.
 *
 * @author [Your Name]
 */
public class ReminderServiceTest {

    /** Returns fixed rows instead of querying MySQL. */
    private static class StubDAO extends AppointmentDAO {
        @Override
        public List<Appointment> findByDate(String date) {
            return new ArrayList<>();
        }
    }

    private ReminderService service;
    private Appointment appointment;

    @Before
    public void setUp() {
        service = new ReminderService(new StubDAO());
        appointment = new Appointment(
                "APT1001", "Kamal Silva", "12 Galle Road, Colombo 03",
                "0771234567", "Dr. Fernando", "Filling", "2026-09-01", "10:30");
    }

    // -----------------------------------------------------------------
    // TC-12  Reminder text contains every detail the patient needs
    // -----------------------------------------------------------------
    @Test
    public void testReminderMessageContent() {

        String message = service.buildSmsMessage(appointment);

        assertTrue("Clinic name missing",       message.contains("SUNRISE DENTAL"));
        assertTrue("Patient name missing",      message.contains("Kamal Silva"));
        assertTrue("Dentist name missing",      message.contains("Dr. Fernando"));
        assertTrue("Date missing",              message.contains("2026-09-01"));
        assertTrue("Time missing",              message.contains("10:30"));
        assertTrue("Appointment number missing", message.contains("APT1001"));
        assertTrue("Clinic telephone missing",  message.contains("011-2345678"));
    }

    // -----------------------------------------------------------------
    // TC-13  Messages must fit a single 160-character SMS segment
    //
    // This test was added after a defect was found: a patient with a long
    // name produced a 166-character message, which a provider would bill
    // as two segments. The compact fallback template fixed it.
    // -----------------------------------------------------------------
    @Test
    public void testMessageFitsOneSmsSegment() {

        // ordinary name
        assertTrue("Standard message should fit one segment",
                service.buildSmsMessage(appointment).length() <= ReminderService.SMS_LIMIT);

        // long patient name, long dentist name, long treatment name
        Appointment longNames = new Appointment(
                "APT9999",
                "Wickramasinghe Mudiyanselage Bandaranayake Rajapaksa",
                "address", "0771234567",
                "Dr. Wickramasinghe", "Scaling & Polishing",
                "2026-09-01", "14:00");

        String message = service.buildSmsMessage(longNames);
        assertTrue("Long names must still fit one segment",
                message.length() <= ReminderService.SMS_LIMIT);

        // the compact template drops the treatment, so the essentials remain
        assertTrue("Reference must survive truncation", message.contains("APT9999"));
    }

    // -----------------------------------------------------------------
    // TC-14  No appointments tomorrow means no messages and no file
    // -----------------------------------------------------------------
    @Test
    public void testNoAppointmentsProducesNoMessages() throws Exception {

        List<String> messages = service.generateTomorrowReminders();

        assertEquals("Empty diary should produce no messages", 0, messages.size());
        assertFalse("Nothing should be queued", messages.size() > 0);
    }
}
