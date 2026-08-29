package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for the Appointment entity.
 *
 * These verify that data survives the round trip through the object without
 * being lost or reordered. The eight-argument constructor is an obvious
 * place for a transposition defect - two arguments of the same type swapped
 * by mistake - and a compiler cannot catch that. These tests can.
 *
 * @author [Your Name]
 */
public class AppointmentTest {

    // -----------------------------------------------------------------
    // TC-05  Constructor stores every field in the correct position
    // -----------------------------------------------------------------
    @Test
    public void testConstructorStoresAllFields() {

        Appointment appointment = new Appointment(
                "APT1001",
                "Kamal Silva",
                "12 Galle Road, Colombo 03",
                "0771234567",
                "Dr. Fernando",
                "Filling",
                "2026-06-10",
                "10:30");

        assertEquals("APT1001",                   appointment.getAppointmentNo());
        assertEquals("Kamal Silva",               appointment.getPatientName());
        assertEquals("12 Galle Road, Colombo 03", appointment.getAddress());
        assertEquals("0771234567",                appointment.getContactNo());
        assertEquals("Dr. Fernando",              appointment.getDentistName());
        assertEquals("Filling",                   appointment.getTreatmentType());
        assertEquals("2026-06-10",                appointment.getAppointmentDate());
        assertEquals("10:30",                     appointment.getAppointmentTime());
    }

    // -----------------------------------------------------------------
    // TC-06  Setters update state, empty constructor leaves fields null
    // -----------------------------------------------------------------
    @Test
    public void testSettersUpdateValues() {

        Appointment appointment = new Appointment();
        assertNull("New object should start empty", appointment.getPatientName());

        appointment.setAppointmentNo("APT2002");
        appointment.setPatientName("Nimal Perera");
        appointment.setContactNo("0719998887");
        appointment.setTreatmentType("Root Canal");
        appointment.setAppointmentDate("2026-07-01");
        appointment.setAppointmentTime("15:45");

        assertEquals("APT2002",      appointment.getAppointmentNo());
        assertEquals("Nimal Perera", appointment.getPatientName());
        assertEquals("0719998887",   appointment.getContactNo());
        assertEquals("Root Canal",   appointment.getTreatmentType());

        // derived value must reflect the updated state
        assertEquals("2026-07-01 15:45", appointment.getSlot());
    }

    // -----------------------------------------------------------------
    // TC-07  toString is safe for display in dropdowns and logs
    // -----------------------------------------------------------------
    @Test
    public void testToStringFormat() {

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo("APT3003");
        appointment.setPatientName("Ruwan Bandara");
        appointment.setTreatmentType("Extraction");

        String text = appointment.toString();

        assertTrue("Appointment number missing", text.contains("APT3003"));
        assertTrue("Patient name missing",       text.contains("Ruwan Bandara"));
        assertTrue("Treatment type missing",     text.contains("Extraction"));
    }
}