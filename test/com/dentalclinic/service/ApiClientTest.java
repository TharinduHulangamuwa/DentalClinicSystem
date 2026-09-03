package com.dentalclinic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the REST client's own state.
 *
 * The methods that perform network calls are covered by manual integration
 * tests, because they need the server running. What is tested here is the
 * token handling, which is what makes every authenticated request work.
 *
 * @author [Your Name]
 */
public class ApiClientTest {

    private ApiClient client;

    @Before
    public void setUp() {
        client = new ApiClient("http://localhost:8081");
    }

    @Test public void testDefaultConstructorUsesLocalhostAndServicePort() {
        assertTrue(new ApiClient().getBaseUrl()
                .contains(String.valueOf(ApiServer.PORT)));
    }
    @Test public void testBaseUrlIsStored() {
        assertEquals("http://192.168.1.20:8081",
                new ApiClient("http://192.168.1.20:8081").getBaseUrl());
    }
    @Test public void testNoTokenBeforeSignIn() {
        assertNull(client.getToken());
        assertFalse(client.hasToken());
    }
    @Test public void testTokenCanBeSet() {
        client.setToken("abc123");
        assertEquals("abc123", client.getToken());
        assertTrue(client.hasToken());
    }
    @Test public void testEmptyTokenCountsAsNoToken() {
        client.setToken("");
        assertFalse("An empty string must not be sent as a bearer token",
                    client.hasToken());
    }
    @Test public void testNullTokenCountsAsNoToken() {
        client.setToken("abc");
        client.setToken(null);
        assertFalse(client.hasToken());
    }
    @Test public void testTokenCanBeReplaced() {
        client.setToken("first");
        client.setToken("second");
        assertEquals("second", client.getToken());
    }
    @Test public void testServicePortIsTheDocumentedOne() {
        assertEquals(8081, ApiServer.PORT);
    }
    @Test public void testTwoClientsHoldIndependentTokens() {
        ApiClient other = new ApiClient("http://localhost:8081");
        client.setToken("one");
        other.setToken("two");
        assertEquals("one", client.getToken());
        assertEquals("two", other.getToken());
    }
}
