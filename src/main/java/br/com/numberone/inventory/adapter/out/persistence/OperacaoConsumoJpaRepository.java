
package br.com.numberone.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@Repository
public interface OperacaoConsumoJpaRepository
        extends JpaRepository<OperacaoConsumoEntity, UUID> {
    // The transaction owns this lock even when the operation row does not exist yet.
    @Query(value = """
            SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(CAST(:operationId AS text), 46841))
            """, nativeQuery = true)
    int bloquearOperacao(@Param("operationId") UUID operationId);
}
