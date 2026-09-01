package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * PRESENTATION TIER - staff login window.
 *
 * Passive by design: it builds components, exposes getters, and exposes
 * addXxxListener methods so LoginController can attach behaviour. No
 * validation logic, no database code.
 *
 * USABILITY (version 1.2):
 *   - the username field receives focus when the window opens, so staff can
 *     type immediately without reaching for the mouse
 *   - a caps lock warning appears while typing the password, which is the
 *     single most common cause of a failed login
 *   - the Login button shows "Checking..." while the database is queried, so
 *     the user knows their click registered
 *
 * @author [Your Name]
 */
public class LoginView extends JFrame {

    private final JTextField     txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JButton        btnLogin    = Theme.primaryButton("Login", 'L',
            "Sign in (Alt+L, or press Enter)");
    private final JButton        btnCancel   = Theme.button("Cancel", 'C',
            "Close the application");
    private final JLabel         lblMessage  = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel         lblCapsLock = new JLabel(" ", SwingConstants.CENTER);

    public LoginView() {
        setTitle("Sunrise Dental Clinic - Staff Login");
        setSize(420, 290);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);   // Enter submits

        // Focus the first field as soon as the window appears.
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
        title.setFont(Theme.TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Appointment Management System", SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(new Color(200, 215, 230));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BRAND);
        panel.setBorder(Theme.pad(14, 10, 12, 10));
        panel.add(title,    BorderLayout.CENTER);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    /** GridLayout stretches the fields to fill their cells automatically. */
    private JPanel buildForm() {
        txtUsername.setFont(Theme.LABEL);
        txtUsername.setBorder(Theme.FIELD_BORDER);
        txtUsername.setToolTipText("The username issued by the clinic administrator");

        txtPassword.setFont(Theme.LABEL);
        txtPassword.setBorder(Theme.FIELD_BORDER);

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 12));
        form.setBorder(Theme.pad(22, 40, 6, 40));
        form.add(Theme.formLabel("Username:"));
        form.add(txtUsername);
        form.add(Theme.formLabel("Password:"));
        form.add(txtPassword);
        return form;
    }

    private JPanel buildFooter() {
        lblMessage.setForeground(Theme.ERROR);
        lblMessage.setFont(Theme.LABEL);

        lblCapsLock.setForeground(Theme.WARNING);
        lblCapsLock.setFont(Theme.SMALL);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttons.add(btnLogin);
        buttons.add(btnCancel);

        JPanel messages = new JPanel(new GridLayout(2, 1));
        messages.add(lblMessage);
        messages.add(lblCapsLock);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messages, BorderLayout.NORTH);
        panel.add(buttons,  BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return panel;
    }

    /**
     * Warns while caps lock is on.
     *
     * Passwords are masked, so a user who has caps lock on cannot see why
     * their correct password keeps being rejected. This is a small check that
     * removes a genuinely frustrating failure.
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
                    // some platforms cannot report lock state; the warning is
                    // a convenience, so silently do without it
                    lblCapsLock.setText(" ");
                }
            }
        });
    }

    // ---------- getters ----------
    public String getUsername() { return txtUsername.getText().trim(); }
    public String getPassword() { return new String(txtPassword.getPassword()); }

    // ---------- feedback ----------
    public void setMessage(String msg) { lblMessage.setText(msg); }

    public void clearPassword() {
        txtPassword.setText("");
        txtPassword.requestFocus();
    }

    public void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Checking..." : "Login");
        setCursor(java.awt.Cursor.getPredefinedCursor(
                busy ? java.awt.Cursor.WAIT_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
    }

    // ---------- Observer pattern ----------
    public void addLoginListener(ActionListener l)  { btnLogin.addActionListener(l); }
    public void addCancelListener(ActionListener l) { btnCancel.addActionListener(l); }
}
