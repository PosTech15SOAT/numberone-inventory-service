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

As chamadas internas devem enviar `X-Correlation-Id: <uuid>`. Cadastro de itens e entrada de estoque exigem `X-Authenticated-Subject: <uuid>` para registrar o autor declarado; a aplicação não autentica essa identidade.

**Débito técnico:** MVP — Ausência de autenticação e autorização HTTP nos microserviços. Identidade dos serviços, autorização, NetworkPolicies e TLS/mTLS ficam para evolução futura.

## Preparação integrada de deploy

A documentação transversal está no clone irmão
[Billing: prontidão EKS](../numberone-billing-service/docs/deployment/eks-readiness/README.md).
A orquestração local compartilhada passa a ficar em
[Billing/dev/kind](../numberone-billing-service/dev/kind/README.md), reutilizando este overlay Kind.


### Observabilidade Datadog

A imagem empacota `dd-java-agent` 1.64.2 com checksum e o carrega por `-javaagent`; o Deployment configura `DD_SERVICE=numberone-inventory`, `DD_AGENT_HOST=status.hostIP`, `DD_TRACE_ENABLED=true` e `DD_LOGS_INJECTION=true`. O Deployment usa `DD_ENV=academy`. A renderização do deploy recebe `AWS_ACCOUNT_ID` de `vars.AWS_ACCOUNT_ID` e usa `GITHUB_SHA` como `DD_VERSION`; o entrypoint acrescenta `aws_account_id:<conta>` em `DD_TAGS`, preservando as demais tags. Cada integrante usa a variável de sua conta. Confirme o Agent APM no nó e a porta TCP 8126, coleta de stdout e que não há injeção automática adicional. No kind, tracing permanece desabilitado. O tracer não exige que o Agent esteja disponível para iniciar. A imagem desabilita query strings em spans HTTP; não habilite captura de Authorization, payloads ou outros dados sensíveis.

Os logs em stdout usam JSON ECS, com timestamp, serviço, nível, logger, mensagem e contexto; `dd.trace_id` e `dd.span_id` são inseridos pelo tracer quando há span ativo. Verifique o parsing de objetos ECS e IDs como strings no Datadog antes de assumir a ligação log/trace. O filtro mantém `correlation_id` no MDC durante a requisição, restaura o contexto e preserva o header; endpoints internos continuam exigindo UUID. Busque operações por correlation ID e pelos IDs de operação e compensação registrados nos eventos CT-05/CT-08. A propagação HTTP automática precisa ser verificada com Gateway e serviços reais; SQS e ponte HTTP não comprovam uma trace única, portanto mantenha a correlação operacional por IDs. Métricas HTTP/APM dependem da ingestão; a OS conserva DogStatsD existente. Para o vídeo, mostrar os três serviços, chamadas, erros e latência, log e trace relacionados e uma Saga por IDs, verificando ausência de segredos. Nenhuma recepção no Datadog foi comprovada localmente.

Os eventos operacionais seguem o padrão do Billing: ECS em stdout, SLF4J com `event.code` e IDs estruturados, `correlation_id` e mensagens sem payloads ou credenciais. Para APM, filtre por `env:academy`, `service:numberone-inventory`, `version:<sha>` e `aws_account_id:<conta>`. `DD_TAGS` não garante que a conta apareça no JSON de logs: validar enriquecimento no Agent/pipeline e ingestão real no EKS.

No JSON ECS, `service.name` deriva de `spring.application.name` (`numberone-inventory-service`); o tracer injeta `dd.service=numberone-inventory`. São campos de origens distintas; o mapeamento usado pelo pipeline Datadog deve ser verificado no EKS.

A nomenclatura de atributos usa ponto para entidades ou contextos (`service_order.id`, `budget.revision_number`, `event.code`) e underline para palavras compostas (`service_order_item.id`). O campo técnico independente `correlation_id` é usado no MDC e JSON; `X-Correlation-Id` continua como header. Campos oficiais como `dd.trace_id` e `dd.span_id` são preservados.
