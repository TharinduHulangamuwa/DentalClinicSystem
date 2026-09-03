package com.dentalclinic.service;

/**
 * A failure reported by the web service, carrying the HTTP status code so the
 * client can react differently to "not found", "conflict" and "unauthorised".
 *
 * Using status codes rather than parsing error text means the client's
 * behaviour does not break when a message is reworded.
 *
 * @author [Your Name]
 */
public class ApiException extends Exception {

    /** Standard HTTP status codes used by this API. */
    public static final int BAD_REQUEST  = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN    = 403;
    public static final int NOT_FOUND    = 404;
    public static final int CONFLICT     = 409;
    public static final int SERVER_ERROR = 500;

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean isNotFound()     { return status == NOT_FOUND; }
    public boolean isConflict()     { return status == CONFLICT; }
    public boolean isUnauthorized() { return status == UNAUTHORIZED; }
    public boolean isForbidden()    { return status == FORBIDDEN; }
}
