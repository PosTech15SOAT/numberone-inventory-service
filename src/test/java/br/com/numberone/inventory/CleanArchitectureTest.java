package br.com.numberone.inventory;

import br.com.numberone.inventory.adapter.in.web.ConsultarPrecosController;
import br.com.numberone.inventory.application.port.in.*;
import br.com.numberone.inventory.application.port.out.*;
import br.com.numberone.inventory.application.service.*;
import br.com.numberone.inventory.adapter.out.persistence.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Ensures dependency direction stays from adapters to application contracts. */
class CleanArchitectureTest {
    @Test
    void casosDeUsoImplementamContratosDeEntrada() {
        assertTrue(ConsumirEstoqueUseCase.class.isAssignableFrom(ConsumirEstoqueService.class));
        assertTrue(CompensarEstoqueUseCase.class.isAssignableFrom(CompensarEstoqueService.class));
        assertTrue(GerenciarCatalogoUseCase.class.isAssignableFrom(CatalogoEstoqueService.class));
        assertTrue(ConsultarPrecosUseCase.class.isAssignableFrom(ConsultarPrecosService.class));
        assertTrue(ConsultarSaldoUseCase.class.isAssignableFrom(ConsultarSaldoDisponivelService.class));
    }

    @Test
    void adaptadoresDePersistenciaImplementamContratosDeSaida() {
        assertTrue(ItemEstoqueRepositoryPort.class.isAssignableFrom(ItemEstoquePersistenceAdapter.class));
        assertTrue(OperacaoConsumoRepositoryPort.class.isAssignableFrom(OperacaoConsumoPersistenceAdapter.class));
        assertTrue(OperacaoConsumoItemRepositoryPort.class.isAssignableFrom(OperacaoConsumoItemPersistenceAdapter.class));
    }

    @Test
    void controllerDePrecosDependeDeAbstracao() {
        var campos = List.of(ConsultarPrecosController.class.getDeclaredFields());
        assertTrue(campos.stream().anyMatch(f -> f.getType().equals(ConsultarPrecosUseCase.class)));
        assertFalse(campos.stream().anyMatch(f -> f.getType().equals(ConsultarPrecosService.class)));
    }

    @Test
    void portasSaoInterfacesSemDependenciaDeFramework() {
        for (var contrato : List.of(ConsumirEstoqueUseCase.class, CompensarEstoqueUseCase.class,
                GerenciarCatalogoUseCase.class, ConsultarPrecosUseCase.class,
                ItemEstoqueRepositoryPort.class, OperacaoConsumoRepositoryPort.class)) {
            assertTrue(contrato.isInterface());
            assertTrue(Modifier.isPublic(contrato.getModifiers()));
            for (var annotation : contrato.getAnnotations()) {
                assertFalse(annotation.annotationType().getName().startsWith("org.springframework"));
            }
        }
    }
}
