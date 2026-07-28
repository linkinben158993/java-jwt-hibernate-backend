package io.linkinben.springbootsecurityjwt.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TracingFilterTest {

    private final TracingFilter filter = new TracingFilter();

    // --- generates a UUID trace id, echoes it on the response, and it is present during the chain ---
    @Test
    void generatesTraceId_echoesHeader_andIsInMdcDuringChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuringChain.set(MDC.get(MdcKeys.TRACE_ID));

        filter.doFilter(request, response, chain);

        String header = response.getHeader(MdcKeys.REQUEST_ID_HEADER);
        assertThat(header).isNotBlank();
        assertThat(mdcDuringChain.get()).isEqualTo(header); // same id in MDC and on the header
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();      // B1: cleared after the request
    }

    // --- reuses an inbound X-Request-Id instead of generating one ---
    @Test
    void reusesInboundRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcKeys.REQUEST_ID_HEADER, "inbound-correlation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(MdcKeys.REQUEST_ID_HEADER)).isEqualTo("inbound-correlation-id");
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }

    // --- MDC is cleared even when the downstream chain throws (no leak across pooled threads) ---
    @Test
    void clearsMdcEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain boom = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, boom))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }
}
