package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * PRESENTATION TIER - staff sign-in window.
 *
 * Passive by design. It builds components, exposes getters for what the user
 * typed, and exposes addXxxListener methods so LoginController can attach
 * behaviour (Observer pattern). It contains no validation logic and no
 * database code - note there is no import of java.sql anywhere.
 *
 * SESSION FEATURE: "Keep me signed in on this computer" is the desktop
 * equivalent of a persistent browser cookie. When ticked, the session token
 * is written to a file in the user's home directory and validated against the
 * database at the next launch, so no password is needed until it expires.
 *
 * USABILITY:
 *   - the username field takes focus as soon as the window opens
 *   - a caps lock warning appears while typing the password, the commonest
 *     cause of a failed sign-in against a masked field
 *   - the button reads "Checking..." while the database is queried
 *
 * @author [Your Name]
 */
public class LoginView extends JFrame {

    private final JTextField     txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JCheckBox      chkRemember =
            new JCheckBox("Keep me signed in on this computer");

    private final JButton btnLogin  = Theme.primary("Sign In", 'S',
            "Sign in (Alt+S, or press Enter)");
    private final JButton btnCancel = Theme.secondary("Cancel", 'C',
            "Close the application");

    private final JLabel lblMessage  = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblCapsLock = new JLabel(" ", SwingConstants.CENTER);

    public LoginView() {
        setTitle("Sunrise Dental Clinic - Staff Sign In");
        setSize(460, 430);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.CARD);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                txtUsername.requestFocus();
            }
        });

        watchCapsLock();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Sunrise Dental Clinic", SwingConstants.CENTER);
        title.setFont(Theme.BRAND.deriveFont(23f));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Appointment Management System",
                                     SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.TEXT_FADED);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBackground(Theme.NAVY);
        panel.setBorder(Theme.pad(26, 16, 26, 16));
        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    /**
     * GridLayout stretches each component to fill its cell, so the text
     * fields need no column count and no fill constraint. An earlier version
     * used GridBagLayout and shipped a defect where the fields rendered at
     * zero width; the simpler layout removes that whole class of problem.
     */
    private JPanel buildForm() {
        txtUsername.setFont(Theme.BODY);
        txtUsername.setBorder(Theme.FIELD);
        txtUsername.setToolTipText("The username issued by the clinic administrator");

        txtPassword.setFont(Theme.BODY);
        txtPassword.setBorder(Theme.FIELD);

        chkRemember.setFont(Theme.SMALL);
        chkRemember.setBackground(Theme.CARD);
        chkRemember.setForeground(Theme.TEXT_MUTED);
        chkRemember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chkRemember.setToolTipText("Only on a computer you control. "
                + "A session token is stored on this machine.");

        JPanel fields = new JPanel(new GridLayout(2, 2, 12, 14));
        fields.setBackground(Theme.CARD);
        fields.add(Theme.formLabel("Username:"));
        fields.add(txtUsername);
        fields.add(Theme.formLabel("Password:"));
        fields.add(txtPassword);

        JPanel remember = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        remember.setBackground(Theme.CARD);
        remember.setBorder(Theme.pad(12, 96, 0, 0));
        remember.add(chkRemember);

        JPanel form = new JPanel(new BorderLayout());
        form.setBackground(Theme.CARD);
        form.setBorder(Theme.pad(28, 36, 6, 36));
        form.add(fields,   BorderLayout.NORTH);
        form.add(remember, BorderLayout.CENTER);
        return form;
    }

    private JPanel buildFooter() {
        lblMessage.setForeground(Theme.ERROR);
        lblMessage.setFont(Theme.BODY);

        lblCapsLock.setForeground(Theme.WARNING);
        lblCapsLock.setFont(Theme.SMALL);

        JPanel messages = new JPanel(new GridLayout(2, 1));
        messages.setBackground(Theme.CARD);
        messages.add(lblMessage);
        messages.add(lblCapsLock);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        buttons.setBackground(Theme.CARD);
        buttons.add(btnLogin);
        buttons.add(btnCancel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 20, 0));
        panel.add(messages, BorderLayout.NORTH);
        panel.add(buttons,  BorderLayout.CENTER);
        return panel;
    }

    /**
     * Warns while caps lock is on.
     *
     * A masked password field gives the user no way to see why a correct
     * password keeps being rejected. Wrapped in a catch because some
     * platforms cannot report lock state, so the warning degrades silently
     * rather than breaking sign-in.
     */
    private void watchCapsLock() {
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    boolean on = Toolkit.getDefaultToolkit()
                            .getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
                    lblCapsLock.setText(on ? "Caps Lock is on" : " ");
                } catch (UnsupportedOperationException ex) {
                    lblCapsLock.setText(" ");
                }
            }
        });
    }

    // ---------- getters ----------
    public String  getUsername()   { return txtUsername.getText().trim(); }
    public String  getPassword()   { return new String(txtPassword.getPassword()); }
    public boolean isRememberMe()  { return chkRemember.isSelected(); }

    // ---------- feedback ----------
    public void setMessage(String msg) {
        lblMessage.setForeground(Theme.ERROR);
        lblMessage.setText(msg);
    }

    /** A neutral notice, for example when a stored token has expired. */
    public void setNotice(String msg) {
        lblMessage.setForeground(Theme.TEXT_MUTED);
        lblMessage.setText(msg);
    }

    public void clearPassword() {
        txtPassword.setText("");
        txtPassword.requestFocus();
    }

    public void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Checking..." : "Sign In");
        setCursor(Cursor.getPredefinedCursor(
                busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    // ---------- Observer pattern ----------
    public void addLoginListener(ActionListener l)  { btnLogin.addActionListener(l); }
    public void addCancelListener(ActionListener l) { btnCancel.addActionListener(l); }
}
