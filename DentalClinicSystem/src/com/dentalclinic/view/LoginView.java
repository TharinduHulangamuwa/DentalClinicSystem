package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
 * This class is deliberately passive. It builds components, exposes getters
 * for what the user typed, and exposes addXxxListener methods so the
 * LoginController can attach behaviour (Observer pattern).
 *
 * It contains no validation logic and no database code.
 */
public class LoginView extends JFrame {

    private final JTextField     txtUsername = new JTextField(16);
    private final JPasswordField txtPassword = new JPasswordField(16);
    private final JButton        btnLogin    = new JButton("Login");
    private final JButton        btnCancel   = new JButton("Cancel");
    private final JLabel         lblMessage  = new JLabel(" ", SwingConstants.CENTER);

    public LoginView() {
        setTitle("Sunrise Dental Clinic - Staff Login");
        setSize(430, 260);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);           // centre on screen
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildForm(),    BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);   // Enter key submits
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(23, 58, 95));
        panel.setPreferredSize(new Dimension(430, 60));

        JLabel title = new JLabel("Sunrise Dental Clinic", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Appointment Management System", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(200, 215, 230));

        panel.add(title,    BorderLayout.CENTER);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 30, 5, 30));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Username:"), gc);
        gc.gridx = 1;
        panel.add(txtUsername, gc);

        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        panel.add(txtPassword, gc);

        return panel;
    }

    private JPanel buildFooter() {
        lblMessage.setForeground(new Color(180, 30, 30));
        lblMessage.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel buttons = new JPanel();
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

    // ---------- feedback the controller can push back ----------
    public void setMessage(String msg)  { lblMessage.setText(msg); }
    public void clearPassword()         { txtPassword.setText(""); txtPassword.requestFocus(); }
    public void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Checking..." : "Login");
    }

    // ---------- Observer pattern: controller subscribes here ----------
    public void addLoginListener(ActionListener l)  { btnLogin.addActionListener(l); }
    public void addCancelListener(ActionListener l) { btnCancel.addActionListener(l); }
}
