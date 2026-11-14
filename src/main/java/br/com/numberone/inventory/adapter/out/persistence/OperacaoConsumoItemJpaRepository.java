
package br.com.numberone.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OperacaoConsumoItemJpaRepository
        extends JpaRepository<OperacaoConsumoItemEntity, UUID> {

    List<OperacaoConsumoItemEntity> findByOperationId(
            UUID operationId
    );
}
