# NumberOne Inventory Service

Microsserviço de estoque (Java 21, Spring Boot 4.1.2, PostgreSQL 17, Flyway).

## Organização em Clean Architecture

```text
br.com.numberone.inventory
├── domain/model                # Entidades, regras e objetos de valor, sem dependência de Spring
├── application
│   ├── port/in                 # Contratos dos casos de uso
│   ├── port/out                # Contratos de persistência (interfaces)
│   └── service                 # Implementação dos casos de uso e orquestração transacional
└── adapter
    ├── in/web                  # Controllers REST, DTOs e filtros HTTP
    └── out/persistence         # JPA, mapeamento de entidades e implementação das portas
```

**Regra de dependência:** `adapter -> application -> domain`. Os controllers dependem dos contratos `application.port.in`; os serviços dependem de `application.port.out`; JPA implementa as portas de saída. Anotações de transação e injeção continuam na implementação dos casos de uso, preservando o comportamento do Spring.

## Funcionalidades preservadas

- Consulta de preços: `POST /internal/inventory/consultar-precos`.
- Consumo idempotente de estoque: `POST /internal/inventory/consumptions`.
- Consulta de operação: `GET /internal/inventory/operations/{operationId}`.
- Compensação de consumo: `POST /internal/inventory/consumptions/{operationId}/reversal`.
- Administração do catálogo e entradas de estoque: `/api/admin/...`.

## Validação

No Windows, na raiz do projeto:

```bash
./mvnw.cmd clean test -Dtest='!InventoryServiceApplicationTests,!InventoryI2PostgresTest'
```

Para executar a suíte completa, configure um PostgreSQL de testes disponível e as variáveis de ambiente de conexão conforme `.env.example`. O teste `InventoryI2PostgresTest` requer `I2_POSTGRES_TEST=true` e um banco **descartável**, pois grava dados. O teste `contextLoads` também requer datasource válido.

**Importante:** a refatoração estrutural não substitui a execução real de Maven, Flyway e testes de integração. A criação de RDS no Learner Lab pode ser bloqueada por IAM.

## Aplicar esta atualização

Copie `src/` e `README.md` para a branch de trabalho, **removendo antes o diretório antigo** `src/main/java/br/com/numberone/inventory/domain/port/out` para não manter interfaces duplicadas. Preserve `.git` e suas configurações locais. Execute `git status` e `./mvnw.cmd clean test` antes do commit.

## Contrato REST de preços para Billing

`POST /internal/inventory/consultar-precos` recebe `{"inventoryItemIds":["<uuid>"]}` e retorna `{"items":[{"inventoryItemId":"<uuid>","salePrice":125.50,"currency":"BRL"}]}`. O campo `salePrice` é um **número JSON**, com duas casas decimais, em BRL.

A consulta não altera nem reserva estoque. IDs repetidos são consultados uma única vez. Se qualquer insumo estiver ausente ou sem preço válido, a API **não retorna preços parciais**: responde HTTP `404` com `code` (`INVENTORY_ITEMS_NOT_FOUND` ou `INVENTORY_ITEM_WITHOUT_PRICE`), `message` e `inventoryItemIds` afetados. Entrada inválida retorna HTTP `400`.

O Billing deve enviar `Authorization: Bearer <token interno de Billing>` e `X-Correlation-Id: <uuid>`, conforme o filtro de acesso interno. O token deve ser configurado de forma segura no ambiente de execução.
