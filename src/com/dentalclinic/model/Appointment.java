package com.dentalclinic.model;

/**
 * One row of the appointments table.
 *
 * Dates and times are held as Strings in ISO format (yyyy-MM-dd and HH:mm)
 * because that is exactly what MySQL accepts for DATE and TIME columns, and
 * exactly what Validator checks. Keeping one format from the text field
 * through to the database removes a whole class of parsing bug.
 *
 * @author [Your Name]
 */
public class Appointment {

    private String appointmentNo;     // APT1001
    private String patientName;
    private String address;
    private String contactNo;         // 0771234567
    private String dentistName;
    private String treatmentType;
    private String appointmentDate;   // yyyy-MM-dd
    private String appointmentTime;   // HH:mm
    private String status = "BOOKED";

    public Appointment() { }

    public Appointment(String appointmentNo, String patientName, String address,
                       String contactNo, String dentistName, String treatmentType,
                       String appointmentDate, String appointmentTime) {
        this.appointmentNo   = appointmentNo;
        this.patientName     = patientName;
        this.address         = address;
        this.contactNo       = contactNo;
        this.dentistName     = dentistName;
        this.treatmentType   = treatmentType;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
    }

    public String getAppointmentNo()         { return appointmentNo; }
    public void setAppointmentNo(String v)   { this.appointmentNo = v; }

    public String getPatientName()           { return patientName; }
    public void setPatientName(String v)     { this.patientName = v; }

    public String getAddress()               { return address; }
    public void setAddress(String v)         { this.address = v; }

    public String getContactNo()             { return contactNo; }
    public void setContactNo(String v)       { this.contactNo = v; }

    public String getDentistName()           { return dentistName; }
    public void setDentistName(String v)     { this.dentistName = v; }

    public String getTreatmentType()         { return treatmentType; }
    public void setTreatmentType(String v)   { this.treatmentType = v; }

    public String getAppointmentDate()       { return appointmentDate; }
    public void setAppointmentDate(String v) { this.appointmentDate = v; }

    public String getAppointmentTime()       { return appointmentTime; }
    public void setAppointmentTime(String v) { this.appointmentTime = v; }

    public String getStatus()                { return status; }
    public void setStatus(String v)          { this.status = v; }

    /** Convenience used by the search panel and the reminder service. */
    public String getSlot() {
        return appointmentDate + " " + appointmentTime;
    }

    @Override
    public String toString() {
        return appointmentNo + " - " + patientName + " (" + treatmentType + ")";
    }
}
