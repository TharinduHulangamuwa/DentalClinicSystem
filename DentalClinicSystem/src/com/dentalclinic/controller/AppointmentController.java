package com.dentalclinic.controller;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentDAO;
import com.dentalclinic.model.Bill;
import com.dentalclinic.model.DBConnection;
import com.dentalclinic.model.ReminderService;
import com.dentalclinic.model.Session;
import com.dentalclinic.model.User;
import com.dentalclinic.model.Validator;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import com.dentalclinic.model.UserDAO;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * LOGIC TIER - all appointment, billing and reporting behaviour.
 *
 * MULTITHREADING IS DEMONSTRATED TWICE IN THIS CLASS:
 *
 *   THREAD 1  startClockThread()
 *             A user-created daemon Thread that updates the status bar clock
 *             once per second for the whole life of the application. It never
 *             touches Swing directly - every update is handed to the Event
 *             Dispatch Thread through SwingUtilities.invokeLater().
 *
 *   THREAD 2  loadAppointmentsInBackground()
 *             A SwingWorker that queries MySQL for all appointment rows on a
 *             worker thread, then populates the JTable back on the EDT inside
 *             done(). The window stays responsive while the query runs.
 */
public class AppointmentController {

    private final MainView       view;
    private final AppointmentDAO dao = new AppointmentDAO();
    private final User           loggedInUser;

    /** Holds the last successful search so the bill button knows what to bill. */
    private Appointment currentAppointment;

    /** Reference kept so the clock can be stopped cleanly on exit. */
    private Thread clockThread;

    /** Generates patient reminder notifications. */
    private final ReminderService reminderService = new ReminderService(new AppointmentDAO());

    public AppointmentController(MainView view, User loggedInUser) {
        this.view         = view;
        this.loggedInUser = loggedInUser;

        view.setLoggedInUser(loggedInUser.getFullName());

        // ---- Observer pattern: subscribe to every view event ----
        view.addSaveListener(e    -> saveAppointment());
        view.addClearListener(e   -> {
            if (view.confirmClear()) {
                view.clearForm();
                suggestNextAppointmentNo();
            }
        });
        view.addSearchListener(e  -> searchAppointment());
        view.addBillListener(e    -> generateBill());
        view.addRefreshListener(e -> loadAppointmentsInBackground());
        view.addReportRefreshListener(e -> loadDailyReport());
        view.addRemindersListener(e -> generateReminders());
        view.addLogoutListener(e -> logout());
        view.addWindowCloseListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        // ---- startup sequence ----
        loadTreatments();
        startClockThread();               // THREAD 1
        loadAppointmentsInBackground();   // THREAD 2
        loadDailyReport();
        suggestNextAppointmentNo();
        view.focusRegisterTab();
    }

    // =================================================================
    // THREAD 1 - LIVE CLOCK ON A BACKGROUND DAEMON THREAD
    //
    // Why a separate thread: the clock must tick once per second forever.
    // Doing that on the Event Dispatch Thread with a sleep loop would
    // freeze the entire user interface.
    //
    // Why setDaemon(true): a daemon thread does not prevent the JVM from
    // shutting down, so the application can exit without hanging.
    //
    // Why invokeLater: Swing components must only be modified on the EDT.
    // This background thread NEVER calls view.setClock() directly - it
    // schedules the call onto the EDT instead. This is the correct,
    // thread-safe pattern.
    // =================================================================
    private void startClockThread() {
        clockThread = new Thread(() -> {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE dd MMM yyyy   HH:mm:ss");

            while (!Thread.currentThread().isInterrupted()) {
                final String timestamp = formatter.format(new Date());

                // Hand the UI update to the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> view.setClock(timestamp));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Restore the interrupt flag and leave the loop cleanly
                    Thread.currentThread().interrupt();
                }
            }
        }, "clock-thread");

        clockThread.setDaemon(true);
        clockThread.start();
    }

    // =================================================================
    // THREAD 2 - ASYNCHRONOUS DATABASE LOAD WITH SwingWorker
    //
    // findAll() may return hundreds of rows once the clinic has been
    // running for a year. Executing that query on the Event Dispatch
    // Thread would freeze the window and Windows would report the
    // application as "Not Responding".
    //
    // SwingWorker splits the work in two:
    //   doInBackground()  runs on a worker thread  -> slow work allowed
    //   done()            runs on the EDT          -> Swing updates allowed
    // =================================================================
    private void loadAppointmentsInBackground() {
        view.setStatus("Loading appointments...");
        view.setBusy(true);

        new SwingWorker<List<Appointment>, Void>() {

            @Override
            protected List<Appointment> doInBackground() throws Exception {
                // WORKER THREAD - no Swing calls permitted in here
                return dao.findAll();
            }

            @Override
            protected void done() {
                // EVENT DISPATCH THREAD - Swing calls are safe again
                try {
                    List<Appointment> appointments = get();

                    DefaultTableModel model = view.getTableModel();
                    model.setRowCount(0);
                    for (Appointment a : appointments) {
                        model.addRow(new Object[]{
                            a.getAppointmentNo(),
                            a.getPatientName(),
                            a.getContactNo(),
                            a.getDentistName(),
                            a.getTreatmentType(),
                            a.getAppointmentDate(),
                            a.getAppointmentTime()
                        });
                    }
                    view.setStatus(appointments.size() + " appointment(s) loaded.");

                } catch (Exception ex) {
                    view.setStatusError("Could not load appointments.");
                    view.showError("Could not load appointments.\n\n"
                                 + ex.getMessage()
                                 + "\n\nCheck that WAMP is running.");
                } finally {
                    view.setBusy(false);
                }
            }
        }.execute();
    }

    // =================================================================
    // Load the treatment price list into the dropdown
    // =================================================================
    private void loadTreatments() {
        try {
            Map<String, Double> treatments = dao.findTreatments();
            view.setTreatmentOptions(treatments.keySet());
        } catch (Exception ex) {
            view.showError("Could not load the treatment list.\n\n" + ex.getMessage());
        }
    }

    // =================================================================
    // FUNCTIONALITY 2 - REGISTER NEW APPOINTMENT
    // =================================================================
    private void saveAppointment() {

        // Version 1.2: every validation failure is reported beside the field
        // that caused it instead of in a modal dialog. All fields are checked
        // in one pass so the user sees every problem at once rather than
        // fixing one, resubmitting, and discovering the next.
        view.clearAllFieldErrors();

        boolean valid = true;

        if (!Validator.isValidAppointmentNo(view.getAppointmentNo())) {
            view.showFieldError("appointmentNo", "Must be APT plus 4 digits");
            valid = false;
        }
        if (!Validator.isValidName(view.getPatientName())) {
            view.showFieldError("patientName", "Letters and spaces only");
            valid = false;
        }
        if (!Validator.isValidContact(view.getContactNo())) {
            view.showFieldError("contactNo", "10 digits starting with 0");
            valid = false;
        }
        if (!Validator.isValidDate(view.getDate())) {
            view.showFieldError("date", "Use yyyy-MM-dd");
            valid = false;
        } else if (!Validator.isNotPastDate(view.getDate())) {
            view.showFieldError("date", "Cannot book in the past");
            valid = false;
        }
        if (!Validator.isValidTime(view.getTime())) {
            view.showFieldError("time", "Use HH:mm, e.g. 14:30");
            valid = false;
        } else if (!Validator.isWithinClinicHours(view.getTime())) {
            view.showFieldError("time", "Clinic open 08:00 to 20:00");
            valid = false;
        }

        if (!valid) {
            view.setStatusError("Please correct the highlighted fields.");
            return;
        }

        Appointment appointment = new Appointment(
                view.getAppointmentNo(),
                view.getPatientName(),
                view.getAddress(),
                view.getContactNo(),
                view.getDentistName(),
                view.getTreatmentType(),
                view.getDate(),
                view.getTime());

        try {
            view.setBusy(true);

            // duplicate appointment number
            if (dao.findByNo(appointment.getAppointmentNo()) != null) {
                view.showFieldError("appointmentNo", "Already in use");
                view.setStatusError("That appointment number already exists.");
                return;
            }

            // double booking, the problem stated in the scenario
            if (dao.slotTaken(appointment.getDentistName(),
                              appointment.getAppointmentDate(),
                              appointment.getAppointmentTime())) {
                view.showFieldError("time", "Dentist already booked");
                view.setStatusError("Double booking prevented.");
                view.showError("DOUBLE BOOKING PREVENTED\n\n"
                             + appointment.getDentistName()
                             + " already has an appointment on "
                             + appointment.getAppointmentDate() + " at "
                             + appointment.getAppointmentTime()
                             + ".\n\nPlease choose a different time or dentist.");
                return;
            }

            if (dao.save(appointment)) {
                view.setStatusSuccess("Appointment " + appointment.getAppointmentNo()
                        + " saved for " + appointment.getPatientName() + ".");
                view.clearForm();
                suggestNextAppointmentNo();
                loadAppointmentsInBackground();
                loadDailyReport();
            }

        } catch (SQLIntegrityConstraintViolationException ex) {
            view.showFieldError("time", "Slot taken");
            view.setStatusError("Double booking prevented by the database.");
            view.showError("DOUBLE BOOKING PREVENTED BY THE DATABASE\n\n"
                         + "That slot was taken while you were filling the form.");
        } catch (Exception ex) {
            view.setStatusError("Save failed.");
            view.showError("Could not save the appointment.\n\n" + ex.getMessage());
        } finally {
            view.setBusy(false);
        }
    }

    /**
     * Looks up the next free appointment number and offers it to the user.
     * Inventing a unique number by hand is error prone and slow; the system
     * already knows what the next one should be.
     */
    private void suggestNextAppointmentNo() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new AppointmentDAO().nextAppointmentNo();
            }
            @Override
            protected void done() {
                try {
                    view.suggestAppointmentNo(get());
                } catch (Exception ignored) {
                    // a suggestion is a convenience; failing to make one is harmless
                }
            }
        }.execute();
    }

    // =================================================================
    // FUNCTIONALITY 3 - DISPLAY APPOINTMENT DETAILS
    // =================================================================
    private void searchAppointment() {
        if (!Validator.isValidAppointmentNo(view.getSearchNo())) {
            view.setStatusError("Enter a valid number such as APT1001.");
            return;
        }

        try {
            currentAppointment = dao.findByNo(view.getSearchNo());

            if (currentAppointment == null) {
                view.setDetails("\n   No appointment found with number "
                              + view.getSearchNo() + ".\n");
                view.setReceipt("");
                view.setStatusError("No record found for " + view.getSearchNo() + ".");
                return;
            }

            view.setDetails(
                  "Appointment No : " + currentAppointment.getAppointmentNo()   + "\n"
                + "Patient Name   : " + currentAppointment.getPatientName()     + "\n"
                + "Address        : " + currentAppointment.getAddress()         + "\n"
                + "Contact No     : " + currentAppointment.getContactNo()       + "\n"
                + "Dentist        : " + currentAppointment.getDentistName()     + "\n"
                + "Treatment      : " + currentAppointment.getTreatmentType()   + "\n"
                + "Date           : " + currentAppointment.getAppointmentDate() + "\n"
                + "Time           : " + currentAppointment.getAppointmentTime());

            view.setStatusSuccess("Found: " + currentAppointment.getPatientName());

        } catch (Exception ex) {
            view.showError("Search failed.\n\n" + ex.getMessage());
        }
    }

    // =================================================================
    // FUNCTIONALITY 4 - CALCULATE AND PRINT BILL
    // =================================================================
    private void generateBill() {
        if (currentAppointment == null) {
            view.setStatusError("Search for an appointment first.");
            return;
        }
        if (!Validator.isValidFee(view.getConsultationFee())) {
            view.setStatusError("Consultation fee must be a number of zero or more.");
            return;
        }

        try {
            double treatmentCost = dao.findTreatmentCost(currentAppointment.getTreatmentType());
            double fee           = Double.parseDouble(view.getConsultationFee());

            Bill bill = new Bill(currentAppointment, treatmentCost, fee);
            view.setReceipt(bill.generateReceipt());
            view.setStatusSuccess(String.format("Bill generated. Total LKR %,.2f", bill.getTotal()));

        } catch (Exception ex) {
            view.showError("Could not generate the bill.\n\n" + ex.getMessage());
        }
    }

    // =================================================================
    // MANAGEMENT REPORT - reads the vw_daily_schedule SQL view
    //
    // Loaded on a SwingWorker for the same reason as the appointment
    // list: an aggregate query over a year of appointments is slow
    // enough to freeze the window if run on the Event Dispatch Thread.
    // =================================================================
    private void loadDailyReport() {
        new SwingWorker<List<String[]>, Void>() {

            @Override
            protected List<String[]> doInBackground() throws Exception {
                return new AppointmentDAO().dailyScheduleReport();
            }

            @Override
            protected void done() {
                try {
                    List<String[]> rows = get();
                    DefaultTableModel model = view.getReportTableModel();
                    model.setRowCount(0);
                    for (String[] row : rows) {
                        model.addRow(row);
                    }
                } catch (Exception ex) {
                    view.setStatus("Daily report could not be loaded.");
                }
            }
        }.execute();
    }

    // =================================================================
    // PATIENT REMINDERS - generates notification messages
    //
    // Runs on a SwingWorker because it queries the database and then
    // writes a file, either of which could block the interface.
    // =================================================================
    private void generateReminders() {
        view.setStatus("Generating reminders...");

        new SwingWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                return reminderService.generateTomorrowReminders();
            }

            @Override
            protected void done() {
                try {
                    List<String> messages = get();

                    if (messages.isEmpty()) {
                        view.setNotifications("No appointments scheduled for tomorrow.\n\n"
                                + "Nothing to send.");
                        view.setStatus("No reminders needed.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("REMINDER MESSAGES GENERATED\n");
                    sb.append("===========================\n");
                    sb.append(messages.size()).append(" message(s) queued for dispatch.\n");
                    sb.append("Written to the '").append(ReminderService.OUTPUT_FOLDER);
                    sb.append("' folder inside the project.\n\n");

                    for (int i = 0; i < messages.size(); i++) {
                        sb.append("[").append(i + 1).append("] ")
                          .append(messages.get(i)).append("\n\n");
                    }

                    view.setNotifications(sb.toString());
                    view.setStatusSuccess(messages.size() + " reminder(s) generated.");

                } catch (Exception ex) {
                    view.setStatusError("Reminder generation failed.");
                    view.showError("Could not generate reminders.\n\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    // =================================================================
    // LOGOUT - ends the session and returns to the login screen
    //
    // The database connection stays open because the application is
    // still running; only the session is discarded.
    // =================================================================
    private void logout() {
        int choice = javax.swing.JOptionPane.showConfirmDialog(view,
                "Log out and return to the login screen?",
                "Confirm Logout", javax.swing.JOptionPane.YES_NO_OPTION);

        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        if (clockThread != null) {
            clockThread.interrupt();
        }
        Session.end();
        view.dispose();

        LoginView loginView = new LoginView();
        new LoginController(loginView, new UserDAO());
        loginView.setVisible(true);
    }

    // =================================================================
    // FUNCTIONALITY 6 - EXIT SYSTEM SAFELY
    // =================================================================
    private void exitApplication() {
        if (!view.confirmExit()) {
            return;
        }
        // Stop the background clock thread cleanly
        if (clockThread != null) {
            clockThread.interrupt();
        }
        // Release the shared database connection
        Session.end();
        DBConnection.close();
        System.out.println("Application closed by " + loggedInUser.getUsername());
        System.exit(0);
    }
}
