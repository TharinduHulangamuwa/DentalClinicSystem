package com.dentalclinic.model;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * DAO for the sessions table - the desktop equivalent of a server-side
 * session store.
 *
 * THREE SECURITY DECISIONS, each worth explaining in the report:
 *
 * 1. Tokens come from SecureRandom, not Math.random() or a timestamp. A
 *    predictable token could be guessed by someone who knew roughly when a
 *    colleague signed in.
 *
 * 2. Only the SHA-256 HASH of the token is stored, exactly as passwords are.
 *    Someone who reads this table cannot replay what they find.
 *
 * 3. Validation checks three things together: the token exists, the session
 *    is still active, and it has not passed expires_at. Checking only the
 *    first would let a signed-out or stale token back in.
 *
 * @author [Your Name]
 */
public class SessionDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Bytes of entropy in a token. 32 bytes = 256 bits. */
    private static final int TOKEN_BYTES = 32;

    /**
     * Creates a session row and returns the RAW token.
     * The raw value is never stored - only its hash.
     */
    public String create(User user, boolean rememberMe) throws SQLException {
        String rawToken = generateToken();
        int minutes = rememberMe
                ? Session.REMEMBER_DAYS * 24 * 60
                : Session.TIMEOUT_MINUTES;

        String sql = "INSERT INTO sessions "
                   + "(token_hash, user_id, expires_at, remember_me, machine_name) "
                   + "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(rawToken));
            ps.setInt(2, user.getUserId());
            ps.setInt(3, minutes);
            ps.setBoolean(4, rememberMe);
            ps.setString(5, machineName());
            ps.executeUpdate();
        }
        return rawToken;
    }

    /**
     * Checks a token and returns the user it belongs to.
     * @return the User, or null if unknown, closed or expired
     */
    public User validate(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isEmpty()) {
            return null;
        }
        String sql = "SELECT u.user_id, u.username, u.full_name, u.role "
                   + "FROM sessions s JOIN users u ON s.user_id = u.user_id "
                   + "WHERE s.token_hash = ? AND s.active = 1 AND s.expires_at > NOW()";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(rawToken));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("username"),
                                    rs.getString("full_name"), rs.getString("role"));
                }
            }
        }
        return null;
    }

    /** Refreshes last_activity and pushes expires_at forward. */
    public void touch(String rawToken) throws SQLException {
        String sql = "UPDATE sessions SET last_activity = NOW(), "
                   + "expires_at = DATE_ADD(NOW(), INTERVAL "
                   + "  CASE WHEN remember_me = 1 THEN ? ELSE ? END MINUTE) "
                   + "WHERE token_hash = ? AND active = 1";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Session.REMEMBER_DAYS * 24 * 60);
            ps.setInt(2, Session.TIMEOUT_MINUTES);
            ps.setString(3, PasswordUtil.hash(rawToken));
            ps.executeUpdate();
        }
    }

    /** Closes one session - the equivalent of invalidating a cookie. */
    public void end(String rawToken) throws SQLException {
        if (rawToken == null) {
            return;
        }
        String sql = "UPDATE sessions SET active = 0 WHERE token_hash = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(rawToken));
            ps.executeUpdate();
        }
    }

    /** Closes every session for one user, on every machine. */
    public int endAllForUser(int userId) throws SQLException {
        String sql = "UPDATE sessions SET active = 0 WHERE user_id = ? AND active = 1";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    /** Housekeeping, run at startup. */
    public int purgeExpired() throws SQLException {
        String sql = "UPDATE sessions SET active = 0 "
                   + "WHERE active = 1 AND expires_at < NOW()";
        try (Statement st = DBConnection.getConnection().createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    /** Rows of vw_active_sessions, for the sessions screen. */
    public List<String[]> activeSessions() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT username, full_name, machine_name, created_at, "
                   + "last_activity, idle_minutes FROM vw_active_sessions";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("machine_name"),
                    rs.getString("created_at"),
                    rs.getString("last_activity"),
                    rs.getString("idle_minutes") + " min"
                });
            }
        }
        return rows;
    }

    /** 256 bits of cryptographically secure randomness, URL-safe encoded. */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Records which machine opened the session, for the audit trail. */
    private String machineName() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return (host == null || host.isEmpty()) ? "unknown" : host;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
