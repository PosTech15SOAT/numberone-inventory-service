package br.com.numberone.inventory.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

@Component
class InventoryAccessFilter extends OncePerRequestFilter {
    private final ObjectMapper json;

    InventoryAccessFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID correlation;
        try {
            correlation = InventoryHttp.uuid(request.getHeader(InventoryHttp.CORRELATION));
        } catch (IllegalArgumentException ex) {
            correlation = null;
        }
        UUID responseCorrelation = correlation == null ? UUID.randomUUID() : correlation;
        request.setAttribute(InventoryHttp.REQUEST_CORRELATION, responseCorrelation);
        response.setHeader(InventoryHttp.CORRELATION, responseCorrelation.toString());

        String previousCorrelation = MDC.get("correlation_id");
        MDC.put("correlation_id", responseCorrelation.toString());
        try {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if ((path.equals("/internal/inventory") || path.startsWith("/internal/inventory/")) && correlation == null) {
                response.setStatus(400);
                response.setContentType("application/json");
                json.writeValue(response.getOutputStream(), new InventoryHttp.Problem(
                        "INVALID_REQUEST", "X-Correlation-Id deve ser um UUID.", responseCorrelation, null));
                return;
            }
            chain.doFilter(request, response);
        } finally {
            if (previousCorrelation == null) {
                MDC.remove("correlation_id");
            } else {
                MDC.put("correlation_id", previousCorrelation);
            }
        }
    }
}
