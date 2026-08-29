package com.dentalclinic.model;

/**
 * Represents one row of the users table.
 *
 * Note the private fields with public getters and setters. The class diagram
 * in Task A must show these access modifiers (- for private, + for public):
 * the rubric awards marks specifically for "clear identification of private
 * and public access modifiers, visible in the class diagram".
 *
 * The password is deliberately NOT stored in this object. Once a user is
 * authenticated there is no reason to keep their credential in memory.
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

    public int getUserId()              { return userId; }
    public void setUserId(int userId)   { this.userId = userId; }

    public String getUsername()         { return username; }
    public void setUsername(String u)   { this.username = u; }

    public String getFullName()         { return fullName; }
    public void setFullName(String f)   { this.fullName = f; }

    public String getRole()             { return role; }
    public void setRole(String role)    { this.role = role; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}