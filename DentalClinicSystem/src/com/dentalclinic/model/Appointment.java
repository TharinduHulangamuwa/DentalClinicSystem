package com.dentalclinic.model;

/**
 * Represents one row of the appointments table.
 *
 * Dates and times are held as Strings in ISO format (yyyy-MM-dd and HH:mm)
 * because that is exactly what MySQL accepts for DATE and TIME columns, and
 * exactly what the Validator checks. Keeping one consistent format from the
 * text field through to the database removes a whole class of parsing bugs.
 */
public class Appointment {

    private String appointmentNo;     // e.g. APT1001
    private String patientName;
    private String address;
    private String contactNo;         // e.g. 0771234567
    private String dentistName;
    private String treatmentType;
    private String appointmentDate;   // yyyy-MM-dd
    private String appointmentTime;   // HH:mm

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

    public String getAppointmentNo()            { return appointmentNo; }
    public void setAppointmentNo(String v)      { this.appointmentNo = v; }

    public String getPatientName()              { return patientName; }
    public void setPatientName(String v)        { this.patientName = v; }

    public String getAddress()                  { return address; }
    public void setAddress(String v)            { this.address = v; }

    public String getContactNo()                { return contactNo; }
    public void setContactNo(String v)          { this.contactNo = v; }

    public String getDentistName()              { return dentistName; }
    public void setDentistName(String v)        { this.dentistName = v; }

    public String getTreatmentType()            { return treatmentType; }
    public void setTreatmentType(String v)      { this.treatmentType = v; }

    public String getAppointmentDate()          { return appointmentDate; }
    public void setAppointmentDate(String v)    { this.appointmentDate = v; }

    public String getAppointmentTime()          { return appointmentTime; }
    public void setAppointmentTime(String v)    { this.appointmentTime = v; }

    /** Convenience used by the search results panel. */
    public String getSlot() {
        return appointmentDate + " " + appointmentTime;
    }

    @Override
    public String toString() {
        return appointmentNo + " - " + patientName + " (" + treatmentType + ")";
    }
}
