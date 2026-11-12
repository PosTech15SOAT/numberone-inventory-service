package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.ConflitoIdempotenciaException;
import br.com.numberone.inventory.application.service.ConflitoCompensacaoException;
import br.com.numberone.inventory.application.service.ItemIndisponivelException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@RestControllerAdvice(assignableTypes = {InventoryInternalController.class, InventoryAdminController.class})
class InventoryExceptionHandler {
    @ExceptionHandler(ConflitoCompensacaoException.class)
    ResponseEntity<InventoryHttp.Problem> compensacao(ConflitoCompensacaoException ex, HttpServletRequest request) {
        return ResponseEntity.status(409).body(new InventoryHttp.Problem(ex.getMessage(),
                "Compensacao ou tentativa em conflito: " + ex.getMessage(),
                (UUID) request.getAttribute(InventoryHttp.REQUEST_CORRELATION), ex.getOperationId(), ex.getCompensationId()));
    }
    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class, HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class, MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<InventoryHttp.Problem> invalid(HttpServletRequest request) {
        return problem(request, 400, "INVALID_REQUEST", "A requisicao contem dados invalidos.", null);
    }

    @ExceptionHandler(ConflitoIdempotenciaException.class)
    ResponseEntity<InventoryHttp.Problem> conflito(ConflitoIdempotenciaException ex, HttpServletRequest request) {
        return problem(request, 409, "IDEMPOTENCY_CONFLICT", "Identidade de operacao ja utilizada com outro pedido.",
                ex.getOperationId());
    }

    @ExceptionHandler(ItemIndisponivelException.class)
    ResponseEntity<InventoryHttp.Problem> ausente(HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) && request.getRequestURI().endsWith("/consumptions")) {
            return invalid(request);
        }
        return problem(request, 404, "RESOURCE_NOT_FOUND", "Item de estoque inexistente.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<InventoryHttp.Problem> integridade(HttpServletRequest request) {
        if (request.getAttribute(InventoryHttp.ADMIN_SUBJECT) != null) {
            return problem(request, 409, "PERSISTENCE_CONFLICT", "Dados em conflito com o cadastro.", null);
        }
        return indisponivel(request);
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<InventoryHttp.Problem> indisponivel(HttpServletRequest request) {
        return problem(request, 503, "DEPENDENCY_UNAVAILABLE", "Estoque temporariamente indisponivel.", null);
    }

    private ResponseEntity<InventoryHttp.Problem> problem(HttpServletRequest request, int status, String code,
                                                         String message, UUID operationId) {
        var correlation = (UUID) request.getAttribute(InventoryHttp.REQUEST_CORRELATION);
        return ResponseEntity.status(status).body(new InventoryHttp.Problem(code, message, correlation, operationId));
    }
}
