package com.dentalclinic.model;

/**
 * Calculates a patient bill and formats the printable receipt.
 *
 * DESIGN DECISION: the arithmetic and the receipt text live here, not in the
 * view. That keeps the calculation unit-testable, and means the same Bill
 * object can be rendered to a text area today and to a printer or PDF later
 * without changing this class.
 *
 * The fields are final because a bill, once issued, should not change.
 *
 * @author [Your Name]
 */
public class Bill {

    private static final String LINE =
        "==================================================";
    private static final String THIN =
        "--------------------------------------------------";

    private final Appointment appointment;
    private final double treatmentCost;
    private final double consultationFee;

    public Bill(Appointment appointment, double treatmentCost, double consultationFee) {
        this.appointment     = appointment;
        this.treatmentCost   = treatmentCost;
        this.consultationFee = consultationFee;
    }

    public Appointment getAppointment() { return appointment; }
    public double getTreatmentCost()    { return treatmentCost; }
    public double getConsultationFee()  { return consultationFee; }

    /** Total payable, in Sri Lankan rupees. */
    public double getTotal() {
        return treatmentCost + consultationFee;
    }

    /** The plain-text receipt shown on screen and given to the patient. */
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("            SUNRISE DENTAL CLINIC\n");
        sb.append("              Colombo, Sri Lanka\n");
        sb.append("              Tel: 011-2345678\n");
        sb.append(LINE).append("\n");
        sb.append(String.format("Appointment No : %s%n", appointment.getAppointmentNo()));
        sb.append(String.format("Patient        : %s%n", appointment.getPatientName()));
        sb.append(String.format("Contact        : %s%n", appointment.getContactNo()));
        sb.append(String.format("Dentist        : %s%n", appointment.getDentistName()));
        sb.append(String.format("Date / Time    : %s   %s%n",
                appointment.getAppointmentDate(), appointment.getAppointmentTime()));
        sb.append(THIN).append("\n");
        sb.append(String.format("%-32s %15s%n",
                "Treatment: " + appointment.getTreatmentType(),
                String.format("%,.2f", treatmentCost)));
        sb.append(String.format("%-32s %15s%n", "Consultation Fee",
                String.format("%,.2f", consultationFee)));
        sb.append(THIN).append("\n");
        sb.append(String.format("%-32s %15s%n", "TOTAL PAYABLE (LKR)",
                String.format("%,.2f", getTotal())));
        sb.append(LINE).append("\n");
        sb.append("      Thank you for visiting. Get well soon!\n");
        sb.append(LINE).append("\n");
        return sb.toString();
    }
}
