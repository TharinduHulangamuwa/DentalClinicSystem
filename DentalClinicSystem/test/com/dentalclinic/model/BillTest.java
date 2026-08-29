package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for billing.
 *
 * Billing is the highest-risk area of this system: the scenario explicitly
 * lists "billing errors" as one of the problems the clinic wants solved.
 * A defect here costs the clinic real money or overcharges a patient, so
 * these tests carry the most weight in the test plan.
 *
 * Note the delta argument on assertEquals for doubles. Floating point
 * arithmetic is not exact, so comparing doubles for exact equality is
 * unreliable. A tolerance of 0.001 is far tighter than one cent.
 *
 * @author [Your Name]
 */
public class BillTest {

    private Appointment appointment;

    @Before
    public void setUp() {
        // Fresh fixture before every test, so tests cannot affect each other
        appointment = new Appointment(
                "APT1002",
                "Sanduni Jayasuriya",
                "5 Kandy Road, Kadawatha",
                "0712223334",
                "Dr. Silva",
                "Root Canal",
                "2026-06-10",
                "14:00");
    }

    // -----------------------------------------------------------------
    // TC-08  Total = treatment cost + consultation fee
    // -----------------------------------------------------------------
    @Test
    public void testTotalCalculation() {

        Bill bill = new Bill(appointment, 25000.00, 1500.00);

        assertEquals(25000.00, bill.getTreatmentCost(),   0.001);
        assertEquals(1500.00,  bill.getConsultationFee(), 0.001);
        assertEquals(26500.00, bill.getTotal(),           0.001);
    }

    // -----------------------------------------------------------------
    // TC-09  Zero-cost boundary: consultation-only visits
    // -----------------------------------------------------------------
    @Test
    public void testConsultationOnlyBill() {

        appointment.setTreatmentType("Consultation Only");
        Bill bill = new Bill(appointment, 0.00, 1500.00);

        assertEquals(1500.00, bill.getTotal(), 0.001);

        // free consultation promotion - total must be exactly zero, not an error
        Bill freeVisit = new Bill(appointment, 0.00, 0.00);
        assertEquals(0.00, freeVisit.getTotal(), 0.001);
    }

    // -----------------------------------------------------------------
    // TC-10  Receipt content is complete and correctly formatted
    // -----------------------------------------------------------------
    @Test
    public void testReceiptContent() {

        Bill bill = new Bill(appointment, 25000.00, 1500.00);
        String receipt = bill.generateReceipt();

        assertTrue("Clinic header missing",         receipt.contains("SUNRISE DENTAL CLINIC"));
        assertTrue("Appointment number missing",    receipt.contains("APT1002"));
        assertTrue("Patient name missing",          receipt.contains("Sanduni Jayasuriya"));
        assertTrue("Dentist name missing",          receipt.contains("Dr. Silva"));
        assertTrue("Treatment type missing",        receipt.contains("Root Canal"));
        assertTrue("Total not shown with grouping", receipt.contains("26,500.00"));
        assertTrue("Total label missing",           receipt.contains("TOTAL PAYABLE"));
    }

    // -----------------------------------------------------------------
    // TC-11  Decimal amounts do not lose precision
    // -----------------------------------------------------------------
    @Test
    public void testDecimalPrecision() {

        Bill bill = new Bill(appointment, 4500.50, 1250.25);
        assertEquals(5750.75, bill.getTotal(), 0.001);
    }
}