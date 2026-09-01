package com.dentalclinic.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SINGLETON PATTERN - the currently authenticated staff session.
 *
 * WHY THIS CLASS EXISTS
 * A Java Swing application does not run in a browser, so the HTTP session
 * cookie a web application relies on (JSESSIONID and similar) is simply not
 * available. The concept behind it still is: state identifying who is signed
 * in, for how long, and whether that authentication is still valid.
 *
 * This class implements that concept explicitly. Each sign-in generates a
 * cryptographically random token; the token is hashed and stored in the
 * sessions table, and the raw value is held here in memory. Every user action
 * refreshes the last-activity timestamp, and an idle session expires.
 *
 * The parallel with a web session is exact:
 *     browser cookie        ->  the token held here and in the local file
 *     server session store  ->  the sessions table in MySQL
 *     session timeout       ->  expires_at plus the idle check below
 *     session.invalidate()  ->  SessionDAO.end()
 *
 * @author [Your Name]
 */
public class Session {

    /** Idle minutes after which a session closes automatically. */
    public static final int TIMEOUT_MINUTES = 30;

    /** Days a "keep me signed in" token survives without use. */
    public static final int REMEMBER_DAYS = 7;

    /** Minutes remaining at which the user is warned. */
    public static final int WARN_MINUTES = 5;

    private static Session instance;

    private final String  token;
    private final User    user;
    private final Date    loginTime;
    private final boolean remembered;
    private Date lastActivity;

    private Session(String token, User user, boolean remembered) {
        this.token        = token;
        this.user         = user;
        this.remembered   = remembered;
        this.loginTime    = new Date();
        this.lastActivity = new Date();
    }

    // ---------------- lifecycle ----------------

    public static synchronized void start(String token, User user, boolean remembered) {
        instance = new Session(token, user, remembered);
        System.out.println("[SESSION] opened for " + user.getUsername()
                + "  token=" + mask(token) + "  remembered=" + remembered);
    }

    public static synchronized void end() {
        if (instance != null) {
            System.out.println("[SESSION] closed for " + instance.user.getUsername()
                    + " after " + instance.getDurationMinutes() + " minute(s)");
        }
        instance = null;
    }

    public static synchronized Session getInstance() {
        return instance;
    }

    public static synchronized boolean isActive() {
        return instance != null && !instance.hasTimedOut();
    }

    // ---------------- activity ----------------

    /** Called on every user action, pushing the idle deadline back. */
    public synchronized void touch() {
        this.lastActivity = new Date();
    }

    public synchronized boolean hasTimedOut() {
        return getIdleMinutes() >= TIMEOUT_MINUTES;
    }

    public synchronized long getIdleMinutes() {
        return (new Date().getTime() - lastActivity.getTime()) / 60000;
    }

    /** Minutes left before automatic sign-out, shown in the header. */
    public synchronized long getMinutesRemaining() {
        long left = TIMEOUT_MINUTES - getIdleMinutes();
        return left < 0 ? 0 : left;
    }

    public synchronized boolean isExpiringSoon() {
        return getMinutesRemaining() <= WARN_MINUTES;
    }

    // ---------------- accessors ----------------

    public String  getToken()     { return token; }
    public User    getUser()      { return user; }
    public boolean isRemembered() { return remembered; }

    public long getDurationMinutes() {
        return (new Date().getTime() - loginTime.getTime()) / 60000;
    }

    public String getLoginTimeText() {
        return new SimpleDateFormat("HH:mm").format(loginTime);
    }

    /** Shows only enough of a token to identify it; never log the whole thing. */
    public static String mask(String t) {
        return (t == null || t.length() < 10) ? "????" : t.substring(0, 10) + "...";
    }
}
