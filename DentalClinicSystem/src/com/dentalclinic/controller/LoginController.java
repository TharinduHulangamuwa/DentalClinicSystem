package com.dentalclinic.controller;

import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionDAO;
import com.dentalclinic.model.SessionStore;
import com.dentalclinic.model.User;
import com.dentalclinic.model.UserDAO;
import com.dentalclinic.model.Validator;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import javax.swing.SwingWorker;

/**
 * LOGIC TIER - authentication and session creation.
 *
 * The controller is the only class that knows both the view and the DAOs.
 * The view knows nothing about the database; the DAOs know nothing about
 * Swing. That separation is the core of the MVC pattern.
 *
 * A successful sign-in does two things: it verifies the password, and it
 * creates a session row identified by a random token. If the user asked to
 * be remembered, the token is also written to a local file so the next
 * launch can resume without a password.
 *
 * THREADING: authentication queries MySQL, so it runs inside a SwingWorker.
 * Run directly in the button handler it would freeze the window for as long
 * as the database took to answer.
 *
 * @author [Your Name]
 */
public class LoginController {

    private final LoginView  view;
    private final UserDAO    userDAO;
    private final SessionDAO sessionDAO = new SessionDAO();

    public LoginController(LoginView view, UserDAO userDAO) {
        this.view    = view;
        this.userDAO = userDAO;

        // Observer pattern: subscribe to the view's events
        this.view.addLoginListener(e -> attemptLogin());
        this.view.addCancelListener(e -> System.exit(0));
    }

    private void attemptLogin() {
        final String  username = view.getUsername();
        final String  password = view.getPassword();
        final boolean remember = view.isRememberMe();

        // Client-side checks first, so we do not trouble the database with
        // an obviously incomplete attempt.
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

        new SwingWorker<User, Void>() {

            @Override
            protected User doInBackground() throws Exception {
                // WORKER THREAD - slow work is safe here
                return userDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                // EVENT DISPATCH THREAD - Swing calls are safe again
                view.setBusy(false);
                try {
                    User user = get();
                    if (user == null) {
                        view.setMessage("Invalid username or password.");
                        view.clearPassword();
                        return;
                    }
                    openSession(user, remember);

                } catch (Exception ex) {
                    view.setMessage("Cannot reach the database. Is WampServer running?");
                    System.err.println("Sign-in error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /** Creates the session row, optionally persists it, and opens the app. */
    private void openSession(User user, boolean remember) {
        try {
            String token = sessionDAO.create(user, remember);
            Session.start(token, user, remember);

            if (remember) {
                SessionStore.save(token);
            } else {
                // A token left over from a previous "remember me" must not
                // survive an ordinary sign-in, or the next launch would
                // silently resume the wrong session.
                SessionStore.clear();
            }

            MainView mainView = new MainView();
            new AppointmentController(mainView, user);
            mainView.setVisible(true);
            view.dispose();

        } catch (Exception ex) {
            view.setMessage("Could not start a session: " + ex.getMessage());
        }
    }
}
