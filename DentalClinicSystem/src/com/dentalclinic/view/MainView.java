package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * PRESENTATION TIER - main application window.
 *
 * Four tabs cover the six functionalities required by the scenario:
 *   Tab 1  Register Appointment   -> functionality 2
 *   Tab 2  Search and Bill        -> functionalities 3 and 4
 *   Tab 3  All Appointments       -> reporting
 *   Tab 4  Help                   -> functionality 5
 *   Window close handler          -> functionality 6
 *
 * The status bar shows a live clock updated by a background thread that
 * lives in AppointmentController.
 */
public class MainView extends JFrame {

    // ---------- Tab 1: register ----------
    private final JTextField txtAppointmentNo = new JTextField(20);
    private final JTextField txtPatientName   = new JTextField(20);
    private final JTextField txtAddress       = new JTextField(20);
    private final JTextField txtContactNo     = new JTextField(20);
    private final JComboBox<String> cmbDentist   = new JComboBox<>(new String[]{
            "Dr. Fernando", "Dr. Silva", "Dr. Perera", "Dr. Wickramasinghe"});
    private final JComboBox<String> cmbTreatment = new JComboBox<>();
    private final JTextField txtDate = new JTextField(20);
    private final JTextField txtTime = new JTextField(20);
    private final JButton btnSave  = new JButton("Save Appointment");
    private final JButton btnClear = new JButton("Clear Form");

    // ---------- Tab 2: search and bill ----------
    private final JTextField txtSearchNo        = new JTextField(10);
    private final JButton    btnSearch          = new JButton("Search");
    private final JTextArea  txtDetails         = new JTextArea(9, 44);
    private final JTextField txtConsultationFee = new JTextField("1500", 8);
    private final JButton    btnBill            = new JButton("Calculate & Print Bill");
    private final JTextArea  txtReceipt         = new JTextArea(16, 44);

    // ---------- Tab 3: reports ----------
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Appt No", "Patient", "Contact", "Dentist",
                         "Treatment", "Date", "Time"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable  tblAppointments = new JTable(tableModel);
    private final JButton btnRefresh      = new JButton("Refresh from Database");

    // ---------- status bar ----------
    private final JLabel lblStatus = new JLabel("  Ready");
    private final JLabel lblUser   = new JLabel("", SwingConstants.CENTER);
    private final JLabel lblClock  = new JLabel();

    public MainView() {
        setTitle("Sunrise Dental Clinic - Appointment Management System");
        setSize(920, 680);
        setMinimumSize(new Dimension(820, 600));
        // Controller decides what happens on close (functionality 6)
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Register Appointment", buildRegisterPanel());
        tabs.addTab("Search & Bill",        buildBillPanel());
        tabs.addTab("All Appointments",     buildReportPanel());
        tabs.addTab("Help",                 buildHelpPanel());

        add(tabs,             BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // =================================================================
    // TAB 1
    // =================================================================
    private JPanel buildRegisterPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("New Patient Appointment"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 10, 7, 10);
        gc.anchor = GridBagConstraints.WEST;

        addRow(form, gc, 0, "Appointment No:",  txtAppointmentNo, "format APT1001");
        addRow(form, gc, 1, "Patient Name:",    txtPatientName,   "letters and spaces only");
        addRow(form, gc, 2, "Address:",         txtAddress,       "optional");
        addRow(form, gc, 3, "Contact No:",      txtContactNo,     "10 digits, e.g. 0771234567");
        addRow(form, gc, 4, "Dentist:",         cmbDentist,       "");
        addRow(form, gc, 5, "Treatment Type:",  cmbTreatment,     "loaded from database");
        addRow(form, gc, 6, "Date:",            txtDate,          "yyyy-MM-dd, e.g. 2026-06-15");
        addRow(form, gc, 7, "Time:",            txtTime,          "HH:mm 24-hour, 08:00-20:00");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        buttons.add(btnSave);
        buttons.add(btnClear);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form,    BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }

    /** Helper so the form code stays readable instead of 40 repeated lines. */
    private void addRow(JPanel panel, GridBagConstraints gc, int row,
                        String label, java.awt.Component field, String hint) {
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel(label), gc);

        gc.gridx = 1;
        panel.add(field, gc);

        gc.gridx = 2;
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hintLabel.setForeground(Color.GRAY);
        panel.add(hintLabel, gc);
    }

    // =================================================================
    // TAB 2
    // =================================================================
    private JPanel buildBillPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        top.setBorder(BorderFactory.createTitledBorder("Find Appointment"));
        top.add(new JLabel("Appointment No:"));
        top.add(txtSearchNo);
        top.add(btnSearch);
        top.add(new JLabel("      Consultation Fee (LKR):"));
        top.add(txtConsultationFee);
        top.add(btnBill);

        txtDetails.setEditable(false);
        txtDetails.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtDetails.setBorder(BorderFactory.createTitledBorder("Appointment Details"));

        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtReceipt.setBorder(BorderFactory.createTitledBorder("Patient Receipt"));

        JPanel centre = new JPanel(new java.awt.GridLayout(2, 1, 6, 6));
        centre.add(new JScrollPane(txtDetails));
        centre.add(new JScrollPane(txtReceipt));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top,    BorderLayout.NORTH);
        panel.add(centre, BorderLayout.CENTER);
        return panel;
    }

    // =================================================================
    // TAB 3
    // =================================================================
    private JPanel buildReportPanel() {
        tblAppointments.setRowHeight(24);
        tblAppointments.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tblAppointments.setAutoCreateRowSorter(true);

        JPanel south = new JPanel();
        south.add(btnRefresh);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(tblAppointments), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // =================================================================
    // TAB 4 - functionality 5, step-by-step instructions for new staff
    // =================================================================
    private JPanel buildHelpPanel() {
        JTextArea help = new JTextArea(
            "SUNRISE DENTAL CLINIC - SYSTEM USER GUIDE\n"
          + "=========================================\n\n"
          + "1. LOGGING IN\n"
          + "   Enter the username and password issued to you by the clinic\n"
          + "   administrator, then click Login (or press Enter). Only\n"
          + "   authorised staff accounts can open the system.\n\n"
          + "2. REGISTERING A NEW APPOINTMENT\n"
          + "   a. Open the 'Register Appointment' tab.\n"
          + "   b. Appointment No: type APT followed by four digits (APT1001).\n"
          + "      Each number must be unique.\n"
          + "   c. Patient Name: letters, spaces, full stops and hyphens only.\n"
          + "   d. Address: optional but recommended.\n"
          + "   e. Contact No: exactly ten digits starting with 0.\n"
          + "   f. Dentist: choose from the dropdown.\n"
          + "   g. Treatment Type: choose from the dropdown. The price list is\n"
          + "      read from the database, so it is always current.\n"
          + "   h. Date: yyyy-MM-dd, for example 2026-06-15. Past dates are\n"
          + "      rejected.\n"
          + "   i. Time: HH:mm on a 24-hour clock, between 08:00 and 20:00.\n"
          + "   j. Click 'Save Appointment'.\n\n"
          + "   The system will refuse to save if the chosen dentist already\n"
          + "   has an appointment at that exact date and time. This prevents\n"
          + "   the double bookings that occurred with the old paper diary.\n\n"
          + "3. FINDING AN APPOINTMENT\n"
          + "   Open 'Search & Bill', type the appointment number and click\n"
          + "   Search. The full patient record appears in the upper panel.\n\n"
          + "4. PRINTING A PATIENT BILL\n"
          + "   After searching, enter the consultation fee and click\n"
          + "   'Calculate & Print Bill'. The receipt appears in the lower\n"
          + "   panel showing the treatment cost, the consultation fee and\n"
          + "   the total payable in Sri Lankan Rupees.\n\n"
          + "5. VIEWING ALL APPOINTMENTS\n"
          + "   Open the 'All Appointments' tab. The list loads automatically\n"
          + "   in the background. Click 'Refresh from Database' at any time\n"
          + "   to reload. Click any column heading to sort by that column.\n\n"
          + "6. CLOSING THE SYSTEM\n"
          + "   Click the X on the window. You will be asked to confirm.\n"
          + "   The database connection is then closed safely so no records\n"
          + "   are left locked.\n\n"
          + "TROUBLESHOOTING\n"
          + "   'Cannot connect to database' - ask the administrator to check\n"
          + "   that the WAMP server is running (the tray icon must be green).\n\n"
          + "   'Double booking' - that dentist is already busy at that time.\n"
          + "   Choose a different time or a different dentist.\n");
        help.setEditable(false);
        help.setFont(new Font("SansSerif", Font.PLAIN, 13));
        help.setCaretPosition(0);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(help), BorderLayout.CENTER);
        return panel;
    }

    // =================================================================
    // STATUS BAR - the clock label is updated by the background thread
    // =================================================================
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setBorder(BorderFactory.createEtchedBorder());
        bar.setPreferredSize(new Dimension(920, 26));
        lblClock.setHorizontalAlignment(SwingConstants.RIGHT);
        bar.add(lblStatus, BorderLayout.WEST);
        bar.add(lblUser,   BorderLayout.CENTER);
        bar.add(lblClock,  BorderLayout.EAST);
        return bar;
    }

    // =================================================================
    // GETTERS - controller reads user input through these
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
    public DefaultTableModel getTableModel() { return tableModel; }

    // =================================================================
    // SETTERS - controller pushes results back through these
    // =================================================================
    public void setTreatmentOptions(Collection<String> types) {
        cmbTreatment.removeAllItems();
        for (String t : types) {
            cmbTreatment.addItem(t);
        }
    }

    public void setDetails(String text)      { txtDetails.setText(text); txtDetails.setCaretPosition(0); }
    public void setReceipt(String text)      { txtReceipt.setText(text); txtReceipt.setCaretPosition(0); }
    public void setStatus(String text)       { lblStatus.setText("  " + text); }
    public void setClock(String text)        { lblClock.setText(text + "   "); }
    public void setLoggedInUser(String name) { lblUser.setText("Logged in as: " + name); }

    public void clearForm() {
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

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmExit() {
        return JOptionPane.showConfirmDialog(this,
                "Close the Appointment Management System?", "Confirm Exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    // =================================================================
    // OBSERVER PATTERN - controller subscribes to these events
    // =================================================================
    public void addSaveListener(ActionListener l)    { btnSave.addActionListener(l); }
    public void addClearListener(ActionListener l)   { btnClear.addActionListener(l); }
    public void addSearchListener(ActionListener l)  { btnSearch.addActionListener(l); }
    public void addBillListener(ActionListener l)    { btnBill.addActionListener(l); }
    public void addRefreshListener(ActionListener l) { btnRefresh.addActionListener(l); }
    public void addWindowCloseListener(WindowAdapter adapter) { addWindowListener(adapter); }
}
