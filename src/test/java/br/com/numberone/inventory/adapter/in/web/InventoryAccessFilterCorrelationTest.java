package br.com.numberone.inventory.adapter.in.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAccessFilterCorrelationTest {
    private final InventoryAccessFilter filter = new InventoryAccessFilter(new ObjectMapper());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void carriesCorrelationAndRestoresExistingContextEvenOnFailure() throws Exception {
        String correlation = UUID.randomUUID().toString();
        MDC.put("correlation_id", "previous");
        MDC.put("dd.trace_id", "trace");
        MDC.put("dd.span_id", "span");
        var request = new MockHttpServletRequest("GET", "/internal/inventory/items");
        request.addHeader("X-Correlation-Id", correlation);
        var response = new MockHttpServletResponse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("correlation_id")).isEqualTo(correlation);
            throw new jakarta.servlet.ServletException("failure");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(correlation);
        assertThat(MDC.get("correlation_id")).isEqualTo("previous");
        assertThat(MDC.get("dd.trace_id")).isEqualTo("trace");
        assertThat(MDC.get("dd.span_id")).isEqualTo("span");
    }
}
