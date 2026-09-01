package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * PRESENTATION TIER - main application window.
 *
 * Six tabs cover the functionalities required by the scenario plus the
 * reporting and notification features added in version 1.1.
 *
 * LAYOUT: only BorderLayout, GridLayout and FlowLayout are used. An earlier
 * version used GridBagLayout and shipped a defect where text fields rendered
 * at zero width. The simpler layouts remove that whole class of problem,
 * because GridLayout stretches each component to fill its cell automatically.
 *
 * USABILITY (version 1.2): validation errors now appear beside the field that
 * caused them, in red, with the field itself tinted. Previously every error
 * was a modal dialog, which meant the user had to read the message, dismiss
 * it, then remember which field it referred to. Inline messages remove that
 * memory step and let the user see several problems at once.
 *
 * @author [Your Name]
 */
public class MainView extends JFrame {

    // ---------- Tab 1: register ----------
    private final JTextField txtAppointmentNo = new JTextField();
    private final JTextField txtPatientName   = new JTextField();
    private final JTextField txtAddress       = new JTextField();
    private final JTextField txtContactNo     = new JTextField();
    private final JComboBox<String> cmbDentist = new JComboBox<>(new String[]{
            "Dr. Fernando", "Dr. Silva", "Dr. Perera", "Dr. Wickramasinghe"});
    private final JComboBox<String> cmbTreatment = new JComboBox<>();
    private final JTextField txtDate = new JTextField();
    private final JTextField txtTime = new JTextField();

    private final JButton btnSave     = Theme.primaryButton("Save Appointment", 'S',
            "Save this appointment (Alt+S)");
    private final JButton btnClear    = Theme.button("Clear Form", 'C',
            "Empty every field (Alt+C)");
    private final JButton btnToday    = Theme.button("Today", 'T', "Fill in today's date");
    private final JButton btnTomorrow = Theme.button("Tomorrow", 'M', "Fill in tomorrow's date");

    /** One error label per field, so messages appear where the problem is. */
    private final Map<String, JLabel> errorLabels = new LinkedHashMap<>();
    private final Map<String, JComponent> fields  = new LinkedHashMap<>();

    // ---------- Tab 2: search and bill ----------
    private final JTextField txtSearchNo        = new JTextField(10);
    private final JButton    btnSearch          = Theme.primaryButton("Search", 'R',
            "Find this appointment (Alt+R, or press Enter)");
    private final JTextArea  txtDetails         = new JTextArea();
    private final JTextField txtConsultationFee = new JTextField("1500", 8);
    private final JButton    btnBill            = Theme.primaryButton("Calculate Bill", 'B',
            "Work out the total and print the receipt (Alt+B)");
    private final JTextArea  txtReceipt         = new JTextArea();

    // ---------- Tab 3: all appointments ----------
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Appt No", "Patient", "Contact", "Dentist",
                         "Treatment", "Date", "Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final StripedTable tblAppointments = new StripedTable(tableModel);
    private final JButton btnRefresh = Theme.button("Refresh", 'F',
            "Reload the list from the database (Alt+F)");

    // ---------- Tab 4: daily summary ----------
    private final DefaultTableModel reportModel = new DefaultTableModel(
            new String[]{"Date", "Dentist", "Appointments", "Expected Revenue (LKR)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final StripedTable tblReport = new StripedTable(reportModel);
    private final JButton btnReportRefresh = Theme.button("Refresh Report", 'P',
            "Recalculate the daily summary (Alt+P)");

    // ---------- Tab 5: reminders ----------
    private final JTextArea txtNotifications = new JTextArea();
    private final JButton   btnSendReminders = Theme.primaryButton(
            "Generate Tomorrow's Reminders", 'G', "Build reminder messages (Alt+G)");

    // ---------- status bar ----------
    private final JLabel  lblStatus = new JLabel("  Ready");
    private final JLabel  lblUser   = new JLabel("", SwingConstants.CENTER);
    private final JLabel  lblClock  = new JLabel();
    private final JButton btnLogout = Theme.button("Logout", 'L',
            "End this session and return to the login screen (Alt+L)");

    private final JTabbedPane tabs = new JTabbedPane();

    /** Kept so clicking a table row can trigger the same action as the button. */
    private ActionListener searchListener;

    public MainView() {
        setTitle("Sunrise Dental Clinic - Appointment Management System");
        setSize(940, 690);
        setMinimumSize(new Dimension(860, 620));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        tabs.setFont(Theme.LABEL);
        tabs.addTab("Register Appointment", buildRegisterPanel());
        tabs.addTab("Search & Bill",        buildBillPanel());
        tabs.addTab("All Appointments",     buildListPanel());
        tabs.addTab("Daily Summary",        buildSummaryPanel());
        tabs.addTab("Reminders",            buildNotificationPanel());
        tabs.addTab("Help",                 buildHelpPanel());

        add(buildHeader(),    BorderLayout.NORTH);
        add(tabs,             BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        wireConvenienceKeys();
    }

    /** Clinic banner, so the window is identifiable at a glance. */
    private JPanel buildHeader() {
        JLabel title = new JLabel("  Sunrise Dental Clinic");
        title.setFont(Theme.TITLE);
        title.setForeground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BRAND);
        panel.setBorder(Theme.pad(10, 10, 10, 10));
        panel.add(title, BorderLayout.WEST);
        return panel;
    }

    // =================================================================
    // TAB 1 - register a new appointment
    //
    // Three columns: label, input, message. The third column holds the
    // helper text, and the same label switches to a red error message
    // when validation fails, so feedback appears exactly where the
    // problem is rather than in a dialog the user must dismiss.
    // =================================================================
    private JPanel buildRegisterPanel() {

        JPanel form = new JPanel(new GridLayout(8, 3, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Patient and Appointment Details"));

        addRow(form, "appointmentNo", "Appointment No:", txtAppointmentNo,
               "format APT1001");
        addRow(form, "patientName",   "Patient Name:",   txtPatientName,
               "letters and spaces");
        addRow(form, "address",       "Address:",        txtAddress,
               "optional");
        addRow(form, "contactNo",     "Contact No:",     txtContactNo,
               "10 digits, e.g. 0771234567");
        addRow(form, "dentistName",   "Dentist:",        cmbDentist,
               "");
        addRow(form, "treatmentType", "Treatment Type:", cmbTreatment,
               "price comes from the database");
        addRow(form, "date",          "Date:",           txtDate,
               "yyyy-MM-dd");
        addRow(form, "time",          "Time:",           txtTime,
               "HH:mm, clinic open 08:00-20:00");

        // quick date fill, so staff booking for tomorrow need not type a date
        JPanel dateHelpers = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dateHelpers.add(Theme.hint("Quick fill:"));
        dateHelpers.add(btnToday);
        dateHelpers.add(btnTomorrow);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        buttons.add(btnSave);
        buttons.add(btnClear);

        JPanel south = new JPanel(new BorderLayout());
        south.add(dateHelpers, BorderLayout.NORTH);
        south.add(buttons,     BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(Theme.pad(15, 25, 10, 25));
        panel.add(form,  BorderLayout.NORTH);
        panel.add(south, BorderLayout.CENTER);
        return panel;
    }

    /** Adds one label / input / message row and registers it for error display. */
    private void addRow(JPanel form, String key, String label,
                        JComponent field, String hint) {
        field.setFont(Theme.LABEL);
        if (field instanceof JTextField) {
            field.setBorder(Theme.FIELD_BORDER);
        }

        JLabel message = Theme.hint(hint);

        form.add(Theme.formLabel(label));
        form.add(field);
        form.add(message);

        fields.put(key, field);
        errorLabels.put(key, message);
    }

    // =================================================================
    // TAB 2 - search and bill
    // =================================================================
    private JPanel buildBillPanel() {

        txtSearchNo.setFont(Theme.LABEL);
        txtSearchNo.setBorder(Theme.FIELD_BORDER);
        txtSearchNo.setToolTipText("Type an appointment number such as APT1001");

        txtConsultationFee.setFont(Theme.LABEL);
        txtConsultationFee.setBorder(Theme.FIELD_BORDER);
        txtConsultationFee.setToolTipText("Fee charged for this consultation, in rupees");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        top.setBorder(BorderFactory.createTitledBorder("Find an appointment, then bill it"));
        top.add(new JLabel("Appointment No:"));
        top.add(txtSearchNo);
        top.add(btnSearch);
        top.add(Theme.hint("        "));
        top.add(new JLabel("Consultation Fee (LKR):"));
        top.add(txtConsultationFee);
        top.add(btnBill);

        txtDetails.setEditable(false);
        txtDetails.setFont(Theme.MONO);
        txtDetails.setBorder(BorderFactory.createTitledBorder("Appointment Details"));
        txtDetails.setText("\n   Search for an appointment number to see the patient record here.\n");

        txtReceipt.setEditable(false);
        txtReceipt.setFont(Theme.MONO);
        txtReceipt.setBorder(BorderFactory.createTitledBorder("Patient Receipt"));
        txtReceipt.setText("\n   The receipt will appear here once you calculate the bill.\n");

        JPanel centre = new JPanel(new GridLayout(2, 1, 5, 5));
        centre.add(new JScrollPane(txtDetails));
        centre.add(new JScrollPane(txtReceipt));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top,    BorderLayout.NORTH);
        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // =================================================================
    // TAB 3 - every appointment
    // =================================================================
    private JPanel buildListPanel() {

        tblAppointments.width(0, 80);
        tblAppointments.width(1, 160);
        tblAppointments.width(4, 140);

        // Double clicking a row jumps to the billing tab with that record
        // loaded. Retyping a number the user can already see on screen is
        // wasted effort and a chance to mistype.
        tblAppointments.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedInBilling();
                }
            }
        });

        JLabel hint = Theme.hint("  Double click a row to open it in Search & Bill."
                + "  Click a column heading to sort.");

        JPanel south = new JPanel();
        south.add(btnRefresh);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(hint, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblAppointments), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /** Copies the selected appointment number into the billing tab and searches. */
    private void openSelectedInBilling() {
        int row = tblAppointments.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = tblAppointments.convertRowIndexToModel(row);
        String number = String.valueOf(tableModel.getValueAt(modelRow, 0));

        txtSearchNo.setText(number);
        tabs.setSelectedIndex(1);

        if (searchListener != null) {
            searchListener.actionPerformed(new ActionEvent(this, 0, "search"));
        }
    }

    // =================================================================
    // TAB 4 - management summary
    // =================================================================
    private JPanel buildSummaryPanel() {

        tblReport.rightAlign(2);
        tblReport.rightAlign(3);

        JLabel note = Theme.hint(
            "  Appointments and expected treatment revenue per dentist per day.");

        JPanel south = new JPanel();
        south.add(btnReportRefresh);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(note, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblReport), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // =================================================================
    // TAB 5 - patient reminders
    // =================================================================
    private JPanel buildNotificationPanel() {

        txtNotifications.setEditable(false);
        txtNotifications.setFont(Theme.MONO);
        txtNotifications.setText(
            "\n  No reminders generated yet.\n\n"
          + "  Click the button below to build reminder messages for every\n"
          + "  patient with an appointment tomorrow. Messages are written to\n"
          + "  the 'reminders' folder inside the project, ready for the\n"
          + "  clinic's SMS provider or a mail merge.\n");

        JPanel south = new JPanel();
        south.add(btnSendReminders);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(txtNotifications), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // =================================================================
    // TAB 6 - help for new staff
    // =================================================================
    private JPanel buildHelpPanel() {
        JTextArea help = new JTextArea(
            "SUNRISE DENTAL CLINIC - SYSTEM USER GUIDE\n"
          + "=========================================\n\n"
          + "KEYBOARD SHORTCUTS\n"
          + "   Alt+S  Save appointment      Alt+C  Clear form\n"
          + "   Alt+R  Search                Alt+B  Calculate bill\n"
          + "   Alt+F  Refresh list          Alt+G  Generate reminders\n"
          + "   Alt+L  Logout                F5     Refresh current tab\n"
          + "   Enter  Submits the form you are currently in\n\n"
          + "1. LOGGING IN\n"
          + "   Enter the username and password issued to you by the clinic\n"
          + "   administrator. Only authorised staff accounts can open the\n"
          + "   system.\n\n"
          + "2. REGISTERING A NEW APPOINTMENT\n"
          + "   a. Open the 'Register Appointment' tab.\n"
          + "   b. Appointment No: APT followed by four digits (APT1001).\n"
          + "      Each number must be unique.\n"
          + "   c. Patient Name: letters and spaces only.\n"
          + "   d. Address: optional but recommended.\n"
          + "   e. Contact No: exactly ten digits starting with 0.\n"
          + "   f. Dentist and Treatment Type: choose from the dropdowns.\n"
          + "   g. Date: yyyy-MM-dd. Use the Today or Tomorrow buttons to\n"
          + "      fill it in without typing.\n"
          + "   h. Time: HH:mm on a 24-hour clock, between 08:00 and 20:00.\n"
          + "   i. Click 'Save Appointment' or press Alt+S.\n\n"
          + "   If something is wrong, the message appears in red beside the\n"
          + "   field that needs fixing, and the field is highlighted. You do\n"
          + "   not have to dismiss a dialog to see which field it was.\n\n"
          + "   The system refuses to save if the chosen dentist already has\n"
          + "   an appointment at that exact date and time. This prevents the\n"
          + "   double bookings that occurred with the old paper diary.\n\n"
          + "3. FINDING AN APPOINTMENT\n"
          + "   Open 'Search & Bill', type the appointment number and press\n"
          + "   Enter. Alternatively open 'All Appointments' and double click\n"
          + "   any row, which opens that record in the billing tab for you.\n\n"
          + "4. PRINTING A PATIENT BILL\n"
          + "   After searching, enter the consultation fee and click\n"
          + "   'Calculate Bill'. The receipt shows the treatment cost, the\n"
          + "   consultation fee and the total payable in rupees.\n\n"
          + "5. VIEWING ALL APPOINTMENTS\n"
          + "   The 'All Appointments' tab loads automatically in the\n"
          + "   background. Click any column heading to sort by that column.\n\n"
          + "6. DAILY SUMMARY\n"
          + "   Shows how many appointments each dentist has on each day and\n"
          + "   the treatment revenue expected. Useful for spotting a dentist\n"
          + "   who is over-booked.\n\n"
          + "7. PATIENT REMINDERS\n"
          + "   Generates a reminder message for every patient booked for\n"
          + "   tomorrow and writes them to a dispatch file.\n\n"
          + "8. LOGGING OUT AND CLOSING\n"
          + "   'Logout' ends your session and returns to the login screen,\n"
          + "   leaving the system running for the next member of staff.\n"
          + "   Closing the window shuts the system down completely.\n\n"
          + "TROUBLESHOOTING\n"
          + "   'Cannot connect to database' - ask the administrator to check\n"
          + "   that the WAMP server is running (tray icon must be green).\n\n"
          + "   'Double booking' - that dentist is already busy at that time.\n"
          + "   Choose a different time or a different dentist.\n");
        help.setEditable(false);
        help.setFont(Theme.LABEL);
        help.setCaretPosition(0);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(help), BorderLayout.CENTER);
        return panel;
    }

    // =================================================================
    // STATUS BAR
    // =================================================================
    private JPanel buildStatusBar() {
        lblStatus.setFont(Theme.LABEL);
        lblUser.setFont(Theme.LABEL);
        lblClock.setFont(Theme.LABEL);
        lblClock.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel labels = new JPanel(new GridLayout(1, 3));
        labels.add(lblStatus);
        labels.add(lblUser);
        labels.add(lblClock);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEtchedBorder());
        bar.add(labels,    BorderLayout.CENTER);
        bar.add(btnLogout, BorderLayout.EAST);
        return bar;
    }

    // =================================================================
    // KEYBOARD CONVENIENCE
    // =================================================================
    private void wireConvenienceKeys() {

        // Enter in the search box does the same as clicking Search.
        txtSearchNo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && searchListener != null) {
                    searchListener.actionPerformed(new ActionEvent(this, 0, "search"));
                }
            }
        });

        // Quick date fill.
        btnToday.addActionListener(e -> {
            txtDate.setText(LocalDate.now().toString());
            clearFieldError("date");
        });
        btnTomorrow.addActionListener(e -> {
            txtDate.setText(LocalDate.now().plusDays(1).toString());
            clearFieldError("date");
        });

        // Typing in a field clears its error, so old messages do not linger.
        for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
            if (entry.getValue() instanceof JTextField) {
                final String key = entry.getKey();
                ((JTextField) entry.getValue()).addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        clearFieldError(key);
                    }
                });
            }
        }
    }

    // =================================================================
    // GETTERS
    // =================================================================
    public String getAppointmentNo()   { return txtAppointmentNo.getText().trim(); }
    public String getPatientName()     { return txtPatientName.getText().trim(); }
    public String getAddress()         { return txtAddress.getText().trim(); }
    public String getContactNo()       { return txtContactNo.getText().trim(); }
    public String getDentistName()     { return (String) cmbDentist.getSelectedItem(); }
    public String getTreatmentType()   { return (String) cmbTreatment.getSelectedItem(); }
    public String getDate()            { return txtDate.getText().trim(); }
    public String getTime()            { return txtTime.getText().trim(); }
    public String getSearchNo()        { return txtSearchNo.getText().trim(); }
    public String getConsultationFee() { return txtConsultationFee.getText().trim(); }

    public DefaultTableModel getTableModel()       { return tableModel; }
    public DefaultTableModel getReportTableModel() { return reportModel; }

    /** True when the register form has anything typed into it. */
    public boolean isRegisterFormDirty() {
        return !getAppointmentNo().isEmpty() || !getPatientName().isEmpty()
            || !getAddress().isEmpty()       || !getContactNo().isEmpty()
            || !getDate().isEmpty()          || !getTime().isEmpty();
    }

    // =================================================================
    // INLINE VALIDATION FEEDBACK
    // =================================================================

    /**
     * Shows a red message beside one field and tints the field itself.
     * Called by the controller instead of opening an error dialog.
     */
    public void showFieldError(String fieldKey, String message) {
        JLabel label = errorLabels.get(fieldKey);
        if (label != null) {
            label.setText(message);
            label.setForeground(Theme.ERROR);
            label.setFont(Theme.SMALL);
        }
        JComponent field = fields.get(fieldKey);
        if (field instanceof JTextField) {
            field.setBackground(Theme.FIELD_ERROR);
            field.setBorder(Theme.FIELD_BORDER_ERROR);
            field.requestFocus();
        }
    }

    /** Restores one field to its normal appearance. */
    public void clearFieldError(String fieldKey) {
        JLabel label = errorLabels.get(fieldKey);
        if (label != null && label.getForeground().equals(Theme.ERROR)) {
            label.setText("");
            label.setForeground(Theme.TEXT_MUTED);
        }
        JComponent field = fields.get(fieldKey);
        if (field instanceof JTextField) {
            field.setBackground(Theme.FIELD_OK);
            field.setBorder(Theme.FIELD_BORDER);
        }
    }

    /** Clears every field error before a fresh validation pass. */
    public void clearAllFieldErrors() {
        for (String key : errorLabels.keySet()) {
            clearFieldError(key);
        }
    }

    // =================================================================
    // SETTERS
    // =================================================================
    public void setTreatmentOptions(Collection<String> types) {
        cmbTreatment.removeAllItems();
        for (String t : types) {
            cmbTreatment.addItem(t);
        }
    }

    /** Pre-fills the next free appointment number so staff need not invent one. */
    public void suggestAppointmentNo(String number) {
        if (txtAppointmentNo.getText().trim().isEmpty()) {
            txtAppointmentNo.setText(number);
        }
    }

    public void setDetails(String text) {
        txtDetails.setText(text);
        txtDetails.setCaretPosition(0);
    }

    public void setReceipt(String text) {
        txtReceipt.setText(text);
        txtReceipt.setCaretPosition(0);
    }

    public void setNotifications(String text) {
        txtNotifications.setText(text);
        txtNotifications.setCaretPosition(0);
    }

    /** Neutral status message. */
    public void setStatus(String text) {
        lblStatus.setForeground(Color.BLACK);
        lblStatus.setText("  " + text);
    }

    /** Green status, used to confirm something worked. */
    public void setStatusSuccess(String text) {
        lblStatus.setForeground(Theme.SUCCESS);
        lblStatus.setText("  " + text);
    }

    /** Red status, used when something failed. */
    public void setStatusError(String text) {
        lblStatus.setForeground(Theme.ERROR);
        lblStatus.setText("  " + text);
    }

    public void setClock(String text)        { lblClock.setText(text + "   "); }
    public void setLoggedInUser(String name) { lblUser.setText("Logged in as: " + name); }

    /**
     * Shows a wait cursor while a background task runs, so the user can see
     * that the system is working rather than wondering whether their click
     * registered.
     */
    public void setBusy(boolean busy) {
        setCursor(Cursor.getPredefinedCursor(
                busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void clearForm() {
        clearAllFieldErrors();
        txtAppointmentNo.setText("");
        txtPatientName.setText("");
        txtAddress.setText("");
        txtContactNo.setText("");
        txtDate.setText("");
        txtTime.setText("");
        cmbDentist.setSelectedIndex(0);
        if (cmbTreatment.getItemCount() > 0) {
            cmbTreatment.setSelectedIndex(0);
        }
        txtAppointmentNo.requestFocus();
    }

    /** Asks before discarding typed data. */
    public boolean confirmClear() {
        if (!isRegisterFormDirty()) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this,
                "Clear the form? Anything you have typed will be lost.",
                "Confirm Clear", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmExit() {
        return JOptionPane.showConfirmDialog(this,
                "Close the Appointment Management System?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** Moves the user to the registration tab and focuses the first field. */
    public void focusRegisterTab() {
        tabs.setSelectedIndex(0);
        txtAppointmentNo.requestFocus();
    }

    // =================================================================
    // OBSERVER PATTERN
    // =================================================================
    public void addSaveListener(ActionListener l)    { btnSave.addActionListener(l); }
    public void addClearListener(ActionListener l)   { btnClear.addActionListener(l); }
    public void addBillListener(ActionListener l)    { btnBill.addActionListener(l); }
    public void addRefreshListener(ActionListener l) { btnRefresh.addActionListener(l); }
    public void addReportRefreshListener(ActionListener l) { btnReportRefresh.addActionListener(l); }
    public void addRemindersListener(ActionListener l) { btnSendReminders.addActionListener(l); }
    public void addLogoutListener(ActionListener l)  { btnLogout.addActionListener(l); }

    /** Search is also triggered by Enter and by double clicking a table row. */
    public void addSearchListener(ActionListener l) {
        this.searchListener = l;
        btnSearch.addActionListener(l);
    }

    public void addWindowCloseListener(WindowAdapter adapter) {
        addWindowListener(adapter);
    }
}
