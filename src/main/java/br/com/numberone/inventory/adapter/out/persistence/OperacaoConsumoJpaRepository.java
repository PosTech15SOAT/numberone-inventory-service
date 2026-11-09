
package br.com.numberone.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OperacaoConsumoJpaRepository
        extends JpaRepository<OperacaoConsumoEntity, UUID> {
}
