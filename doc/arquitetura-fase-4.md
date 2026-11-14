# Arquitetura do Inventory · Fase 4

Inventory mantém o catálogo de peças e insumos, preços, saldo e movimentações. A OS é responsável pelo trabalho executado. Billing é responsável pelo valor comercial aprovado. As duas aplicações leem o Inventory por HTTP, mantendo o banco de estoque isolado.

## Caminho do código

- `adapter/in/web` recebe consultas de preço, catálogo e comandos de consumo e compensação. `InventoryAccessFilter` separa rotas administrativas e escopos internos de OS e Billing. As rotas entre serviços exigem Bearer e `X-Correlation-Id` UUID na revisão `253bf8a`.
- `application/port/in` define os casos de uso. `application/service` implementa consulta, consumo e compensação. `application/port/out` define as operações que os casos de uso pedem à persistência.
- `domain/model` representa materiais, movimentações, tentativas e evidências. `adapter/out/persistence` conecta as portas à persistência JPA e PostgreSQL. Flyway controla o schema.

## Contratos e efeitos duráveis

| Consumidor | Operação | Resultado |
| --- | --- | --- |
| Billing | `POST /internal/inventory/consultar-precos` | Consulta os preços por UUID e devolve valores em BRL. |
| OS | `GET /internal/inventory/items/{inventoryItemId}` | Referência do material e situação ativa. |
| OS | `POST /internal/inventory/consumptions` (CT-05) | Consome o lote em uma transação ou registra recusa; a chave corresponde ao `operationId`. |
| OS | `GET /internal/inventory/operations/{operationId}` (CT-08) | Consulta o resultado persistido; 404 com `UNKNOWN` indica ausência observada. |
| OS | `POST /internal/inventory/consumptions/{operationId}/reversal` (CT-08B) | Fecha tentativa pendente com `CLOSED` ou registra a movimentação inversa com `REVERSED`, com evidência e `compensationId`. |
| Entrada administrativa | `/api/admin/itens` e `/api/admin/estoque` | Cadastro, consultas, entrada e listagem das movimentações. |

O fluxo em uso realiza baixa direta no início do item. A decisão do saldo e a movimentação são duráveis no PostgreSQL do Inventory. A tentativa idempotente conserva identidade e hash. Uma tentativa fechada antes da baixa impede consumo atrasado. A reversão preserva o movimento original, exige evidência e só efetua um movimento inverso. Inventory participa da Saga por REST, e a OS decide quando avançar. O SQS da jornada comercial faz o caminho Billing → OS.

## Deploy e evidências

Java 21, Spring Boot, Flyway e PostgreSQL. A branch do Item 2 acrescenta Docker, ClusterIP `numberone-inventory-service:8082` com container em 8080, readiness com banco e pipeline independente. Na branch `develop` desta revisão o Item 2 aguarda merge. O CI da proposta passou com 98 testes executados e 84,6% de cobertura de linhas no teste local do mesmo conjunto. Um teste de preço preexistente permanece desabilitado. O pipeline incluiu PostgreSQL real e gate JaCoCo de 80%.

[Collection Postman](api/numberone-inventory.postman_collection.json) · [Integração I1](i1.md) · [Compensação I2](i2.md) · [PR Docker/Kubernetes/CI](https://github.com/PosTech15SOAT/numberone-inventory-service/pull/5) · [CI de b4541da](https://github.com/PosTech15SOAT/numberone-inventory-service/actions/runs/43352559150)

O Billing em `develop` já omite Bearer; o Inventory publicado responde 401 a chamadas internas sem identidade. Confirmar com a equipe o contrato comum antes de ligar os serviços no EKS. O deploy e a política de entrada externa ainda precisam de evidência no ambiente final.
