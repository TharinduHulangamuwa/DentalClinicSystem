package com.dentalclinic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for the exception carrying HTTP status codes from the service.
 *
 * The client reacts to the STATUS rather than the message text, so its
 * behaviour does not break when a message is reworded on the server.
 *
 * @author [Your Name]
 */
public class ApiExceptionTest {

    @Test public void testStatusIsStored() {
        assertEquals(404, new ApiException(404, "gone").getStatus());
    }
    @Test public void testMessageIsStored() {
        assertEquals("gone", new ApiException(404, "gone").getMessage());
    }
    @Test public void testNotFoundIsRecognised() {
        assertTrue(new ApiException(ApiException.NOT_FOUND, "x").isNotFound());
    }
    @Test public void testConflictIsRecognised() {
        assertTrue(new ApiException(ApiException.CONFLICT, "x").isConflict());
    }
    @Test public void testUnauthorizedIsRecognised() {
        assertTrue(new ApiException(ApiException.UNAUTHORIZED, "x").isUnauthorized());
    }
    @Test public void testForbiddenIsRecognised() {
        assertTrue(new ApiException(ApiException.FORBIDDEN, "x").isForbidden());
    }
    @Test public void testConflictIsNotNotFound() {
        assertFalse(new ApiException(ApiException.CONFLICT, "x").isNotFound());
    }
    @Test public void testUnauthorizedIsNotForbidden() {
        assertFalse("401 means not signed in; 403 means signed in but not allowed",
                    new ApiException(ApiException.UNAUTHORIZED, "x").isForbidden());
    }
    @Test public void testServerErrorMatchesNoSpecificCheck() {
        ApiException e = new ApiException(ApiException.SERVER_ERROR, "boom");
        assertFalse(e.isNotFound());
        assertFalse(e.isConflict());
        assertFalse(e.isUnauthorized());
        assertFalse(e.isForbidden());
    }
    @Test public void testStandardCodeConstants() {
        assertEquals(400, ApiException.BAD_REQUEST);
        assertEquals(401, ApiException.UNAUTHORIZED);
        assertEquals(403, ApiException.FORBIDDEN);
        assertEquals(404, ApiException.NOT_FOUND);
        assertEquals(409, ApiException.CONFLICT);
        assertEquals(500, ApiException.SERVER_ERROR);
    }
    @Test public void testIsAnException() {
        assertTrue(new ApiException(400, "x") instanceof Exception);
    }
}
