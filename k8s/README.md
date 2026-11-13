# Docker, Kubernetes e CI/CD do Inventory

A imagem usa Java 21 e executa com UID/GID 10001. O CI empacota o JAR após `mvnw clean verify`, aplica o limite de 80% de cobertura de linhas e testa a imagem contra um PostgreSQL descartável. O CI habilita `I2_POSTGRES_TEST=true` para executar também os testes de consumo e compensação no PostgreSQL.

## Validação local

Em um shell Bash com Docker, kubectl, jq e Java 21:

O `clean verify` requer `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` de um PostgreSQL descartável. Exporte `I2_POSTGRES_TEST=true` para reproduzir a execução de CI.

```bash
./mvnw --batch-mode clean verify
docker build -t numberone-inventory:local .
bash scripts/smoke-image.sh numberone-inventory:local
kubectl kustomize k8s/base
kubectl kustomize k8s/overlays/homologation
```

## Homologação no EKS

O workflow manual usa o environment `homologation`, aceita `main` e executa o CI no mesmo commit. Publique em `main` via PR originado de `develop`. Configure as revisões e restrições de branch do environment no GitHub. O workflow recebe o JAR validado, publica no ECR, resolve o digest, executa a validação no servidor e aguarda rollout e health via port-forward.

| Variável do environment | Valor esperado |
| --- | --- |
| `AWS_REGION`, `AWS_ACCOUNT_ID`, `EKS_CLUSTER_NAME` | Região, conta e nome do cluster fornecidos pela plataforma |
| `ECR_REPOSITORY` | `numberone-inventory-service` |
| `K8S_NAMESPACE` | Namespace provisionado para a aplicação |
| `DB_IDENTIFIER`, `DB_SECRET_ARN` | Instância RDS PostgreSQL e ARN do Secret com `username` e `password` |
| `DB_APP_USERNAME` | Usuário exclusivo da aplicação, diferente do master |

Configure os secrets do GitHub `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e `AWS_SESSION_TOKEN` para a sessão AWS Academy. O fluxo monta JDBC com TLS para `numberone_inventory` e sincroniza `numberone-inventory-database` no namespace. O usuário precisa ter acesso ao banco exclusivo e às migrations Flyway da aplicação. O Service interno é `numberone-inventory-service:8082` com destino na porta HTTP 8080 do container.

Para integrar com OS e Billing, a plataforma fornece `OS_TO_INVENTORY_TOKEN` e `BILLING_TO_INVENTORY_TOKEN` em `numberone-inventory-integrations`. O ConfigMap opcional `numberone-inventory-runtime` recebe as opções operacionais. Configure `GATEWAY_HEADERS_TRUSTED` no ConfigMap de runtime após validar a entrada confiável e a rede no Item 3. As opções de integração são injetadas quando configuradas. Mudanças de valores usados como variáveis de ambiente exigem recriar os Pods.

## Diagnóstico e rollback

O resumo do workflow registra commit, digest, namespace e resultado. Use o estado de Deployment, Pods e Service para investigar rollout. Para voltar a uma imagem anterior, recupere seu digest no resumo da execução anterior e confirme que a imagem entende o schema atual. Aplique o digest anterior e aguarde readiness antes de liberar tráfego:

```bash
kubectl -n "$K8S_NAMESPACE" set image deployment/numberone-inventory=inventory="$PREVIOUS_IMAGE_URI"
kubectl -n "$K8S_NAMESPACE" rollout status deployment/numberone-inventory --timeout=10m
```

O rollback da imagem não reverte migrations ou configurações. Trate correções de schema pelo procedimento de banco da equipe.
