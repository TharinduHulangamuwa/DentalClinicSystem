package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * PRESENTATION TIER - the main application window.
 *
 * LAYOUT: BorderLayout, GridLayout, FlowLayout, BoxLayout and CardLayout
 * only. An earlier version used GridBagLayout and shipped a defect where
 * text fields rendered at zero width; the simpler layouts remove that
 * whole class of problem because GridLayout stretches each component to
 * fill its cell automatically.
 *
 * INTERFACE DESIGN DECISIONS
 *
 * 1. Sidebar navigation rather than tabs. Tabs work for three or four
 *    screens and become cramped beyond that. A sidebar scales, keeps
 *    every destination visible, and marks the current one with an accent
 *    bar.
 *
 * 2. A dashboard as the opening screen. The receptionist's first question
 *    each morning is how busy the day is; one large number answers that
 *    faster than a table they must read and count.
 *
 * 3. Validation errors appear beside the field that caused them, and all
 *    fields are checked in one pass. The previous approach - one modal
 *    dialog per error - forced the user to read a message, dismiss it,
 *    then remember which field it referred to.
 *
 * @author [Your Name]
 */
public class MainView extends JFrame {

    // ---------------- screen keys ----------------
    public static final String SCREEN_DASHBOARD = "dashboard";
    public static final String SCREEN_REGISTER  = "register";
    public static final String SCREEN_BILL      = "bill";
    public static final String SCREEN_LIST      = "list";
    public static final String SCREEN_SUMMARY   = "summary";
    public static final String SCREEN_REMINDERS = "reminders";
    public static final String SCREEN_SESSIONS  = "sessions";
    public static final String SCREEN_HELP      = "help";

    private final CardLayout cards   = new CardLayout();
    private final JPanel     content = new JPanel(cards);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    // ---------------- dashboard ----------------
    private final StatCard cardToday    = new StatCard("Appointments today", "0", Theme.ACCENT);
    private final StatCard cardTomorrow = new StatCard("Tomorrow", "0", Theme.SUCCESS);
    private final StatCard cardTotal    = new StatCard("Total on record", "0", Theme.NAVY_LIGHT);
    private final StatCard cardRevenue  = new StatCard("Today's expected revenue (LKR)", "0.00", Theme.WARNING);

    private final DefaultTableModel todayModel = new DefaultTableModel(
            new String[]{"Time", "Appt No", "Patient", "Dentist", "Treatment"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final StripedTable tblToday = new StripedTable(todayModel);

    // ---------------- register ----------------
    private final JTextField txtAppointmentNo = new JTextField();
    private final JTextField txtPatientName   = new JTextField();
    private final JTextField txtAddress       = new JTextField();
    private final JTextField txtContactNo     = new JTextField();
    private final JComboBox<String> cmbDentist = new JComboBox<>(new String[]{
            "Dr. Fernando", "Dr. Silva", "Dr. Perera", "Dr. Wickramasinghe"});
    private final JComboBox<String> cmbTreatment = new JComboBox<>();
    private final JTextField txtDate = new JTextField();
    private final JTextField txtTime = new JTextField();

    private final JButton btnSave     = Theme.primary("Save Appointment", 'S',
            "Save this appointment (Alt+S)");
    private final JButton btnUpdate   = Theme.secondary("Update", 'U',
            "Save changes to the loaded appointment (Alt+U)");
    private final JButton btnClear    = Theme.secondary("Clear", 'C',
            "Empty every field (Alt+C)");
    private final JButton btnToday    = Theme.secondary("Today", 'T', "Use today's date");
    private final JButton btnTomorrow = Theme.secondary("Tomorrow", 'M', "Use tomorrow's date");
    private final JLabel  lblFormMode = new JLabel(" ");

    private final Map<String, JLabel>     errorLabels = new LinkedHashMap<>();
    private final Map<String, JComponent> fields      = new LinkedHashMap<>();

    // ---------------- bill ----------------
    private final JTextField txtSearchNo        = new JTextField(12);
    private final JButton    btnSearch          = Theme.primary("Search", 'R',
            "Find this appointment (Alt+R, or press Enter)");
    private final JTextArea  txtDetails         = new JTextArea();
    private final JTextField txtConsultationFee = new JTextField("1500", 8);
    private final JButton    btnBill            = Theme.primary("Calculate Bill", 'B',
            "Work out the total and print the receipt (Alt+B)");
    private final JButton    btnSaveReceipt     = Theme.secondary("Save Receipt", 'V',
            "Write the receipt to a text file");
    private final JTextArea  txtReceipt         = new JTextArea();

    // ---------------- appointment list ----------------
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Appt No", "Patient", "Contact", "Dentist",
                         "Treatment", "Date", "Time"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final StripedTable tblAppointments = new StripedTable(tableModel);
    private final JTextField txtFilter   = new JTextField(18);
    private final JButton btnRefresh     = Theme.secondary("Refresh", 'F',
            "Reload from the database (Alt+F)");
    private final JButton btnEdit        = Theme.secondary("Edit Selected", 'E',
            "Load the selected appointment into the register form");
    private final JButton btnDelete      = Theme.danger("Delete Selected", 'D',
            "Permanently remove the selected appointment");

    // ---------------- summary ----------------
    private final DefaultTableModel reportModel = new DefaultTableModel(
            new String[]{"Date", "Dentist", "Appointments", "Expected Revenue (LKR)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final StripedTable tblReport = new StripedTable(reportModel);
    private final JButton btnReportRefresh = Theme.secondary("Refresh Report", 'P',
            "Recalculate the summary (Alt+P)");

    // ---------------- reminders ----------------
    private final JTextArea txtNotifications = new JTextArea();
    private final JButton   btnSendReminders = Theme.primary(
            "Generate Tomorrow's Reminders", 'G', "Build reminder messages (Alt+G)");

    // ---------------- sessions ----------------
    private final DefaultTableModel sessionModel = new DefaultTableModel(
            new String[]{"Username", "Full Name", "Machine",
                         "Signed In", "Last Activity", "Idle"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final StripedTable tblSessions = new StripedTable(sessionModel);
    private final JTextArea txtSessionInfo   = new JTextArea();
    private final JButton btnSessionRefresh  = Theme.secondary("Refresh", 'H',
            "Reload the active session list");
    private final JButton btnEndAllSessions  = Theme.danger("Sign Out Everywhere", 'X',
            "Close this account's sessions on every machine");

    // ---------------- header and status ----------------
    private final JLabel  lblScreenTitle = new JLabel("Dashboard");
    private final JLabel  lblUser        = new JLabel();
    private final JLabel  lblSession     = new JLabel();
    private final JLabel  lblStatus      = new JLabel("Ready");
    private final JLabel  lblClock       = new JLabel();
    private final JButton btnLogout      = Theme.onDark("Sign Out", 'L',
            "End this session (Alt+L)");

    private ActionListener searchListener;

    public MainView() {
        setTitle("Sunrise Dental Clinic - Appointment Management System");
        setSize(1150, 740);
        setMinimumSize(new Dimension(1040, 660));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CANVAS);

        content.setBackground(Theme.CANVAS);
        content.add(buildDashboardScreen(), SCREEN_DASHBOARD);
        content.add(buildRegisterScreen(),  SCREEN_REGISTER);
        content.add(buildBillScreen(),      SCREEN_BILL);
        content.add(buildListScreen(),      SCREEN_LIST);
        content.add(buildSummaryScreen(),   SCREEN_SUMMARY);
        content.add(buildRemindersScreen(), SCREEN_REMINDERS);
        content.add(buildSessionsScreen(),  SCREEN_SESSIONS);
        content.add(buildHelpScreen(),      SCREEN_HELP);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.CANVAS);
        main.add(buildHeader(),    BorderLayout.NORTH);
        main.add(content,          BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);

        add(buildSidebar(), BorderLayout.WEST);
        add(main,           BorderLayout.CENTER);

        wireConvenienceKeys();
        showScreen(SCREEN_DASHBOARD, "Dashboard");
    }

    // =================================================================
    // SIDEBAR
    // =================================================================
    private JPanel buildSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(Theme.NAVY);
        bar.setPreferredSize(new Dimension(240, 0));

        JLabel brand = new JLabel("Sunrise Dental");
        brand.setFont(Theme.BRAND);
        brand.setForeground(Color.WHITE);
        brand.setBorder(Theme.pad(22, 26, 2, 16));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Clinic Management");
        sub.setFont(Theme.SMALL);
        sub.setForeground(Theme.TEXT_FADED);
        sub.setBorder(Theme.pad(0, 26, 22, 16));
        sub.setAlignmentX(LEFT_ALIGNMENT);

        bar.add(brand);
        bar.add(sub);

        addNav(bar, SCREEN_DASHBOARD, "Dashboard");
        addNav(bar, SCREEN_REGISTER,  "Register Appointment");
        addNav(bar, SCREEN_BILL,      "Search & Bill");
        addNav(bar, SCREEN_LIST,      "All Appointments");
        addNav(bar, SCREEN_SUMMARY,   "Daily Summary");
        addNav(bar, SCREEN_REMINDERS, "Patient Reminders");
        addNav(bar, SCREEN_SESSIONS,  "Active Sessions");
        addNav(bar, SCREEN_HELP,      "Help");

        bar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("Version 1.3");
        version.setFont(Theme.SMALL);
        version.setForeground(new Color(120, 145, 170));
        version.setBorder(Theme.pad(0, 26, 18, 16));
        version.setAlignmentX(LEFT_ALIGNMENT);
        bar.add(version);
        return bar;
    }

    private void addNav(JPanel bar, String screen, String label) {
        NavButton b = new NavButton(label);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addActionListener(e -> showScreen(screen, label));
        navButtons.put(screen, b);
        bar.add(b);
    }

    /** Switches the visible screen and moves the sidebar highlight. */
    public void showScreen(String screen, String title) {
        cards.show(content, screen);
        lblScreenTitle.setText(title);
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setSelected(e.getKey().equals(screen));
        }
    }

    // =================================================================
    // HEADER
    // =================================================================
    private JPanel buildHeader() {
        lblScreenTitle.setFont(Theme.TITLE);
        lblScreenTitle.setForeground(Color.WHITE);

        lblUser.setFont(Theme.BODY);
        lblUser.setForeground(Theme.TEXT_ON_DARK);
        lblUser.setHorizontalAlignment(SwingConstants.RIGHT);

        lblSession.setFont(Theme.SMALL);
        lblSession.setForeground(Theme.TEXT_FADED);
        lblSession.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel userBox = new JPanel(new GridLayout(2, 1));
        userBox.setBackground(Theme.NAVY_LIGHT);
        userBox.add(lblUser);
        userBox.add(lblSession);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setBackground(Theme.NAVY_LIGHT);
        right.add(userBox);
        right.add(btnLogout);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY_LIGHT);
        header.setBorder(Theme.pad(14, 26, 14, 16));
        header.add(lblScreenTitle, BorderLayout.WEST);
        header.add(right,          BorderLayout.EAST);
        return header;
    }

    // =================================================================
    // SCREEN - dashboard
    // =================================================================
    private JPanel buildDashboardScreen() {
        JPanel stats = Theme.canvas(new GridLayout(1, 4, 16, 0));
        stats.add(cardToday);
        stats.add(cardTomorrow);
        stats.add(cardTotal);
        stats.add(cardRevenue);

        tblToday.width(0, 70);
        tblToday.width(1, 90);
        JScrollPane scroll = new JScrollPane(tblToday);
        scroll.setBorder(Theme.titledCard("Today's Schedule"));
        scroll.getViewport().setBackground(Theme.CARD);

        JPanel screen = Theme.canvas(new BorderLayout(0, 18));
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(stats,  BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        return screen;
    }

    // =================================================================
    // SCREEN - register
    //
    // Three columns: label, input, message. The third column holds the
    // helper text and switches to a red error message when validation
    // fails, so feedback appears where the problem is.
    // =================================================================
    private JPanel buildRegisterScreen() {
        JPanel form = Theme.white(new GridLayout(8, 3, 12, 10));
        form.setBorder(Theme.titledCard("Patient and Appointment Details"));

        addRow(form, "appointmentNo", "Appointment No:", txtAppointmentNo, "format APT1001");
        addRow(form, "patientName",   "Patient Name:",   txtPatientName,   "letters and spaces");
        addRow(form, "address",       "Address:",        txtAddress,       "optional");
        addRow(form, "contactNo",     "Contact No:",     txtContactNo,     "10 digits, e.g. 0771234567");
        addRow(form, "dentistName",   "Dentist:",        cmbDentist,       "");
        addRow(form, "treatmentType", "Treatment Type:", cmbTreatment,     "price from the database");
        addRow(form, "date",          "Date:",           txtDate,          "yyyy-MM-dd");
        addRow(form, "time",          "Time:",           txtTime,          "HH:mm, clinic 08:00-20:00");

        lblFormMode.setFont(Theme.SMALL);
        lblFormMode.setForeground(Theme.ACCENT_DARK);

        JPanel quick = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 8, 8));
        quick.add(Theme.hint("Quick date:"));
        quick.add(btnToday);
        quick.add(btnTomorrow);
        quick.add(Box.createHorizontalStrut(20));
        quick.add(lblFormMode);

        JPanel actions = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 12, 4));
        actions.add(btnSave);
        actions.add(btnUpdate);
        actions.add(btnClear);
        btnUpdate.setVisible(false);      // only shown while editing

        JPanel below = Theme.canvas(new BorderLayout());
        below.add(quick,   BorderLayout.NORTH);
        below.add(actions, BorderLayout.CENTER);

        JPanel screen = Theme.canvas(new BorderLayout());
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(form,  BorderLayout.NORTH);
        screen.add(below, BorderLayout.CENTER);
        return screen;
    }

    private void addRow(JPanel form, String key, String label,
                        JComponent field, String hint) {
        field.setFont(Theme.BODY);
        if (field instanceof JTextField) {
            field.setBorder(Theme.FIELD);
        }
        JLabel message = Theme.hint(hint);
        form.add(Theme.formLabel(label));
        form.add(field);
        form.add(message);
        fields.put(key, field);
        errorLabels.put(key, message);
    }

    // =================================================================
    // SCREEN - search and bill
    // =================================================================
    private JPanel buildBillScreen() {
        txtSearchNo.setFont(Theme.BODY);
        txtSearchNo.setBorder(Theme.FIELD);
        txtSearchNo.setToolTipText("An appointment number such as APT1001");

        txtConsultationFee.setFont(Theme.BODY);
        txtConsultationFee.setBorder(Theme.FIELD);
        txtConsultationFee.setToolTipText("Fee for this consultation, in rupees");

        // Two explicit rows rather than one wrapping FlowLayout.
        // A FlowLayout reports the preferred height of a SINGLE row even when
        // it wraps, so in a BorderLayout.NORTH slot the wrapped row is clipped
        // and its buttons become unreachable. Laying the rows out explicitly
        // removes that failure entirely.
        JPanel rowFind = Theme.white(new FlowLayout(FlowLayout.LEFT, 10, 4));
        rowFind.add(Theme.formLabel("Appointment No:"));
        rowFind.add(txtSearchNo);
        rowFind.add(btnSearch);

        JPanel rowBill = Theme.white(new FlowLayout(FlowLayout.LEFT, 10, 4));
        rowBill.add(Theme.formLabel("Consultation Fee (LKR):"));
        rowBill.add(txtConsultationFee);
        rowBill.add(btnBill);
        rowBill.add(btnSaveReceipt);

        JPanel controls = Theme.white(new GridLayout(2, 1, 0, 4));
        controls.setBorder(Theme.titledCard("Find an appointment, then bill it"));
        controls.add(rowFind);
        controls.add(rowBill);

        txtDetails.setEditable(false);
        txtDetails.setFont(Theme.MONO);
        txtDetails.setBackground(Theme.CARD);
        txtDetails.setText("\n   Search for an appointment number to see the record here.\n");

        txtReceipt.setEditable(false);
        txtReceipt.setFont(Theme.MONO);
        txtReceipt.setBackground(Theme.CARD);
        txtReceipt.setText("\n   The receipt appears here once you calculate the bill.\n");

        JScrollPane detail = new JScrollPane(txtDetails);
        detail.setBorder(Theme.titledCard("Appointment Details"));
        JScrollPane receipt = new JScrollPane(txtReceipt);
        receipt.setBorder(Theme.titledCard("Patient Receipt"));

        JPanel panes = Theme.canvas(new GridLayout(2, 1, 0, 14));
        panes.add(detail);
        panes.add(receipt);

        JPanel screen = Theme.canvas(new BorderLayout(0, 14));
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(controls, BorderLayout.NORTH);
        screen.add(panes,    BorderLayout.CENTER);
        return screen;
    }

    // =================================================================
    // SCREEN - all appointments
    // =================================================================
    private JPanel buildListScreen() {
        tblAppointments.width(0, 90);
        tblAppointments.width(1, 170);
        tblAppointments.width(4, 150);

        // Double clicking a row bills it. Retyping a number the user can
        // already see on screen is wasted effort and a chance to mistype.
        tblAppointments.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedInBilling();
                }
            }
        });

        txtFilter.setFont(Theme.BODY);
        txtFilter.setBorder(Theme.FIELD);
        txtFilter.setToolTipText("Filter by patient, number, contact or dentist");

        JPanel top = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(Theme.formLabel("Filter:"));
        top.add(txtFilter);
        top.add(Theme.hint("   Double click a row to bill it.  Click a heading to sort."));
        top.setBorder(Theme.pad(0, 0, 10, 0));

        JScrollPane scroll = new JScrollPane(tblAppointments);
        scroll.setBorder(Theme.hairline());
        scroll.getViewport().setBackground(Theme.CARD);

        JPanel bottom = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 10, 10));
        bottom.add(btnRefresh);
        bottom.add(btnEdit);
        bottom.add(btnDelete);

        JPanel screen = Theme.canvas(new BorderLayout());
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(top,    BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        return screen;
    }

    private void openSelectedInBilling() {
        String number = getSelectedAppointmentNo();
        if (number == null) {
            return;
        }
        txtSearchNo.setText(number);
        showScreen(SCREEN_BILL, "Search & Bill");
        if (searchListener != null) {
            searchListener.actionPerformed(new ActionEvent(this, 0, "search"));
        }
    }

    // =================================================================
    // SCREEN - daily summary
    // =================================================================
    private JPanel buildSummaryScreen() {
        tblReport.rightAlign(2);
        tblReport.rightAlign(3);

        JScrollPane scroll = new JScrollPane(tblReport);
        scroll.setBorder(Theme.hairline());
        scroll.getViewport().setBackground(Theme.CARD);

        JPanel top = Theme.canvas(new BorderLayout());
        top.add(Theme.hint("Appointments and expected treatment revenue "
                + "per dentist per day."), BorderLayout.WEST);
        top.setBorder(Theme.pad(0, 0, 10, 0));

        JPanel bottom = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 0, 10));
        bottom.add(btnReportRefresh);

        JPanel screen = Theme.canvas(new BorderLayout());
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(top,    BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        return screen;
    }

    // =================================================================
    // SCREEN - reminders
    // =================================================================
    private JPanel buildRemindersScreen() {
        txtNotifications.setEditable(false);
        txtNotifications.setFont(Theme.MONO);
        txtNotifications.setBackground(Theme.CARD);
        txtNotifications.setText(
            "\n  No reminders generated yet.\n\n"
          + "  Use the button below to build reminder messages for every\n"
          + "  patient with an appointment tomorrow. Messages are written\n"
          + "  to the 'reminders' folder, ready for the clinic's SMS\n"
          + "  provider or a mail merge.\n");

        JScrollPane scroll = new JScrollPane(txtNotifications);
        scroll.setBorder(Theme.titledCard("Generated Messages"));

        JPanel bottom = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 0, 10));
        bottom.add(btnSendReminders);

        JPanel screen = Theme.canvas(new BorderLayout());
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(scroll, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        return screen;
    }

    // =================================================================
    // SCREEN - active sessions
    // =================================================================
    private JPanel buildSessionsScreen() {
        txtSessionInfo.setEditable(false);
        txtSessionInfo.setFont(Theme.BODY);
        txtSessionInfo.setBackground(Theme.CARD);
        txtSessionInfo.setBorder(Theme.titledCard("This Session"));
        txtSessionInfo.setRows(6);

        JScrollPane scroll = new JScrollPane(tblSessions);
        scroll.setBorder(Theme.titledCard("All Active Sessions"));
        scroll.getViewport().setBackground(Theme.CARD);

        JPanel bottom = Theme.canvas(new FlowLayout(FlowLayout.LEFT, 10, 10));
        bottom.add(btnSessionRefresh);
        bottom.add(btnEndAllSessions);

        JPanel screen = Theme.canvas(new BorderLayout(0, 14));
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(txtSessionInfo, BorderLayout.NORTH);
        screen.add(scroll,         BorderLayout.CENTER);
        screen.add(bottom,         BorderLayout.SOUTH);
        return screen;
    }

    // =================================================================
    // SCREEN - help
    // =================================================================
    private JPanel buildHelpScreen() {
        JTextArea help = new JTextArea(
            "SUNRISE DENTAL CLINIC - SYSTEM USER GUIDE\n"
          + "=========================================\n\n"
          + "KEYBOARD SHORTCUTS\n"
          + "   Alt+S  Save appointment      Alt+U  Update appointment\n"
          + "   Alt+C  Clear form            Alt+R  Search\n"
          + "   Alt+B  Calculate bill        Alt+F  Refresh list\n"
          + "   Alt+E  Edit selected         Alt+D  Delete selected\n"
          + "   Alt+G  Generate reminders    Alt+L  Sign out\n"
          + "   Enter  Submits the form you are currently in\n\n"
          + "1. SIGNING IN\n"
          + "   Enter the username and password issued by the clinic\n"
          + "   administrator. Tick 'Keep me signed in on this computer' only\n"
          + "   on a machine you personally control: it stores a session token\n"
          + "   so the system reopens without asking for your password.\n\n"
          + "2. SESSIONS\n"
          + "   Each sign-in creates a session recorded in the database. The\n"
          + "   header shows how long your session has left. After 30 minutes\n"
          + "   with no activity you are signed out automatically, so an\n"
          + "   unattended reception desk cannot be used by a passer-by.\n\n"
          + "   'Active Sessions' lists every signed-in account. 'Sign Out\n"
          + "   Everywhere' closes your sessions on all machines - use it if\n"
          + "   you have left yourself signed in elsewhere.\n\n"
          + "3. THE DASHBOARD\n"
          + "   Opens on sign-in and shows how many appointments there are\n"
          + "   today and tomorrow, the total on record, and the revenue\n"
          + "   expected today, with today's schedule underneath.\n\n"
          + "4. REGISTERING AN APPOINTMENT\n"
          + "   a. Appointment No: APT followed by four digits. The next free\n"
          + "      number is filled in for you.\n"
          + "   b. Patient Name: letters and spaces only.\n"
          + "   c. Contact No: exactly ten digits starting with 0.\n"
          + "   d. Dentist and Treatment: choose from the dropdowns.\n"
          + "   e. Date: use Today or Tomorrow, or type yyyy-MM-dd.\n"
          + "   f. Time: HH:mm, between 08:00 and 20:00.\n"
          + "   g. Click Save Appointment.\n\n"
          + "   Problems appear in red beside the field concerned and the\n"
          + "   field is highlighted, so you do not have to dismiss a dialog\n"
          + "   to find out which field was wrong.\n\n"
          + "   The system refuses to save if the dentist already has an\n"
          + "   appointment at that exact date and time. This prevents the\n"
          + "   double bookings that happened with the paper diary.\n\n"
          + "5. CHANGING OR CANCELLING AN APPOINTMENT\n"
          + "   Open 'All Appointments', select the row, then:\n"
          + "     Edit Selected   - loads it into the register form. Change\n"
          + "                       what is needed and click Update.\n"
          + "     Delete Selected - removes it permanently after confirming.\n\n"
          + "6. FINDING AND BILLING\n"
          + "   Search & Bill: type the number and press Enter. Or open All\n"
          + "   Appointments and double click any row. Then enter the\n"
          + "   consultation fee and click Calculate Bill. 'Save Receipt'\n"
          + "   writes it to a text file you can print.\n\n"
          + "7. DAILY SUMMARY\n"
          + "   Appointments and expected revenue per dentist per day. Useful\n"
          + "   for spotting a dentist who is over-booked.\n\n"
          + "8. PATIENT REMINDERS\n"
          + "   Builds a reminder for every patient booked tomorrow and\n"
          + "   writes them to a dispatch file for the SMS provider.\n\n"
          + "TROUBLESHOOTING\n"
          + "   'Cannot connect to database' - ask the administrator to check\n"
          + "   that WampServer is running (tray icon must be green).\n\n"
          + "   'Double booking' - that dentist is busy at that time. Choose\n"
          + "   a different time or a different dentist.\n\n"
          + "   Signed out unexpectedly - your session timed out after 30\n"
          + "   minutes of inactivity. Sign in again.\n");
        help.setEditable(false);
        help.setFont(Theme.BODY);
        help.setBackground(Theme.CARD);
        help.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(help);
        scroll.setBorder(Theme.hairline());

        JPanel screen = Theme.canvas(new BorderLayout());
        screen.setBorder(Theme.pad(20, 26, 20, 26));
        screen.add(scroll, BorderLayout.CENTER);
        return screen;
    }

    // =================================================================
    // STATUS BAR
    // =================================================================
    private JPanel buildStatusBar() {
        lblStatus.setFont(Theme.BODY);
        lblStatus.setForeground(Theme.TEXT);
        lblClock.setFont(Theme.BODY);
        lblClock.setForeground(Theme.TEXT_MUTED);
        lblClock.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                Theme.pad(7, 26, 7, 26)));
        bar.add(lblStatus, BorderLayout.WEST);
        bar.add(lblClock,  BorderLayout.EAST);
        return bar;
    }

    // =================================================================
    // KEYBOARD CONVENIENCE
    // =================================================================
    private void wireConvenienceKeys() {
        txtSearchNo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && searchListener != null) {
                    searchListener.actionPerformed(new ActionEvent(this, 0, "search"));
                }
            }
        });

        btnToday.addActionListener(e -> {
            txtDate.setText(LocalDate.now().toString());
            clearFieldError("date");
        });
        btnTomorrow.addActionListener(e -> {
            txtDate.setText(LocalDate.now().plusDays(1).toString());
            clearFieldError("date");
        });

        // Typing in a field clears its own error, so stale messages do not
        // linger next to a value the user has already corrected.
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
    public String getFilterText()      { return txtFilter.getText().trim(); }
    public String getReceiptText()     { return txtReceipt.getText(); }

    public DefaultTableModel getTableModel()        { return tableModel; }
    public DefaultTableModel getReportTableModel()  { return reportModel; }
    public DefaultTableModel getSessionTableModel() { return sessionModel; }
    public DefaultTableModel getTodayTableModel()   { return todayModel; }

    /** The appointment number of the selected row, or null if none. */
    public String getSelectedAppointmentNo() {
        int row = tblAppointments.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = tblAppointments.convertRowIndexToModel(row);
        return String.valueOf(tableModel.getValueAt(modelRow, 0));
    }

    public boolean isRegisterFormDirty() {
        return !getAppointmentNo().isEmpty() || !getPatientName().isEmpty()
            || !getAddress().isEmpty()       || !getContactNo().isEmpty()
            || !getDate().isEmpty()          || !getTime().isEmpty();
    }

    // =================================================================
    // INLINE VALIDATION FEEDBACK
    // =================================================================
    public void showFieldError(String key, String message) {
        JLabel label = errorLabels.get(key);
        if (label != null) {
            label.setText(message);
            label.setForeground(Theme.ERROR);
        }
        JComponent field = fields.get(key);
        if (field instanceof JTextField) {
            field.setBackground(Theme.ERROR_BG);
            field.setBorder(Theme.FIELD_ERROR);
            field.requestFocus();
        }
    }

    public void clearFieldError(String key) {
        JLabel label = errorLabels.get(key);
        if (label != null && Theme.ERROR.equals(label.getForeground())) {
            label.setText("");
            label.setForeground(Theme.TEXT_MUTED);
        }
        JComponent field = fields.get(key);
        if (field instanceof JTextField) {
            field.setBackground(Theme.FIELD_OK);
            field.setBorder(Theme.FIELD);
        }
    }

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

    public void suggestAppointmentNo(String number) {
        if (txtAppointmentNo.getText().trim().isEmpty()) {
            txtAppointmentNo.setText(number);
        }
    }

    /** Loads an appointment into the register form for editing. */
    public void loadForEdit(String no, String name, String address, String contact,
                            String dentist, String treatment, String date, String time) {
        clearAllFieldErrors();
        txtAppointmentNo.setText(no);
        txtAppointmentNo.setEditable(false);    // the key must not change
        txtPatientName.setText(name);
        txtAddress.setText(address == null ? "" : address);
        txtContactNo.setText(contact);
        cmbDentist.setSelectedItem(dentist);
        cmbTreatment.setSelectedItem(treatment);
        txtDate.setText(date);
        txtTime.setText(time);

        lblFormMode.setText("Editing " + no + " - change what you need, then click Update");
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
        showScreen(SCREEN_REGISTER, "Register Appointment");
        txtPatientName.requestFocus();
    }

    public boolean isEditing() {
        return btnUpdate.isVisible();
    }

    public void setDashboardStats(String today, String tomorrow,
                                  String total, String revenue) {
        cardToday.setValue(today);
        cardTomorrow.setValue(tomorrow);
        cardTotal.setValue(total);
        cardRevenue.setValue(revenue);
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

    public void setSessionInfo(String text) {
        txtSessionInfo.setText(text);
        txtSessionInfo.setCaretPosition(0);
    }

    public void setStatus(String text) {
        lblStatus.setForeground(Theme.TEXT);
        lblStatus.setText(text);
    }

    public void setStatusSuccess(String text) {
        lblStatus.setForeground(Theme.SUCCESS);
        lblStatus.setText(text);
    }

    public void setStatusError(String text) {
        lblStatus.setForeground(Theme.ERROR);
        lblStatus.setText(text);
    }

    public void setClock(String text)        { lblClock.setText(text); }
    public void setLoggedInUser(String name) { lblUser.setText(name); }

    /** Session countdown in the header; amber when the time is short. */
    public void setSessionCountdown(String text, boolean warning) {
        lblSession.setText(text);
        lblSession.setForeground(warning ? Theme.AMBER : Theme.TEXT_FADED);
    }

    public void setBusy(boolean busy) {
        setCursor(Cursor.getPredefinedCursor(
                busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void clearForm() {
        clearAllFieldErrors();
        txtAppointmentNo.setText("");
        txtAppointmentNo.setEditable(true);
        txtPatientName.setText("");
        txtAddress.setText("");
        txtContactNo.setText("");
        txtDate.setText("");
        txtTime.setText("");
        cmbDentist.setSelectedIndex(0);
        if (cmbTreatment.getItemCount() > 0) {
            cmbTreatment.setSelectedIndex(0);
        }
        lblFormMode.setText(" ");
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        txtAppointmentNo.requestFocus();
    }

    /** Asks before discarding typed data; silent when the form is empty. */
    public boolean confirmClear() {
        if (!isRegisterFormDirty()) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this,
                "Clear the form? Anything you have typed will be lost.",
                "Confirm Clear", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public boolean confirmDelete(String appointmentNo) {
        return JOptionPane.showConfirmDialog(this,
                "Permanently delete appointment " + appointmentNo + "?\n\n"
              + "This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public boolean confirmExit() {
        return JOptionPane.showConfirmDialog(this,
                "Close the Appointment Management System?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Session",
                JOptionPane.WARNING_MESSAGE);
    }

    // =================================================================
    // OBSERVER PATTERN - the controller subscribes to these events
    // =================================================================
    public void addSaveListener(ActionListener l)          { btnSave.addActionListener(l); }
    public void addUpdateListener(ActionListener l)         { btnUpdate.addActionListener(l); }
    public void addClearListener(ActionListener l)          { btnClear.addActionListener(l); }
    public void addBillListener(ActionListener l)           { btnBill.addActionListener(l); }
    public void addSaveReceiptListener(ActionListener l)    { btnSaveReceipt.addActionListener(l); }
    public void addRefreshListener(ActionListener l)        { btnRefresh.addActionListener(l); }
    public void addEditListener(ActionListener l)           { btnEdit.addActionListener(l); }
    public void addDeleteListener(ActionListener l)         { btnDelete.addActionListener(l); }
    public void addReportRefreshListener(ActionListener l)  { btnReportRefresh.addActionListener(l); }
    public void addRemindersListener(ActionListener l)      { btnSendReminders.addActionListener(l); }
    public void addLogoutListener(ActionListener l)         { btnLogout.addActionListener(l); }
    public void addSessionRefreshListener(ActionListener l) { btnSessionRefresh.addActionListener(l); }
    public void addEndAllSessionsListener(ActionListener l) { btnEndAllSessions.addActionListener(l); }

    public void addSearchListener(ActionListener l) {
        this.searchListener = l;
        btnSearch.addActionListener(l);
    }

    /** Fires as the user types in the appointment filter box. */
    public void addFilterListener(ActionListener l) {
        txtFilter.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                l.actionPerformed(new ActionEvent(this, 0, "filter"));
            }
        });
    }

    public void addWindowCloseListener(WindowAdapter adapter) {
        addWindowListener(adapter);
    }

    /** Any mouse or key event anywhere refreshes the session idle timer. */
    public void addGlobalActivityListener(ActionListener l) {
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
            event -> l.actionPerformed(new ActionEvent(this, 0, "activity")),
            java.awt.AWTEvent.MOUSE_EVENT_MASK | java.awt.AWTEvent.KEY_EVENT_MASK);
    }
}
