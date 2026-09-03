package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for billing.
 *
 * Billing is the highest-risk area of the system: the scenario names "billing
 * errors" as a problem to solve. A defect here costs the clinic money or
 * overcharges a patient, so these tests carry the most weight in the plan.
 *
 * Note the delta on every double comparison. Floating point arithmetic is not
 * exact, so comparing doubles for equality is unreliable; 0.001 is far
 * tighter than one cent.
 *
 * @author [Your Name]
 */
public class BillTest {

    private Appointment appointment;

    @Before
    public void setUp() {
        appointment = new Appointment("APT1002", "Sanduni Jayasuriya",
                "5 Kandy Road, Kadawatha", "0712223334",
                "Dr. Silva", "Root Canal", "2026-06-10", "14:00");
    }

    @Test public void testTreatmentCostIsStored() {
        assertEquals(25000.00, new Bill(appointment, 25000.00, 1500.00)
                .getTreatmentCost(), 0.001);
    }
    @Test public void testConsultationFeeIsStored() {
        assertEquals(1500.00, new Bill(appointment, 25000.00, 1500.00)
                .getConsultationFee(), 0.001);
    }
    @Test public void testTotalIsCostPlusFee() {
        assertEquals(26500.00, new Bill(appointment, 25000.00, 1500.00)
                .getTotal(), 0.001);
    }
    @Test public void testAppointmentIsRetrievable() {
        assertEquals("APT1002", new Bill(appointment, 0, 0)
                .getAppointment().getAppointmentNo());
    }

    @Test public void testConsultationOnlyChargesTheFeeAlone() {
        appointment.setTreatmentType("Consultation Only");
        assertEquals(1500.00, new Bill(appointment, 0.00, 1500.00).getTotal(), 0.001);
    }
    @Test public void testFreeVisitTotalsZero() {
        assertEquals("A free visit must total zero, not fail",
                     0.00, new Bill(appointment, 0.00, 0.00).getTotal(), 0.001);
    }
    @Test public void testTreatmentWithNoConsultationFee() {
        assertEquals(6000.00, new Bill(appointment, 6000.00, 0.00).getTotal(), 0.001);
    }
    @Test public void testDecimalAmountsKeepPrecision() {
        assertEquals(5750.75, new Bill(appointment, 4500.50, 1250.25)
                .getTotal(), 0.001);
    }
    @Test public void testLargeAmountsAreHandled() {
        assertEquals(46500.00, new Bill(appointment, 45000.00, 1500.00)
                .getTotal(), 0.001);
    }
    @Test public void testRepeatedCallsGiveTheSameTotal() {
        Bill bill = new Bill(appointment, 25000.00, 1500.00);
        assertEquals(bill.getTotal(), bill.getTotal(), 0.001);
    }

    @Test public void testReceiptShowsClinicHeader() {
        assertTrue(receipt().contains("SUNRISE DENTAL CLINIC"));
    }
    @Test public void testReceiptShowsAppointmentNumber() {
        assertTrue(receipt().contains("APT1002"));
    }
    @Test public void testReceiptShowsPatientName() {
        assertTrue(receipt().contains("Sanduni Jayasuriya"));
    }
    @Test public void testReceiptShowsContactNumber() {
        assertTrue(receipt().contains("0712223334"));
    }
    @Test public void testReceiptShowsDentistName() {
        assertTrue(receipt().contains("Dr. Silva"));
    }
    @Test public void testReceiptShowsTreatmentType() {
        assertTrue(receipt().contains("Root Canal"));
    }
    @Test public void testReceiptShowsDateAndTime() {
        assertTrue(receipt().contains("2026-06-10"));
        assertTrue(receipt().contains("14:00"));
    }
    @Test public void testReceiptShowsTotalWithThousandsSeparator() {
        assertTrue("26,500.00 is easier to read than 26500.0",
                   receipt().contains("26,500.00"));
    }
    @Test public void testReceiptShowsTotalLabel() {
        assertTrue(receipt().contains("TOTAL PAYABLE"));
    }
    @Test public void testReceiptNeverShowsRawDoubleFormatting() {
        assertFalse("Money must never appear as 26500.0",
                    receipt().contains("26500.0 "));
    }
    @Test public void testReceiptIsMultiLine() {
        assertTrue(receipt().split("\n").length > 10);
    }

    private String receipt() {
        return new Bill(appointment, 25000.00, 1500.00).generateReceipt();
    }
}
