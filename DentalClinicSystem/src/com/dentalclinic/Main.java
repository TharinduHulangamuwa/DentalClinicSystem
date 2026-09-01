package com.dentalclinic;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.LoginController;
import com.dentalclinic.model.DBConnection;
import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionDAO;
import com.dentalclinic.model.SessionStore;
import com.dentalclinic.model.User;
import com.dentalclinic.model.UserDAO;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * Startup sequence:
 *   1. Apply the native look and feel.
 *   2. Verify the database is reachable, failing with a helpful message if
 *      WampServer is not running.
 *   3. Close any sessions that expired while the program was not running.
 *   4. Look for a stored session token. If the database says it is still
 *      valid, sign the user straight in; otherwise show the sign-in window.
 *
 * Step 4 is the desktop equivalent of a browser sending a persistent cookie:
 * the token proves a previous successful authentication, so no password is
 * required until it expires or is revoked.
 *
 * Note SwingUtilities.invokeLater. Swing components must be created on the
 * Event Dispatch Thread, including the very first window.
 *
 * @author [Your Name]
 */
public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set the system look and feel: " + e.getMessage());
        }

        if (!DBConnection.isReachable()) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to the dental_clinic database.\n\n"
              + "Please check:\n"
              + "  1. WampServer is running and the tray icon is GREEN\n"
              + "  2. sql/dental_clinic.sql has been run in phpMyAdmin\n"
              + "  3. The MySQL Connector/J JAR is on the classpath\n\n"
              + "The exact error has been printed to the Output window.\n\n"
              + "The application will now close.",
                "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        final SessionDAO sessionDAO = new SessionDAO();
        try {
            int closed = sessionDAO.purgeExpired();
            if (closed > 0) {
                System.out.println("[SESSION] " + closed + " expired session(s) closed");
            }
        } catch (Exception e) {
            System.err.println("Session housekeeping failed: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            String storedToken = SessionStore.load();

            if (storedToken != null) {
                try {
                    User user = sessionDAO.validate(storedToken);

                    if (user != null) {
                        sessionDAO.touch(storedToken);
                        Session.start(storedToken, user, true);

                        MainView mainView = new MainView();
                        new AppointmentController(mainView, user);
                        mainView.setVisible(true);
                        System.out.println("[SESSION] resumed for " + user.getUsername());
                        return;
                    }

                    // Expired, revoked, or the row was deleted.
                    SessionStore.clear();
                    showLogin("Your saved session has expired. Please sign in.");
                    return;

                } catch (Exception e) {
                    System.err.println("Could not validate the stored token: "
                            + e.getMessage());
                    SessionStore.clear();
                }
            }
            showLogin(null);
        });
    }

    private static void showLogin(String notice) {
        LoginView loginView = new LoginView();
        new LoginController(loginView, new UserDAO());
        if (notice != null) {
            loginView.setNotice(notice);
        }
        loginView.setVisible(true);
    }
}
