package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the Appointment entity.
 *
 * The eight-argument constructor is an obvious place for a transposition
 * defect - two arguments of the same type swapped by mistake - which a
 * compiler cannot catch but these tests can.
 *
 * @author [Your Name]
 */
public class AppointmentTest {

    private Appointment appointment;

    @Before
    public void setUp() {
        appointment = new Appointment("APT1001", "Kamal Silva",
                "12 Galle Road, Colombo 03", "0771234567",
                "Dr. Fernando", "Filling", "2026-06-10", "10:30");
    }

    @Test public void testConstructorStoresAppointmentNo() {
        assertEquals("APT1001", appointment.getAppointmentNo());
    }
    @Test public void testConstructorStoresPatientName() {
        assertEquals("Kamal Silva", appointment.getPatientName());
    }
    @Test public void testConstructorStoresAddress() {
        assertEquals("12 Galle Road, Colombo 03", appointment.getAddress());
    }
    @Test public void testConstructorStoresContactNo() {
        assertEquals("0771234567", appointment.getContactNo());
    }
    @Test public void testConstructorStoresDentistName() {
        assertEquals("Dr. Fernando", appointment.getDentistName());
    }
    @Test public void testConstructorStoresTreatmentType() {
        assertEquals("Filling", appointment.getTreatmentType());
    }
    @Test public void testConstructorStoresDate() {
        assertEquals("2026-06-10", appointment.getAppointmentDate());
    }
    @Test public void testConstructorStoresTime() {
        assertEquals("10:30", appointment.getAppointmentTime());
    }
    @Test public void testStatusDefaultsToBooked() {
        assertEquals("BOOKED", appointment.getStatus());
    }

    @Test public void testEmptyConstructorLeavesFieldsNull() {
        Appointment empty = new Appointment();
        assertNull(empty.getAppointmentNo());
        assertNull(empty.getPatientName());
        assertNull(empty.getContactNo());
    }

    @Test public void testSetterUpdatesAppointmentNo() {
        appointment.setAppointmentNo("APT2002");
        assertEquals("APT2002", appointment.getAppointmentNo());
    }
    @Test public void testSetterUpdatesPatientName() {
        appointment.setPatientName("Nimal Perera");
        assertEquals("Nimal Perera", appointment.getPatientName());
    }
    @Test public void testSetterUpdatesContactNo() {
        appointment.setContactNo("0719998887");
        assertEquals("0719998887", appointment.getContactNo());
    }
    @Test public void testSetterUpdatesTreatmentType() {
        appointment.setTreatmentType("Root Canal");
        assertEquals("Root Canal", appointment.getTreatmentType());
    }
    @Test public void testSetterUpdatesDateAndTime() {
        appointment.setAppointmentDate("2026-07-01");
        appointment.setAppointmentTime("15:45");
        assertEquals("2026-07-01", appointment.getAppointmentDate());
        assertEquals("15:45", appointment.getAppointmentTime());
    }
    @Test public void testSetterUpdatesStatus() {
        appointment.setStatus("CANCELLED");
        assertEquals("CANCELLED", appointment.getStatus());
    }

    @Test public void testGetSlotCombinesDateAndTime() {
        assertEquals("2026-06-10 10:30", appointment.getSlot());
    }
    @Test public void testGetSlotReflectsUpdatedValues() {
        appointment.setAppointmentDate("2026-07-01");
        appointment.setAppointmentTime("15:45");
        assertEquals("2026-07-01 15:45", appointment.getSlot());
    }

    @Test public void testToStringIncludesNumber() {
        assertTrue(appointment.toString().contains("APT1001"));
    }
    @Test public void testToStringIncludesPatient() {
        assertTrue(appointment.toString().contains("Kamal Silva"));
    }
    @Test public void testToStringIncludesTreatment() {
        assertTrue(appointment.toString().contains("Filling"));
    }
}
