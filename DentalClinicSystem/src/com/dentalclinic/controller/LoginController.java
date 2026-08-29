package com.dentalclinic.controller;

import com.dentalclinic.model.User;
import com.dentalclinic.model.UserDAO;
import com.dentalclinic.model.Validator;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import javax.swing.SwingWorker;

/**
 * LOGIC TIER - handles staff authentication.
 *
 * The controller is the only class that knows both the view and the DAO.
 * The view knows nothing about the database; the DAO knows nothing about
 * Swing. That separation is the core of the MVC pattern.
 *
 * THREADING: authentication queries MySQL, so it runs inside a SwingWorker.
 * If it ran directly in the button handler the login window would freeze
 * for as long as the database took to answer.
 */
public class LoginController {

    private final LoginView view;
    private final UserDAO   userDAO;

    public LoginController(LoginView view, UserDAO userDAO) {
        this.view    = view;
        this.userDAO = userDAO;

        // Observer pattern: subscribe to the view's events
        this.view.addLoginListener(e -> attemptLogin());
        this.view.addCancelListener(e -> System.exit(0));
    }

    private void attemptLogin() {
        final String username = view.getUsername();
        final String password = view.getPassword();

        // ---- client-side validation happens before we bother the database ----
        if (!Validator.isNotEmpty(username)) {
            view.setMessage("Username is required.");
            return;
        }
        if (!Validator.isNotEmpty(password)) {
            view.setMessage("Password is required.");
            return;
        }

        view.setMessage(" ");
        view.setBusy(true);

        // ---- THREADING: query the database off the Event Dispatch Thread ----
        new SwingWorker<User, Void>() {

            @Override
            protected User doInBackground() throws Exception {
                // Runs on a worker thread. Safe to be slow here.
                return userDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                // SwingWorker guarantees this runs back on the EDT,
                // so touching Swing components here is safe.
                view.setBusy(false);
                try {
                    User user = get();
                    if (user == null) {
                        view.setMessage("Invalid username or password.");
                        view.clearPassword();
                        return;
                    }
                    openMainWindow(user);
                } catch (Exception ex) {
                    view.setMessage("Cannot reach database. Is WAMP running?");
                    System.err.println("Login error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openMainWindow(User user) {
        MainView mainView = new MainView();
        new AppointmentController(mainView, user);   // controller wires itself in
        mainView.setVisible(true);
        view.dispose();                              // close the login window
    }
}
