package io.linkinben.springbootsecurityjwt.tracing;

/** MDC key names and the correlation header, shared by the tracing filter, aspect and log pattern. */
public final class MdcKeys {

    /** Per-request correlation/trace id — constant for the whole request (incl. async via decorator). */
    public static final String TRACE_ID = "traceId";

    /** Authenticated user id, added to MDC after authentication (O6). */
    public static final String USER_ID = "uId";

    /** Request/response correlation header. Honoured inbound if present, else a UUID is generated. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private MdcKeys() {
    }
}
