package com.dentalclinic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for the User entity, including the role check that guards every
 * staff account operation.
 *
 * @author [Your Name]
 */
public class UserTest {

    @Test public void testConstructorStoresId() {
        assertEquals(1, new User(1, "admin", "Clinic Administrator", "ADMIN")
                .getUserId());
    }
    @Test public void testConstructorStoresUsername() {
        assertEquals("admin", new User(1, "admin", "Clinic Administrator", "ADMIN")
                .getUsername());
    }
    @Test public void testConstructorStoresFullName() {
        assertEquals("Clinic Administrator",
                new User(1, "admin", "Clinic Administrator", "ADMIN").getFullName());
    }
    @Test public void testConstructorStoresRole() {
        assertEquals("ADMIN", new User(1, "admin", "Clinic Administrator", "ADMIN")
                .getRole());
    }
    @Test public void testEmptyConstructorLeavesFieldsNull() {
        User u = new User();
        assertNull(u.getUsername());
        assertNull(u.getRole());
        assertEquals(0, u.getUserId());
    }

    @Test public void testSettersUpdateEveryField() {
        User u = new User();
        u.setUserId(7);
        u.setUsername("nimali");
        u.setFullName("Nimali Perera");
        u.setRole("STAFF");

        assertEquals(7, u.getUserId());
        assertEquals("nimali", u.getUsername());
        assertEquals("Nimali Perera", u.getFullName());
        assertEquals("STAFF", u.getRole());
    }

    @Test public void testIsAdminTrueForAdminRole() {
        assertTrue(new User(1, "admin", "Admin", "ADMIN").isAdmin());
    }
    @Test public void testIsAdminFalseForStaffRole() {
        assertFalse(new User(2, "nimali", "Nimali", "STAFF").isAdmin());
    }
    @Test public void testIsAdminIgnoresCase() {
        assertTrue("Role comparison must not be case sensitive",
                   new User(1, "admin", "Admin", "admin").isAdmin());
    }
    @Test public void testIsAdminFalseForUnknownRole() {
        assertFalse(new User(3, "x", "X", "MANAGER").isAdmin());
    }
    @Test public void testToStringShowsNameAndRole() {
        String s = new User(1, "admin", "Clinic Administrator", "ADMIN").toString();
        assertTrue(s.contains("Clinic Administrator"));
        assertTrue(s.contains("ADMIN"));
    }
}
