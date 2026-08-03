package io.linkinben.springbootsecurityjwt.tracing;

/** MDC key names and the correlation header, shared by the tracing filter, aspect and log pattern. */
public final class MdcKeys {

    /** Per-request correlation id — constant for the whole request (incl. async via decorator). */
    public static final String CORRELATION_ID = "correlationId";

    /** Authenticated user id, added to MDC after authentication (O6). */
    public static final String USER_ID = "uId";

    /**
     * Request/response correlation header. The front-end generates and sends it; the backend honours it
     * inbound if present, else generates a UUID.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private MdcKeys() {
    }
}
