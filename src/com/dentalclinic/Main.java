package com.dentalclinic;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.LoginController;
import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionStore;
import com.dentalclinic.model.User;
import com.dentalclinic.service.ApiClient;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * THE CLIENT PROCESS. Start ApiServer first, then this.
 *
 * Startup sequence:
 *   1. Apply the native look and feel.
 *   2. Look for a stored session token. Ask the SERVER whether it is still
 *      valid; if so, sign the user straight in, otherwise show sign-in.
 *
 * Note what is NOT here any more: no database check, no JDBC, no credentials.
 * This process talks only HTTP. That is what makes the system distributed.
 *
 * Step 2 is the desktop equivalent of a browser sending a persistent cookie:
 * the token proves a previous successful authentication, so no password is
 * required until it expires or is revoked.
 *
 * Note SwingUtilities.invokeLater. Swing components must be created on the
 * Event Dispatch Thread, including the very first window.
 *
 * @author [Your Name]
 */
public class Main {

    /** One client for the whole application, shared by both controllers. */
    private static final ApiClient API = new ApiClient();

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set the system look and feel: "
                    + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            String storedToken = SessionStore.load();

            if (storedToken != null) {
                try {
                    User user = API.restoreSession(storedToken);

                    if (user != null) {
                        Session.start(storedToken, user, true);
                        MainView mainView = new MainView();
                        new AppointmentController(mainView, user, API);
                        mainView.setVisible(true);
                        System.out.println("[SESSION] resumed for "
                                + user.getUsername());
                        return;
                    }
                    SessionStore.clear();
                    showLogin("Your saved session has expired. Please sign in.");
                    return;

                } catch (Exception e) {
                    // Most often the server is not running yet.
                    System.err.println("Could not restore the session: "
                            + e.getMessage());
                    SessionStore.clear();
                    showLogin(null);
                    return;
                }
            }
            showLogin(null);
        });
    }

    private static void showLogin(String notice) {
        LoginView loginView = new LoginView();
        new LoginController(loginView, API);
        if (notice != null) {
            loginView.setNotice(notice);
        }
        loginView.setVisible(true);
    }
}
