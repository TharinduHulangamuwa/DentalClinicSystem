package com.dentalclinic.controller;

import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionStore;
import com.dentalclinic.model.User;
import com.dentalclinic.model.Validator;
import com.dentalclinic.service.ApiClient;
import com.dentalclinic.service.ApiException;
import com.dentalclinic.view.LoginView;
import com.dentalclinic.view.MainView;
import javax.swing.SwingWorker;

/**
 * LOGIC TIER - authentication and session creation.
 *
 * The controller is the only class that knows both the view and the API
 * client. The view knows nothing about the network; the client knows nothing
 * about Swing. That separation is the core of the MVC pattern, and here it
 * also keeps the whole database tier on the other side of an HTTP boundary.
 *
 * The client sends the credentials to POST /api/login. The SERVER verifies
 * the password and issues a session token; this class never touches the
 * database and holds no database credentials. If the user asked to be
 * remembered, the token is written to a local file so the next launch can
 * resume without a password.
 *
 * THREADING: the HTTP call runs inside a SwingWorker. Run directly in the
 * button handler it would freeze the window for as long as the network and
 * the server took to answer - which over a network is longer than a local
 * database query, so the need is greater here, not less.
 *
 * @author [Your Name]
 */
public class LoginController {

    private final LoginView view;
    private final ApiClient api;

    public LoginController(LoginView view, ApiClient api) {
        this.view = view;
        this.api  = api;

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
                // WORKER THREAD - the HTTP call happens here, off the EDT.
                // The server checks the password and issues the session
                // token; this client never sees the database.
                return api.signIn(username, password, remember);
            }

            @Override
            protected void done() {
                // EVENT DISPATCH THREAD - Swing calls are safe again
                view.setBusy(false);
                try {
                    openSession(get(), remember);

                } catch (java.util.concurrent.ExecutionException ee) {
                    Throwable cause = ee.getCause();

                    if (cause instanceof ApiException
                            && ((ApiException) cause).isUnauthorized()) {
                        view.setMessage("Invalid username or password.");
                        view.clearPassword();
                    } else {
                        view.setMessage("Cannot reach the clinic server. "
                                + "Is ApiServer running?");
                        System.err.println("Sign-in error: " + cause);
                    }
                } catch (Exception ex) {
                    view.setMessage("Sign-in failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /** Stores the token issued by the server and opens the main window. */
    private void openSession(User user, boolean remember) {
        try {
            String token = api.getToken();
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
            new AppointmentController(mainView, user, api);
            mainView.setVisible(true);
            view.dispose();

        } catch (Exception ex) {
            view.setMessage("Could not start a session: " + ex.getMessage());
        }
    }
}
