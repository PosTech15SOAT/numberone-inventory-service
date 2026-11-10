package br.com.numberone.inventory.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
class InventoryAccessFilter extends OncePerRequestFilter {
    private static final String INTERNAL = "/internal/inventory/";
    private static final Set<String> HEALTH = Set.of("/actuator/health", "/actuator/health/liveness",
            "/actuator/health/readiness");
    private static final Pattern OS_READ = Pattern.compile(INTERNAL + "(items|operations)/[^/]+");
    private static final Pattern ADMIN = Pattern.compile("/api/admin/(itens(?:/[^/]+)?"
            + "|estoque/(?:entrada|itens/[^/]+/movimentacoes))");

    private final ObjectMapper json;
    private final boolean gatewayTrusted;
    private final String osToken;
    private final String billingToken;

    InventoryAccessFilter(ObjectMapper json,
                          @Value("${app.security.gateway-trusted:false}") boolean gatewayTrusted,
                          @Value("${app.security.os-token:}") String osToken,
                          @Value("${app.security.billing-token:}") String billingToken) {
        if (!osToken.isBlank() && osToken.equals(billingToken)) {
            throw new IllegalArgumentException("Configure tokens distintos para OS e Billing.");
        }
        this.json = json;
        this.gatewayTrusted = gatewayTrusted;
        this.osToken = osToken;
        this.billingToken = billingToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        UUID correlation = readUuid(request.getHeader(InventoryHttp.CORRELATION));
        UUID responseCorrelation = correlation == null ? UUID.randomUUID() : correlation;
        request.setAttribute(InventoryHttp.REQUEST_CORRELATION, responseCorrelation);
        response.setHeader(InventoryHttp.CORRELATION, responseCorrelation.toString());

        if ("GET".equals(method) && HEALTH.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        boolean os = matches(auth, osToken);
        boolean billing = matches(auth, billingToken);
        if (path.startsWith(INTERNAL)) {
            if (!os && !billing) {
                error(response, 401, "UNAUTHENTICATED", "Identidade interna invalida.", responseCorrelation);
                return;
            }
            boolean osRoute = "GET".equals(method) && OS_READ.matcher(path).matches()
                    || "POST".equals(method) && path.equals(INTERNAL + "consumptions");
            boolean billingRoute = "POST".equals(method) && path.equals(INTERNAL + "consultar-precos");
            if (!(os && osRoute || billing && billingRoute)) {
                error(response, 403, "FORBIDDEN", "Rota fora do escopo do servico.", responseCorrelation);
                return;
            }
            if (correlation == null) {
                error(response, 400, "INVALID_REQUEST", "X-Correlation-Id deve ser um UUID.", responseCorrelation);
                return;
            }
        } else if (ADMIN.matcher(path).matches()) {
            UUID subject = readUuid(request.getHeader("X-Authenticated-Subject"));
            if (os || billing) {
                error(response, 403, "FORBIDDEN", "Identidade interna fora do escopo administrativo.", responseCorrelation);
                return;
            }
            if (!gatewayTrusted || subject == null || !"ACTIVE".equals(request.getHeader("X-Authenticated-Status"))) {
                error(response, 401, "UNAUTHENTICATED", "Identidade administrativa invalida.", responseCorrelation);
                return;
            }
            String roles = request.getHeader("X-Authenticated-Roles");
            if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
                error(response, 403, "FORBIDDEN", "Acesso administrativo necessario.", responseCorrelation);
                return;
            }
            request.setAttribute(InventoryHttp.ADMIN_SUBJECT, subject);
        } else {
            error(response, 404, "RESOURCE_NOT_FOUND", "Rota inexistente.", responseCorrelation);
            return;
        }
        chain.doFilter(request, response);
    }

    private static UUID readUuid(String header) {
        try {
            return InventoryHttp.uuid(header);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean matches(String authorization, String token) {
        return !token.isBlank() && authorization != null
                && MessageDigest.isEqual(("Bearer " + token).getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8));
    }

    private void error(HttpServletResponse response, int status, String code, String message, UUID correlation)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), new InventoryHttp.Problem(code, message, correlation, null));
    }
}
