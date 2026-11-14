
package br.com.numberone.inventory.adapter.out.persistence;

import br.com.numberone.inventory.domain.model.OperacaoConsumoEstoque;
import br.com.numberone.inventory.domain.model.StatusOperacaoConsumo;
import br.com.numberone.inventory.application.port.out.OperacaoConsumoRepositoryPort;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Repository
public class OperacaoConsumoPersistenceAdapter
        implements OperacaoConsumoRepositoryPort {

    private final OperacaoConsumoJpaRepository repository;

    public OperacaoConsumoPersistenceAdapter(
            OperacaoConsumoJpaRepository repository) {

        this.repository = repository;
    }

    @Override
    public void bloquearOperacao(UUID operationId) {
        repository.bloquearOperacao(operationId);
    }

    @Override
    public Optional<OperacaoConsumoEstoque> buscarPorOperationId(
            UUID operationId) {

        return repository.findById(operationId)
                .map(OperacaoConsumoEntity::toDomain);
    }

    @Override
    public OperacaoConsumoEstoque salvar(
            OperacaoConsumoEstoque operacao) {

        Optional<OperacaoConsumoEntity> existente =
                repository.findById(operacao.getOperationId());

        if (existente.isPresent()) {
            OperacaoConsumoEntity entity = existente.get();

            OperacaoConsumoEstoque atual =
                    entity.toDomain();

            if (!(atual.getStatus() == StatusOperacaoConsumo.CLOSED && operacao.getRequestHash() == null)
                    && !atual.possuiMesmoRequestHash(operacao.getRequestHash())) {
                throw new IllegalStateException(
                        "operationId ja utilizado com dados diferentes."
                );
            }

            if (!Objects.equals(atual.getServiceOrderId(),
                    operacao.getServiceOrderId())
                    || !Objects.equals(atual.getServiceOrderItemId(),
                    operacao.getServiceOrderItemId())
                    || !Objects.equals(atual.getResponsibleUserId(), operacao.getResponsibleUserId())
                    || atual.getCompensationId() != null && !atual.getCompensationId().equals(operacao.getCompensationId())) {
                throw new IllegalStateException(
                        "Dados da operacao existente nao correspondem."
                );
            }

            if (entity.getStatus() != operacao.getStatus()) {
                entity.atualizarStatus(operacao.getStatus(), operacao.getCompensationId());
            }

            return repository.save(entity).toDomain();
        }

        if (operacao.getStatus() != StatusOperacaoConsumo.PROCESSING && operacao.getStatus() != StatusOperacaoConsumo.CLOSED) {
            throw new IllegalStateException(
                    "Uma nova operacao deve iniciar em PROCESSING ou CLOSED."
            );
        }

        OperacaoConsumoEntity novaEntity =
                OperacaoConsumoEntity.fromDomain(operacao);

        return repository.save(novaEntity).toDomain();
    }
}
