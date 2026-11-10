
package br.com.numberone.inventory.adapter.out.persistence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemEstoqueJpaRepository
        extends JpaRepository<ItemEstoqueEntity, UUID> {

    Optional<ItemEstoqueEntity> findByCodigo(String codigo);
    List<ItemEstoqueEntity> findByAtivoTrueOrderByCodigo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM ItemEstoqueEntity i
            WHERE i.id = :id
            """)
    Optional<ItemEstoqueEntity> buscarPorIdParaAtualizacao(
            @Param("id") UUID id
    );
}
