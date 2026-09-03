package com.dentalclinic.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds appointment reminder messages for patients.
 *
 * HOW DISPATCH WORKS, STATED HONESTLY:
 * Messages are written to a dated text file in the "reminders" folder rather
 * than sent through a live SMTP or SMS gateway. This was deliberate: a real
 * gateway needs paid credentials, an internet connection and an external
 * account, none of which can be demonstrated reliably. The file acts as a
 * dispatch queue that a mail merge or an SMS provider's bulk upload could
 * consume directly.
 *
 * The design keeps a real gateway one method away: only dispatch() would
 * change. Message construction, scheduling and the recipient query stay.
 *
 * @author [Your Name]
 */
public class ReminderService {

    public static final String OUTPUT_FOLDER = "reminders";

    /** A single SMS segment is 160 characters; longer messages cost double. */
    public static final int SMS_LIMIT = 160;

    private final AppointmentDAO dao;

    public ReminderService(AppointmentDAO dao) {
        this.dao = dao;
    }

    /**
     * Builds a reminder for every appointment tomorrow and writes them to a
     * dated dispatch file.
     * @return the messages generated, so the caller can display them
     */
    public List<String> generateTomorrowReminders() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        List<Appointment> appointments = dao.findByDate(tomorrow);

        List<String> messages = new ArrayList<>();
        for (Appointment a : appointments) {
            messages.add(buildSmsMessage(a));
        }
        if (!messages.isEmpty()) {
            dispatch(tomorrow, messages);
        }
        return messages;
    }

    /**
     * Builds the SMS text for one appointment.
     *
     * Two templates are used. The full one names the treatment; if a long
     * patient or dentist name pushes the message past the 160-character
     * segment limit, the compact template drops the treatment type, the least
     * essential detail. Every message therefore costs one segment.
     *
     * This rule exists because testing found a message reaching 166
     * characters for a patient with a long name, which would have been billed
     * as two segments.
     */
    public String buildSmsMessage(Appointment a) {

        String full = "SUNRISE DENTAL: Dear " + a.getPatientName()
                    + ", appt with " + a.getDentistName()
                    + " on " + a.getAppointmentDate()
                    + " at " + a.getAppointmentTime()
                    + " (" + a.getTreatmentType() + "). Ref " + a.getAppointmentNo()
                    + ". Tel 011-2345678";

        if (full.length() <= SMS_LIMIT) {
            return full;
        }

        String compact = "SUNRISE DENTAL: Dear " + a.getPatientName()
                       + ", appt with " + a.getDentistName()
                       + " on " + a.getAppointmentDate()
                       + " at " + a.getAppointmentTime()
                       + ". Ref " + a.getAppointmentNo() + ". Tel 011-2345678";

        if (compact.length() > SMS_LIMIT) {
            return compact.substring(0, SMS_LIMIT - 3) + "...";
        }
        return compact;
    }

    /**
     * Writes the dispatch queue file.
     * This is the ONLY method that changes if a real provider is connected.
     */
    private void dispatch(String date, List<String> messages) throws IOException {
        File folder = new File(OUTPUT_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, "reminders_" + date + ".txt");

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("SUNRISE DENTAL CLINIC - APPOINTMENT REMINDERS");
            out.println("Dispatch date       : " + LocalDate.now());
            out.println("For appointments on : " + date);
            out.println("Messages to send    : " + messages.size());
            out.println("=============================================");
            out.println();
            for (int i = 0; i < messages.size(); i++) {
                out.println("[" + (i + 1) + "] " + messages.get(i));
                out.println();
            }
        }
        System.out.println("[REMINDERS] dispatch file written: " + file.getAbsolutePath());
    }
}
