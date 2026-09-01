package com.dentalclinic.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"));
    }
}
