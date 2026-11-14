> Fase 4: o deploy manual não usa GitHub Environment; configurações e pré-requisitos estão no procedimento `docs/phase4.md` do repositório de infraestrutura Cloud. Referências abaixo ao Environment são históricas.

# Kubernetes Inventory

`base` mantém Deployment/Service usados no Kind. `overlays/kind` continua com
ConfigMap local, porta de Service 8082 e Secret PostgreSQL resolvido pela composição
`numberone-billing-service/dev/kind`. Não aplicar overlay Kind na AWS.

`overlays/homologation` acrescenta configuração AWS e ServiceAccount sem token padrão,
probes liveness/readiness com banco, heap limitado e rolling update com surge 1.
Workflow `.github/workflows/deploy.yml` cria namespace e Secret
`numberone-inventory-database` com DB_URL SSL, DB_USERNAME e DB_PASSWORD e implanta
por digest. Service AWS ClusterIP:8080; container inventory/Deployment numberone-inventory.

Renderização local (requer kubectl, jq e Bash):

```bash
K8S_NAMESPACE=numberone-inventory \
IMAGE_URI=registry.example.invalid/numberone-inventory@sha256:0000000000000000000000000000000000000000000000000000000000000000 \
bash scripts/render-kubernetes.sh > /tmp/inventory-review.yaml
```

O digest sintético é somente para inspeção/schema, não uma imagem implantável.
[Variáveis, dependências e roteiro AWS dos três serviços](../../numberone-billing-service/docs/deployment/eks-readiness/README.md).

Fluxo de branches, CI e deploy manual: [CI/CD](CI-CD.md).
