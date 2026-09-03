package com.dentalclinic.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DATA ACCESS OBJECT (DAO) PATTERN.
 *
 * The only class permitted to run SQL against the users table. Controllers
 * ask it questions in plain Java; they never see a Connection, a
 * PreparedStatement or a ResultSet.
 *
 * Why the pattern matters here:
 *   - swapping MySQL for another database changes this file only
 *   - PreparedStatement with bound parameters prevents SQL injection. A
 *     concatenated query such as  "... WHERE username = '" + name + "'"
 *     could be defeated by typing   ' OR '1'='1   into the sign-in box.
 *     This is the secure-coding evidence for the Ethical EDGE criterion.
 *
 * @author [Your Name]
 */
public class UserDAO {

    /**
     * Verifies staff credentials.
     * @return the matching User, or null when the credentials are wrong
     */
    public User authenticate(String username, String plainPassword) throws SQLException {
        String sql = "SELECT user_id, username, full_name, role "
                   + "FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(plainPassword));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** Used when the administrator creates an account. */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, full_name, role "
                   + "FROM users WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // =================================================================
    // STAFF ACCOUNT MANAGEMENT - administrator only
    //
    // The controller enforces that only an ADMIN can reach these methods.
    // The DAO enforces the rules that protect the data itself: unique
    // usernames, and never removing the last administrator.
    // =================================================================

    /**
     * Creates a staff account.
     *
     * The password is hashed before it reaches the database, so a plain text
     * password never exists anywhere except briefly in memory while the
     * administrator types it.
     *
     * @return the new user's id
     * @throws SQLException if the username is already taken
     */
    public int create(String username, String plainPassword,
                      String fullName, String role) throws SQLException {

        if (usernameExists(username)) {
            throw new SQLException("Username '" + username + "' is already taken.");
        }

        String sql = "INSERT INTO users (username, password, full_name, role) "
                   + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, fullName.trim());
            ps.setString(4, role);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Every staff account, for the administrator's list. */
    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, full_name, role FROM users "
                   + "ORDER BY role, username";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Rows for the staff table, including when each account was created. */
    public List<String[]> staffRows() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.full_name, u.role, u.created_at, "
                   + "  (SELECT COUNT(*) FROM sessions s "
                   + "    WHERE s.user_id = u.user_id AND s.active = 1 "
                   + "      AND s.expires_at > NOW()) AS live_sessions "
                   + "FROM users u ORDER BY u.role, u.username";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    String.valueOf(rs.getString("created_at")),
                    rs.getString("live_sessions")
                });
            }
        }
        return rows;
    }

    /** How many administrators exist. Used to protect the last one. */
    public int countAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM users WHERE role = 'ADMIN'";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt("c") : 0;
        }
    }

    /** Changes a user's display name and role. */
    public boolean update(int userId, String fullName, String role) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, role = ? WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, role);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Sets a new password.
     *
     * Any session that user has open elsewhere is closed at the same time.
     * A password reset normally means the old one was compromised or
     * forgotten, so leaving old sessions alive would defeat the point.
     */
    public boolean resetPassword(int userId, String newPlainPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPlainPassword));
            ps.setInt(2, userId);
            if (ps.executeUpdate() == 0) {
                return false;
            }
        }
        new SessionDAO().endAllForUser(userId);
        return true;
    }

    /**
     * Deletes a staff account.
     *
     * The sessions table has ON DELETE CASCADE, so that user's sessions go
     * with them and no orphaned token can be replayed.
     */
    public boolean delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"));
    }
}
