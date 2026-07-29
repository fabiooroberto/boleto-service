# Serviço de Registro de Boletos (Santander Bill Issuance API)

## Objetivo

Implementar um serviço que integra com a API do Santander para criar e gerenciar boletos, armazenando os dados em MongoDB.

**Nota:** O serviço é agnóstico à origem da API Santander. Funciona com:
- **Desenvolvimento:** Mock local (sem dependências externas)
- **Testes/Produção:** API real Santander (apenas trocar URL + credenciais)

Design doc: `docs/superpowers/specs/2026-07-29-santander-boleto-integration-design.md`

---

## Stack Técnico

- **Spring Boot:** 4.1.0 (latest)
- **Java:** 21 LTS
- **Banco de Dados:** MongoDB 7 (via Docker)
- **Build:** Maven (multi-módulo)
- **Versionamento:** Git

---

## Arquitetura

### Componentes Principais

1. **API do Serviço** (boleto-service:8080) - Spring Boot REST API
   - Agnóstica à origem (Mock ou Sandbox)
   - Orquestra lógica, persiste em MongoDB
2. **Cliente Santander Abstrato** - Interface `SantanderClient`
   - Implementação genérica com HTTP calls
   - Não sabe se chama Mock ou API real
3. **Mock Santander** (santander-mock:8081) - Simula API real
   - 7 endpoints (OAuth + CRUD boletos)
   - Persiste em MongoDB (collection: boletos_mock)
4. **MongoDB** - Persistência centralizada
   - Collection boletos_mock (santander-mock)
   - Collection boletos (boleto-service)
5. **OAuth Manager** - Gerencia tokens com cache (1h TTL)

### Estrutura do Projeto

```
├── boleto-service/          # Aplicação principal (7 endpoints REST)
├── santander-mock/          # Mock da API Santander (7 endpoints + OAuth)
├── docker-compose.yml       # Orquestração de containers
└── pom.xml                  # POM pai (multi-módulo)
```

### Padrão de Implementação

**Abstração Agnóstica:**
- Interface `SantanderClient` define contrato genérico
- Implementação única `RestSantanderClient` faz HTTP calls para URL configurada
- API não conhece "mock" vs "real" — apenas uma URL + credenciais
- Migração para API real: **trocar URL + env vars, sem mudança de código**

**Configuração via `application.yml`:**
```yaml
santander:
  api:
    base-url: http://santander-mock:8081        # Dev: mock local
    # base-url: https://trust-open.api.santander.com.br/collection_bill_management/v2  # Prod: API real
    workspace-id: workspace-mock
    client-id: ${SANTANDER_CLIENT_ID}
    client-secret: ${SANTANDER_CLIENT_SECRET}
```

**Nenhuma mudança de código ao migrar para API real.**

---

## Autenticação

### OAuth 2.0 - Client Credentials Flow

O serviço utilizará **OAuth 2.0** para autenticar na API do Santander.

**Credenciais (variáveis de ambiente):**
```properties
SANTANDER_CLIENT_ID=seu_client_id
SANTANDER_CLIENT_SECRET=seu_client_secret
```

**Endpoints OAuth:**
- **Mock:** `http://santander-mock:8081/oauth/token`
- **Sandbox (Real):** `https://trust-open.api.santander.com.br/oauth/token`

### Cache e Reutilização de Tokens

**Duração e Validade:**
- Token tem **validade de 1 hora (3600 segundos)**
- Uma vez obtido, é **cacheado em memória** durante toda sua validade
- Todas as requisições **reutilizam o mesmo token** sem refazer autenticação

**Fluxo de Reutilização:**
1. **Primeira requisição:** Serviço solicita token ao endpoint OAuth
2. **Recepção:** Obtém `access_token` com TTL de 1 hora
3. **Cache:** Armazena token em memória com timestamp de expiração
4. **Requisições subsequentes:** Reutiliza token em cache (sem nova autenticação)
5. **Renovação automática:** Quando token aproxima-se da expiração (margem de ~1 minuto), obtém novo token

**Benefícios da Reutilização:**
- ✅ **Reduz latência:** Evita 99% das requisições de autenticação OAuth
- ✅ **Diminui carga:** Menos chamadas ao endpoint OAuth
- ✅ **Eficiência:** 100 requisições = apenas 1-2 chamadas OAuth (ao invés de 100)
- ✅ **Renovação automática:** Nunca usa token expirado

**Configuração:**
```yaml
santander:
  auth:
    tokenTtlSeconds: 3600              # 1 hora
    refreshMarginSeconds: 60           # Renovar 1 minuto antes de expirar
```

---

## Fluxo de Integração

1. **Receber requisição** de criar boleto no serviço
2. **Verificar cache de token:**
   - Se válido (não expirado): reutilizar
   - Se expirado/ausente: obter novo token via OAuth
3. **Chamar cliente Santander** com token (Mock ou API Real)
4. **Armazenar dados** do boleto no MongoDB
5. **Retornar resposta** com dados do boleto criado

**Nota:** O token é automaticamente renovado quando aproxima-se da expiração (1 minuto antes), garantindo que todas as requisições usem um token válido.

---

## Referência da API Santander

**URL Base (Sandbox):** `https://trust-open.api.santander.com.br/collection_bill_management/v2`

**Documentação Oficial:** https://developer.santander.com.br/api/user-guide/issuance-of-bills

### 7 Endpoints (Mock + API Real)

#### OAuth
- **POST /oauth/token** — Obter access token (válido 1h)

#### Boleto CRUD (5 endpoints)
1. **POST /workspaces/{workspace_id}/bank_slips** — Criar boleto
2. **PATCH /workspaces/{workspace_id}/bank_slips** — Atualizar boleto
3. **GET /workspaces/{workspace_id}/bank_slips** — Listar boletos (paginado)
4. **GET /workspaces/{workspace_id}/bank_slips/{bank_slip_id}** — Obter boleto por ID
5. **GET /bills** — Listar boletos (alternativo)
6. **GET /bills/{bill_id}** — Obter boleto (alternativo)
7. **POST /bills/{bill_id}/bank_slips** — Gerar PDF

**Todos exigem:** `Authorization: Bearer {access_token}`

**Referência detalhada de payloads:** Ver design doc em `docs/superpowers/specs/2026-07-29-santander-boleto-integration-design.md`

---

## Testes Integrados (Futuro)

Facilitar transição do Mock para API Real do Sandbox:

1. **Ambiente de Desenvolvimento:**
   - Usar Mock (sem dependências externas)
   - Testes unitários e de integração rápidos

2. **Ambiente de Testes Integrados:**
   - Trocar configuração para `type: sandbox`
   - Usar credenciais reais do Santander
   - Validar fluxo completo com API real
   - Sem alteração de código na lógica de negócio

3. **Implementação:**
   - Interface `SantanderClient` abstrai implementação
   - Factory/Strategy pattern para selecionar cliente
   - Mesmo contrato de requisição/resposta em ambas

---

## Próximos Passos (Implementação)

1. Criar estrutura multi-módulo Maven (Parent POM + santander-mock + boleto-service)
2. Implementar santander-mock (7 endpoints + OAuth + MongoDB)
3. Implementar boleto-service (5 endpoints REST)
4. Implementar SantanderClient abstrato (interface + RestSantanderClient)
5. Implementar OAuthManager (cache de token com 1h TTL)
6. Implementar BoletoRepository (MongoDB)
7. Configurar Docker Compose (MongoDB + santander-mock + boleto-service)
8. Testes unitários e de integração
9. Validação contra API real Santander

**Design Doc Aprovado:** `docs/superpowers/specs/2026-07-29-santander-boleto-integration-design.md`
