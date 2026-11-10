package br.com.numberone.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MovimentacaoEstoqueJpaRepository extends JpaRepository<MovimentacaoEstoqueEntity, UUID> {
    List<MovimentacaoEstoqueEntity> findByOperationIdOrderByItemEstoqueId(UUID operationId);
    List<MovimentacaoEstoqueEntity> findByItemEstoqueIdOrderByCreatedAtDescIdAsc(UUID itemEstoqueId);
}
