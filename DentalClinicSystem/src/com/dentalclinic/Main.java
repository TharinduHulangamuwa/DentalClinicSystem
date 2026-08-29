package com.dentalclinic;

import com.dentalclinic.controller.LoginController;
import com.dentalclinic.model.DBConnection;
import com.dentalclinic.model.UserDAO;
import com.dentalclinic.view.LoginView;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * Responsibilities, in order:
 *   1. Apply the native Windows look and feel so the app does not look
 *      like a 1998 Java applet.
 *   2. Verify the database is reachable and fail with a helpful message
 *      if WAMP is not running.
 *   3. Build the login view and its controller on the Event Dispatch
 *      Thread, as the Swing documentation requires.
 *
 * Note the SwingUtilities.invokeLater wrapper. Swing components must be
 * created on the Event Dispatch Thread, including the very first window.
 * Creating them on the main thread is a common bug that usually works by
 * accident and then fails unpredictably.
 */
public class Main {

    public static void main(String[] args) {

        // ---- 1. native look and feel ----
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // ---- 2. startup self-test ----
        if (!DBConnection.isReachable()) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to the dental_clinic database.\n\n"
              + "Please check:\n"
              + "  1. WampServer is running and the tray icon is GREEN\n"
              + "  2. The dental_clinic database exists in phpMyAdmin\n"
              + "  3. The MySQL Connector/J JAR is on the classpath\n\n"
              + "The application will now close.",
                "Database Connection Failed",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ---- 3. start the UI on the Event Dispatch Thread ----
        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            new LoginController(loginView, new UserDAO());
            loginView.setVisible(true);
        });
    }
}
