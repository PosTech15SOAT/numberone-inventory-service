package br.com.numberone.inventory.adapter.in.web;

import br.com.numberone.inventory.application.port.in.GerenciarCatalogoUseCase;
import br.com.numberone.inventory.application.port.in.ConsumirEstoqueUseCase;
import br.com.numberone.inventory.application.port.in.CompensarEstoqueUseCase;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InventoryOperationalLoggingTest {
    @Test
    void operationLookupUsesStructuredFieldsAndCorrelation() {
        var consumo = mock(ConsumirEstoqueUseCase.class);
        var id = UUID.randomUUID();
        var correlation = UUID.randomUUID();
        when(consumo.consultar(id)).thenReturn(Optional.empty());
        var controller = new InventoryInternalController(mock(GerenciarCatalogoUseCase.class), consumo,
                mock(CompensarEstoqueUseCase.class));
        var logger = (Logger) LoggerFactory.getLogger(InventoryInternalController.class);
        var captured = new ListAppender<ILoggingEvent>();
        captured.start();
        logger.addAppender(captured);
        MDC.put("correlation_id", correlation.toString());
        try {
            assertThat(controller.consultar(id.toString(), correlation).getStatusCode().value()).isEqualTo(404);
            var event = captured.list.getFirst();
            assertThat(event.getMDCPropertyMap()).containsEntry("correlation_id", correlation.toString());
            assertThat(event.getKeyValuePairs()).anySatisfy(kv -> {
                assertThat(kv.key).isEqualTo("event.code");
                assertThat(kv.value).isEqualTo("INVENTORY_OPERATION_QUERIED");
            });
            assertThat(event.getKeyValuePairs()).anySatisfy(kv -> {
                assertThat(kv.key).isEqualTo("operation.id");
                assertThat(kv.value).isEqualTo(id);
            });
        } finally {
            MDC.remove("correlation_id");
            logger.detachAppender(captured);
            captured.stop();
        }
    }
    @Test
    void loggingFailureDoesNotChangeResponse() {
        var consumo = mock(ConsumirEstoqueUseCase.class);
        var id = UUID.randomUUID();
        when(consumo.consultar(id)).thenReturn(Optional.empty());
        var controller = new InventoryInternalController(mock(GerenciarCatalogoUseCase.class), consumo,
                mock(CompensarEstoqueUseCase.class));
        var logger = (Logger) LoggerFactory.getLogger(InventoryInternalController.class);
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> failing = mock(Appender.class);
        doThrow(new IllegalStateException("test")).when(failing).doAppend(any());
        logger.addAppender(failing);
        try {
            assertThat(controller.consultar(id.toString(), UUID.randomUUID()).getStatusCode().value()).isEqualTo(404);
        } finally {
            logger.detachAppender(failing);
        }
    }

}
