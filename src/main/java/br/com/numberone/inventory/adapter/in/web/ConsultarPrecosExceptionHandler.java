
package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.service.ConsultarPrecosService.ConsultaPrecosException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = ConsultarPrecosController.class)
public class ConsultarPrecosExceptionHandler {

    @ExceptionHandler(ConsultaPrecosException.class)
    public ResponseEntity<ErroResponse> tratarConsultaPrecos(
            ConsultaPrecosException exception) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(
                        exception.getCodigo(),
                        "Não foi possível consultar os preços de todos os itens.",
                        exception.getItemIds()
                ));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(
            Exception exception) {

        return ResponseEntity
                .badRequest()
                .body(new ErroResponse(
                        "INVALID_REQUEST",
                        "A requisição contém dados inválidos.",
                        List.of()
                ));
    }

    public record ErroResponse(
            String code,
            String message,
            List<UUID> inventoryItemIds
    ) {
    }
}
