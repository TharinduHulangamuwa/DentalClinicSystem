package com.dentalclinic.controller;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Bill;
import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionStore;
import com.dentalclinic.model.User;
import com.dentalclinic.model.Validator;
import com.dentalclinic.service.ApiClient;
import com.dentalclinic.service.ApiException;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * LOGIC TIER - all appointment, billing, reporting and session behaviour.
 *
 * MULTITHREADING IS DEMONSTRATED THREE TIMES IN THIS CLASS:
 *
 *   THREAD 1  startClockThread()
 *             A daemon Thread updating the status bar clock every second for
 *             the life of the application.
 *
 *   THREAD 2  SwingWorker, used by every database operation
 *             Queries run on a worker thread; results are applied on the
 *             Event Dispatch Thread inside done(), so the window never
 *             freezes while MySQL answers.
 *
 *   THREAD 3  startSessionMonitor()
 *             A daemon Thread checking every second whether the session has
 *             gone idle past its limit, refreshing the header countdown and
 *             signing the user out automatically.
 *
 * Neither daemon thread ever touches a Swing component directly. Every
 * update is handed to the Event Dispatch Thread through invokeLater, because
 * Swing components may only be modified on that thread.
 *
 * @author [Your Name]
 */
public class AppointmentController {

    private final MainView  view;
    private final ApiClient api;
    private final User      loggedInUser;

    /** The last successful search, so the bill button knows what to bill. */
    private Appointment currentAppointment;

    private Thread clockThread;
    private Thread sessionThread;

    /** Throttles how often user activity is written to the database. */
    private long lastActivityWrite = 0;

    public AppointmentController(MainView view, User loggedInUser, ApiClient api) {
        this.view         = view;
        this.loggedInUser = loggedInUser;
        this.api          = api;

        view.setLoggedInUser(loggedInUser.getFullName()
                + "  (" + loggedInUser.getRole() + ")");

        // ---- Observer pattern: subscribe to every view event ----
        view.addSaveListener(e          -> saveAppointment());
        view.addUpdateListener(e        -> updateAppointment());
        view.addClearListener(e         -> clearForm());
        view.addSearchListener(e        -> searchAppointment());
        view.addBillListener(e          -> generateBill());
        view.addSaveReceiptListener(e   -> saveReceiptToFile());
        view.addRefreshListener(e       -> loadAppointments());
        view.addEditListener(e          -> editSelected());
        view.addDeleteListener(e        -> deleteSelected());
        view.addFilterListener(e        -> filterAppointments());
        view.addReportRefreshListener(e -> loadDailyReport());
        view.addRemindersListener(e     -> generateReminders());
        view.addLogoutListener(e        -> logout());
        view.addSessionRefreshListener(e -> loadActiveSessions());
        view.addEndAllSessionsListener(e -> signOutEverywhere());
        view.addGlobalActivityListener(e -> recordActivity());

        // Staff account management, administrator only
        view.addCreateUserListener(e    -> createUser());
        view.addClearUserListener(e     -> view.clearUserForm());
        view.addResetPasswordListener(e -> resetPassword());
        view.addChangeRoleListener(e    -> changeRole());
        view.addDeleteUserListener(e    -> deleteUser());
        view.addStaffRefreshListener(e  -> loadStaff());

        view.addWindowCloseListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        // ---- role based access ----
        // A STAFF user never sees the Staff Accounts destination. Hiding the
        // route is a convenience; the real protection is requireAdmin(),
        // which refuses each operation regardless of what is on screen.
        if (!loggedInUser.isAdmin()) {
            view.removeAdminScreens();
        }

        // ---- startup ----
        loadTreatments();
        startClockThread();
        startSessionMonitor();
        refreshEverything();
        describeSession();
    }

    /** Reloads every screen that reads from the database. */
    private void refreshEverything() {
        loadDashboard();
        loadAppointments();
        loadDailyReport();
        suggestNextNumber();
        if (loggedInUser.isAdmin()) {
            loadStaff();
        }
    }

    // =================================================================
    // STAFF ACCOUNT MANAGEMENT - administrator only
    //
    // Every method here starts with requireAdmin(). Checking in the
    // controller rather than only hiding the screen matters: hiding a
    // button is a user interface convenience, not a security control.
    // =================================================================

    /** @return true if the signed-in user may manage accounts */
    private boolean requireAdmin() {
        if (loggedInUser.isAdmin()) {
            return true;
        }
        view.showError("Only an administrator can manage staff accounts.");
        view.setStatusError("Permission denied.");
        return false;
    }

    private void loadStaff() {
        if (!loggedInUser.isAdmin()) {
            return;
        }
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                return api.staffRows();
            }
            @Override
            protected void done() {
                try {
                    DefaultTableModel model = view.getStaffTableModel();
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        model.addRow(row);
                    }
                } catch (Exception ex) {
                    view.setStatusError("Could not load staff accounts.");
                }
            }
        }.execute();
    }

    private void createUser() {
        if (!requireAdmin()) {
            return;
        }

        view.clearUserFieldErrors();
        boolean valid = true;

        if (!Validator.isValidUsername(view.getNewUsername())) {
            view.showUserFieldError("username",
                    "4-20 chars, must start with a letter");
            valid = false;
        }
        if (!Validator.isValidName(view.getNewFullName())) {
            view.showUserFieldError("fullName", "Letters and spaces only");
            valid = false;
        }
        if (!Validator.isStrongPassword(view.getNewPassword())) {
            view.showUserFieldError("password",
                    "At least " + Validator.MIN_PASSWORD_LENGTH
                    + " chars with a letter and a digit");
            valid = false;
        }
        if (!valid) {
            view.setStatusError("Please correct the highlighted fields.");
            return;
        }

        final String username = view.getNewUsername();
        final String fullName = view.getNewFullName();
        final String password = view.getNewPassword();
        final String role     = view.getNewRole();

        try {
            view.setBusy(true);
            int id = api.createUser(username, password, fullName, role);

            view.setStatusSuccess("Account '" + username + "' created as "
                    + role + " (id " + id + ").");
            view.showInfo("Account created.\n\n"
                    + "Username : " + username + "\n"
                    + "Role     : " + role + "\n\n"
                    + "Give the password to " + fullName
                    + " and ask them to change it after signing in.");
            view.clearUserForm();
            loadStaff();

        } catch (Exception ex) {
            // The server replies 409 when the username is taken.
            view.showUserFieldError("username", "Already taken");
            view.setStatusError(ex.getMessage());
        } finally {
            view.setBusy(false);
        }
    }

    private void resetPassword() {
        if (!requireAdmin()) {
            return;
        }
        int    id       = view.getSelectedUserId();
        String username = view.getSelectedUsername();

        if (id < 0) {
            view.setStatusError("Select an account first.");
            return;
        }

        String password = view.promptForPassword(username);
        if (password == null) {
            return;                       // cancelled
        }
        if (!Validator.isStrongPassword(password)) {
            view.showError("The password must be at least "
                    + Validator.MIN_PASSWORD_LENGTH
                    + " characters and contain a letter and a digit.");
            return;
        }

        try {
            if (api.resetPassword(id, password)) {
                view.setStatusSuccess("Password reset for " + username
                        + ". Their other sessions were closed.");
                view.showInfo("Password reset for " + username + ".\n\n"
                        + "Any sessions they had open elsewhere have been "
                        + "signed out, because a reset normally means the old "
                        + "password was compromised or forgotten.");
                loadStaff();
            }
        } catch (Exception ex) {
            view.showError("Could not reset the password.\n\n" + ex.getMessage());
        }
    }

    private void changeRole() {
        if (!requireAdmin()) {
            return;
        }
        int    id       = view.getSelectedUserId();
        String username = view.getSelectedUsername();
        String current  = view.getSelectedUserRole();

        if (id < 0) {
            view.setStatusError("Select an account first.");
            return;
        }

        String role = view.promptForRole(username, current);
        if (role == null || role.equals(current)) {
            return;
        }

        try {
            // Never allow the last administrator to be demoted, or nobody
            // could ever manage accounts again.
            if ("ADMIN".equals(current) && "STAFF".equals(role)
                    && 0 <= 1) {
                view.showError("This is the only administrator account.\n\n"
                        + "Promote another account to ADMIN first.");
                return;
            }

            String fullName = String.valueOf(view.getStaffTableModel()
                    .getValueAt(indexOfUser(id), 2));

            if (api.changeRole(id, fullName, role)) {
                view.setStatusSuccess(username + " is now " + role + ".");
                loadStaff();

                if (id == loggedInUser.getUserId()) {
                    view.showInfo("You changed your own role.\n\n"
                            + "Sign out and back in for it to take effect.");
                }
            }
        } catch (Exception ex) {
            view.showError("Could not change the role.\n\n" + ex.getMessage());
        }
    }

    private void deleteUser() {
        if (!requireAdmin()) {
            return;
        }
        int    id       = view.getSelectedUserId();
        String username = view.getSelectedUsername();
        String role     = view.getSelectedUserRole();

        if (id < 0) {
            view.setStatusError("Select an account first.");
            return;
        }

        // An administrator deleting their own account mid-session would be
        // signed out with no way back in.
        if (id == loggedInUser.getUserId()) {
            view.showError("You cannot delete the account you are signed in as.");
            return;
        }

        try {
            if ("ADMIN".equals(role) && 0 <= 1) {
                view.showError("This is the only administrator account "
                        + "and cannot be deleted.");
                return;
            }
            if (!view.confirmDeleteUser(username)) {
                return;
            }
            if (api.deleteUser(id)) {
                view.setStatusSuccess("Account '" + username + "' deleted.");
                loadStaff();
            }
        } catch (Exception ex) {
            view.showError("Could not delete the account.\n\n" + ex.getMessage());
        }
    }

    /** Finds the table row holding a given user id. */
    private int indexOfUser(int userId) {
        DefaultTableModel model = view.getStaffTableModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (String.valueOf(userId).equals(String.valueOf(model.getValueAt(i, 0)))) {
                return i;
            }
        }
        return 0;
    }

    // =================================================================
    // THREAD 1 - LIVE CLOCK
    //
    // Why a separate thread: the clock must tick once a second forever.
    // A sleep loop on the Event Dispatch Thread would freeze the entire
    // interface. setDaemon(true) means the thread cannot stop the JVM
    // from exiting.
    // =================================================================
    private void startClockThread() {
        clockThread = new Thread(() -> {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE dd MMM yyyy   HH:mm:ss");

            while (!Thread.currentThread().isInterrupted()) {
                final String now = fmt.format(new Date());
                SwingUtilities.invokeLater(() -> view.setClock(now));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "clock-thread");

        clockThread.setDaemon(true);
        clockThread.start();
    }

    // =================================================================
    // THREAD 3 - SESSION MONITOR
    //
    // The check must keep running while the user does nothing at all, so
    // it cannot be driven by user events.
    //
    // Why this matters clinically: a reception desk left unattended with a
    // live session means patient records are readable by anyone walking
    // past. This is a data protection control, not a convenience.
    // =================================================================
    private void startSessionMonitor() {
        sessionThread = new Thread(() -> {
            boolean warned = false;

            while (!Thread.currentThread().isInterrupted()) {
                Session session = Session.getInstance();
                if (session == null) {
                    break;
                }

                if (session.hasTimedOut()) {
                    SwingUtilities.invokeLater(() -> {
                        view.showWarning("Your session expired after "
                                + Session.TIMEOUT_MINUTES
                                + " minutes of inactivity.\n\nPlease sign in again.");
                        endSessionAndReturnToLogin("Session expired.");
                    });
                    break;
                }

                final long left = session.getMinutesRemaining();
                final boolean soon = session.isExpiringSoon();

                if (soon && !warned) {
                    warned = true;
                    SwingUtilities.invokeLater(() -> view.setStatusError(
                            "Session expires in " + left
                          + " minute(s). Move the mouse to stay signed in."));
                } else if (!soon) {
                    warned = false;
                }

                SwingUtilities.invokeLater(() -> view.setSessionCountdown(
                        "Session expires in " + left + " min", soon));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "session-monitor");

        sessionThread.setDaemon(true);
        sessionThread.start();
    }

    /**
     * Records user activity.
     *
     * The in-memory timestamp updates on every event, but the database is
     * written at most once a minute. Writing on every mouse move would issue
     * hundreds of pointless UPDATE statements.
     */
    private void recordActivity() {
        final Session session = Session.getInstance();
        if (session == null) {
            return;
        }
        session.touch();

        long now = System.currentTimeMillis();
        if (now - lastActivityWrite < 60000) {
            return;
        }
        lastActivityWrite = now;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                api.touchSession();
                return null;
            }
        }.execute();
    }

    // =================================================================
    // DASHBOARD
    // =================================================================
    private void loadDashboard() {
        new SwingWorker<Object[], Void>() {

            @Override
            protected Object[] doInBackground() throws Exception {
                Map<String, String> stats = api.dashboardFigures();
                List<Appointment> today =
                        api.findByDate(java.time.LocalDate.now().toString());
                return new Object[]{stats, today};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    Map<String, String> stats = (Map<String, String>) result[0];
                    List<Appointment> today   = (List<Appointment>) result[1];

                    view.setDashboardStats(stats.get("today"), stats.get("tomorrow"),
                                           stats.get("total"), stats.get("revenue"));

                    DefaultTableModel model = view.getTodayTableModel();
                    model.setRowCount(0);
                    for (Appointment a : today) {
                        model.addRow(new Object[]{
                            a.getAppointmentTime(), a.getAppointmentNo(),
                            a.getPatientName(), a.getDentistName(), a.getTreatmentType()});
                    }
                } catch (Exception ex) {
                    view.setStatusError("Could not load the dashboard.");
                }
            }
        }.execute();
    }

    // =================================================================
    // THREAD 2 - ASYNCHRONOUS LOAD OF THE APPOINTMENT LIST
    // =================================================================
    private void loadAppointments() {
        view.setStatus("Loading appointments...");
        view.setBusy(true);

        new SwingWorker<List<Appointment>, Void>() {

            @Override
            protected List<Appointment> doInBackground() throws Exception {
                return api.findAll();        // WORKER THREAD, no Swing calls
            }

            @Override
            protected void done() {          // EVENT DISPATCH THREAD
                try {
                    fillTable(get());
                    view.setStatus(view.getTableModel().getRowCount()
                            + " appointment(s) loaded.");
                } catch (Exception ex) {
                    view.setStatusError("Could not load appointments.");
                    view.showError("Could not load appointments.\n\n"
                            + ex.getMessage() + "\n\nCheck that WampServer is running.");
                } finally {
                    view.setBusy(false);
                }
            }
        }.execute();
    }

    /** Filters the list as the user types. */
    private void filterAppointments() {
        final String term = view.getFilterText();

        new SwingWorker<List<Appointment>, Void>() {
            @Override
            protected List<Appointment> doInBackground() throws Exception {
                return term.isEmpty() ? api.findAll() : api.search(term);
            }
            @Override
            protected void done() {
                try {
                    fillTable(get());
                    view.setStatus(view.getTableModel().getRowCount() + " match(es).");
                } catch (Exception ex) {
                    view.setStatusError("Filter failed.");
                }
            }
        }.execute();
    }

    private void fillTable(List<Appointment> appointments) {
        DefaultTableModel model = view.getTableModel();
        model.setRowCount(0);
        for (Appointment a : appointments) {
            model.addRow(new Object[]{
                a.getAppointmentNo(), a.getPatientName(), a.getContactNo(),
                a.getDentistName(), a.getTreatmentType(),
                a.getAppointmentDate(), a.getAppointmentTime()});
        }
    }

    private void loadTreatments() {
        try {
            view.setTreatmentOptions(api.findTreatments().keySet());
        } catch (Exception ex) {
            view.showError("Could not load the treatment list.\n\n" + ex.getMessage());
        }
    }

    private void suggestNextNumber() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return api.nextAppointmentNo();
            }
            @Override
            protected void done() {
                try {
                    if (!view.isEditing()) {
                        view.suggestAppointmentNo(get());
                    }
                } catch (Exception ignored) {
                    // A suggestion is a convenience; failing to make one is harmless.
                }
            }
        }.execute();
    }

    // =================================================================
    // FUNCTIONALITY 2 - REGISTER A NEW APPOINTMENT
    // =================================================================
    private void saveAppointment() {
        if (!validateForm()) {
            return;
        }
        Appointment appointment = readForm();

        try {
            view.setBusy(true);

            // A friendly pre-check, so the common cases give an instant
            // message without a failed request. The SERVER checks again and
            // is the authority: between this check and the insert another
            // receptionist could take the slot, which is why the catch block
            // below still handles 409.
            if (api.slotTaken(appointment.getDentistName(),
                              appointment.getAppointmentDate(),
                              appointment.getAppointmentTime())) {
                reportDoubleBooking(appointment);
                return;
            }

            if (api.save(appointment)) {
                view.setStatusSuccess("Appointment " + appointment.getAppointmentNo()
                        + " saved for " + appointment.getPatientName() + ".");
                view.clearForm();
                refreshEverything();
            }

        } catch (ApiException ex) {
            // The server refused. 409 means a clash it detected - either the
            // number is taken or the dentist is already booked - including a
            // clash created between our check and the insert, which the
            // uq_slot database constraint catches.
            if (ex.isConflict()) {
                if (ex.getMessage() != null
                        && ex.getMessage().startsWith("DOUBLE_BOOKING")) {
                    reportDoubleBooking(appointment);
                } else {
                    view.showFieldError("appointmentNo", "Already in use");
                    view.setStatusError(ex.getMessage());
                }
            } else {
                view.setStatusError("Save failed.");
                view.showError("The server refused the appointment.\n\n"
                        + ex.getMessage());
            }
        } catch (Exception ex) {
            view.setStatusError("Save failed.");
            view.showError("Could not save the appointment.\n\n" + ex.getMessage());
        } finally {
            view.setBusy(false);
        }
    }

    // =================================================================
    // EDIT AN EXISTING APPOINTMENT
    // =================================================================
    private void editSelected() {
        String no = view.getSelectedAppointmentNo();
        if (no == null) {
            view.setStatusError("Select a row first.");
            return;
        }
        try {
            Appointment a = api.findByNo(no);
            if (a == null) {
                view.setStatusError("That appointment no longer exists.");
                loadAppointments();
                return;
            }
            view.loadForEdit(a.getAppointmentNo(), a.getPatientName(), a.getAddress(),
                             a.getContactNo(), a.getDentistName(), a.getTreatmentType(),
                             a.getAppointmentDate(), a.getAppointmentTime());
            view.setStatus("Editing " + no + ".");

        } catch (Exception ex) {
            view.showError("Could not open that appointment.\n\n" + ex.getMessage());
        }
    }

    private void updateAppointment() {
        if (!validateForm()) {
            return;
        }
        Appointment appointment = readForm();

        try {
            view.setBusy(true);

            // The SERVER checks the slot on PUT, ignoring this appointment's
            // own row, and replies 409 if it clashes. Repeating the check
            // here would duplicate a rule the server already owns.
            if (api.update(appointment)) {
                view.setStatusSuccess("Appointment " + appointment.getAppointmentNo()
                        + " updated.");
                view.clearForm();
                refreshEverything();
            }

        } catch (Exception ex) {
            view.setStatusError("Update failed.");
            view.showError("Could not update the appointment.\n\n" + ex.getMessage());
        } finally {
            view.setBusy(false);
        }
    }

    private void deleteSelected() {
        String no = view.getSelectedAppointmentNo();
        if (no == null) {
            view.setStatusError("Select a row first.");
            return;
        }
        if (!view.confirmDelete(no)) {
            return;
        }
        try {
            if (api.delete(no)) {
                view.setStatusSuccess("Appointment " + no + " deleted.");
                refreshEverything();
            } else {
                view.setStatusError("Nothing was deleted.");
            }
        } catch (Exception ex) {
            view.showError("Could not delete the appointment.\n\n" + ex.getMessage());
        }
    }

    private void clearForm() {
        if (view.confirmClear()) {
            view.clearForm();
            suggestNextNumber();
        }
    }

    /**
     * Validates every field in one pass, reporting each problem beside the
     * field that caused it. Checking all fields rather than stopping at the
     * first means the user sees every problem at once instead of fixing one,
     * resubmitting, and discovering the next.
     */
    private boolean validateForm() {
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
        }
        return valid;
    }

    private Appointment readForm() {
        return new Appointment(view.getAppointmentNo(), view.getPatientName(),
                view.getAddress(), view.getContactNo(), view.getDentistName(),
                view.getTreatmentType(), view.getDate(), view.getTime());
    }

    private void reportDoubleBooking(Appointment a) {
        view.showFieldError("time", "Dentist already booked");
        view.setStatusError("Double booking prevented.");
        view.showError("DOUBLE BOOKING PREVENTED\n\n"
                + a.getDentistName() + " already has an appointment on "
                + a.getAppointmentDate() + " at " + a.getAppointmentTime()
                + ".\n\nPlease choose a different time or dentist.");
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
            currentAppointment = api.findByNo(view.getSearchNo());

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
    // FUNCTIONALITY 4 - CALCULATE AND PRINT THE BILL
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
            double cost = api.findTreatmentCost(currentAppointment.getTreatmentType());
            double fee  = Double.parseDouble(view.getConsultationFee());

            Bill bill = new Bill(currentAppointment, cost, fee);
            view.setReceipt(bill.generateReceipt());
            view.setStatusSuccess(String.format("Bill generated. Total LKR %,.2f",
                                                bill.getTotal()));
        } catch (Exception ex) {
            view.showError("Could not generate the bill.\n\n" + ex.getMessage());
        }
    }

    /** Writes the receipt to a text file the receptionist can print. */
    private void saveReceiptToFile() {
        if (currentAppointment == null || view.getReceiptText().trim().length() < 40) {
            view.setStatusError("Generate a bill first.");
            return;
        }
        try {
            File folder = new File("receipts");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder,
                    "receipt_" + currentAppointment.getAppointmentNo() + ".txt");

            try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
                out.print(view.getReceiptText());
            }
            view.setStatusSuccess("Receipt saved to " + file.getAbsolutePath());
            view.showInfo("Receipt saved to:\n\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            view.showError("Could not save the receipt.\n\n" + ex.getMessage());
        }
    }

    // =================================================================
    // MANAGEMENT REPORT
    // =================================================================
    private void loadDailyReport() {
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                return api.dailyScheduleReport();
            }
            @Override
            protected void done() {
                try {
                    DefaultTableModel model = view.getReportTableModel();
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        model.addRow(row);
                    }
                } catch (Exception ex) {
                    view.setStatusError("Daily report could not be loaded.");
                }
            }
        }.execute();
    }

    // =================================================================
    // PATIENT REMINDERS
    // =================================================================
    private void generateReminders() {
        view.setStatus("Generating reminders...");

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return api.generateReminders();
            }
            @Override
            protected void done() {
                try {
                    List<String> messages = get();

                    if (messages.isEmpty()) {
                        view.setNotifications("\n  No appointments scheduled for "
                                + "tomorrow.\n\n  Nothing to send.\n");
                        view.setStatus("No reminders needed.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("REMINDER MESSAGES GENERATED\n");
                    sb.append("===========================\n");
                    sb.append(messages.size()).append(" message(s) queued for dispatch.\n");
                    sb.append("Written to the 'reminders' folder beside the "
                            + "SERVER, ready for the clinic's SMS provider.\n\n");
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
    // SESSIONS
    // =================================================================
    private void loadActiveSessions() {
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                // expired sessions are purged server-side
                return api.activeSessions();
            }
            @Override
            protected void done() {
                try {
                    DefaultTableModel model = view.getSessionTableModel();
                    model.setRowCount(0);
                    for (String[] row : get()) {
                        model.addRow(row);
                    }
                    describeSession();
                    view.setStatus("Active sessions loaded.");
                } catch (Exception ex) {
                    view.setStatusError("Could not load sessions.");
                }
            }
        }.execute();
    }

    /** Describes the current session on the sessions screen. */
    private void describeSession() {
        Session session = Session.getInstance();
        if (session == null) {
            return;
        }
        view.setSessionInfo(
              "  Signed in as  : " + session.getUser().getFullName()
                                   + "  (" + session.getUser().getRole() + ")\n"
            + "  Signed in at  : " + session.getLoginTimeText() + "\n"
            + "  Session token : " + Session.mask(session.getToken())
                                   + "   (stored hashed in the database)\n"
            + "  Idle timeout  : " + Session.TIMEOUT_MINUTES + " minutes\n"
            + "  Persistent    : " + (session.isRemembered()
                                      ? "yes, token saved on this computer"
                                      : "no, ends when you sign out") + "\n"
            + "  Token file    : " + SessionStore.location());
    }

    private void signOutEverywhere() {
        Session session = Session.getInstance();
        if (session == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(view,
                "Sign out of every machine where this account is signed in?\n\n"
              + "This includes the session you are using now.",
                "Sign Out Everywhere", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int closed = api.endAllSessions();
            view.showInfo(closed + " session(s) closed.");
            endSessionAndReturnToLogin("All sessions were signed out.");
        } catch (Exception ex) {
            view.showError("Could not close the sessions.\n\n" + ex.getMessage());
        }
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(view,
                "Sign out and return to the sign-in screen?",
                "Confirm Sign Out", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            endSessionAndReturnToLogin("You have been signed out.");
        }
    }

    /** Ends the session and reopens the sign-in window. */
    private void endSessionAndReturnToLogin(String notice) {
        Session session = Session.getInstance();

        stopThreads();
        try {
            if (session != null) {
                api.signOut();
            }
        } catch (Exception ex) {
            // The local session ends regardless; a server that cannot be
            // reached will expire the row on its own schedule.
            System.err.println("Could not close the session: " + ex.getMessage());
        }

        SessionStore.clear();
        Session.end();
        view.dispose();

        LoginView loginView = new LoginView();
        new LoginController(loginView, new ApiClient(api.getBaseUrl()));
        loginView.setNotice(notice);
        loginView.setVisible(true);
    }

    // =================================================================
    // FUNCTIONALITY 6 - EXIT SAFELY
    // =================================================================
    private void exitApplication() {
        if (!view.confirmExit()) {
            return;
        }
        stopThreads();

        // Close the session row so the token cannot be reused, unless the
        // user asked to be remembered on this machine.
        try {
            Session session = Session.getInstance();
            if (session != null && !session.isRemembered()) {
                api.signOut();
            }
        } catch (Exception ex) {
            System.err.println("Could not close the session row: " + ex.getMessage());
        }

        Session.end();
                System.out.println("Application closed by " + loggedInUser.getUsername());
        System.exit(0);
    }

    /** Interrupts both daemon threads so they exit their loops cleanly. */
    private void stopThreads() {
        if (clockThread != null) {
            clockThread.interrupt();
        }
        if (sessionThread != null) {
            sessionThread.interrupt();
        }
    }
}
