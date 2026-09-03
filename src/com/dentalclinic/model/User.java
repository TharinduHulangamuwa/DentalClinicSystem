package com.dentalclinic.model;

/**
 * One row of the users table.
 *
 * Note the private fields with public accessors. The class diagram in Task A
 * must show these modifiers (- private, + public): the marking criteria award
 * marks specifically for access modifiers being visible in the class diagram.
 *
 * The password is deliberately NOT held here. Once a user is authenticated
 * there is no reason to keep their credential in memory.
 *
 * @author [Your Name]
 */
public class User {

    private int    userId;
    private String username;
    private String fullName;
    private String role;

    public User() { }

    public User(int userId, String username, String fullName, String role) {
        this.userId   = userId;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
    }

    public int getUserId()            { return userId; }
    public void setUserId(int v)      { this.userId = v; }

    public String getUsername()       { return username; }
    public void setUsername(String v) { this.username = v; }

    public String getFullName()       { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public String getRole()           { return role; }
    public void setRole(String v)     { this.role = v; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
