package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
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
 * This class is passive. It builds components, exposes getters for what the
 * user typed, and exposes addXxxListener methods so LoginController can
 * attach behaviour (Observer pattern). It has no validation logic and no
 * database code.
 *
 * LAYOUT: BorderLayout for the window, GridLayout for the form. GridLayout
 * gives every cell an identical size and stretches components to fill it,
 * so the text fields size correctly with no extra constraints.
 *
 * @author [Your Name]
 */
public class LoginView extends JFrame {

    private final JTextField     txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JButton        btnLogin    = new JButton("Login");
    private final JButton        btnCancel   = new JButton("Cancel");
    private final JLabel         lblMessage  = new JLabel(" ", SwingConstants.CENTER);

    public LoginView() {
        setTitle("Sunrise Dental Clinic - Staff Login");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);          // centre on screen
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);   // Enter key submits
    }

    /** Title bar across the top. */
    private JPanel buildHeader() {
        JLabel title = new JLabel("Sunrise Dental Clinic", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(23, 58, 95));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Two rows, two columns: label then field.
     * GridLayout stretches each component to fill its cell, which is why
     * the fields need no column count or fill constraint.
     */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 12));
        form.setBorder(BorderFactory.createEmptyBorder(20, 40, 10, 40));

        form.add(new JLabel("Username:"));
        form.add(txtUsername);
        form.add(new JLabel("Password:"));
        form.add(txtPassword);

        return form;
    }

    /** Error message above, buttons below. */
    private JPanel buildFooter() {
        lblMessage.setForeground(Color.RED);

        JPanel buttons = new JPanel();          // FlowLayout centres by default
        buttons.add(btnLogin);
        buttons.add(btnCancel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblMessage, BorderLayout.NORTH);
        panel.add(buttons,    BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return panel;
    }

    // ---------- getters used by the controller ----------
    public String getUsername() { return txtUsername.getText().trim(); }
    public String getPassword() { return new String(txtPassword.getPassword()); }

    // ---------- feedback the controller pushes back ----------
    public void setMessage(String msg) { lblMessage.setText(msg); }

    public void clearPassword() {
        txtPassword.setText("");
        txtPassword.requestFocus();
    }

    public void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Checking..." : "Login");
    }

    // ---------- Observer pattern: controller subscribes here ----------
    public void addLoginListener(ActionListener l)  { btnLogin.addActionListener(l); }
    public void addCancelListener(ActionListener l) { btnCancel.addActionListener(l); }
}