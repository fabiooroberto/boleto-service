# Design: Integração Santander Bill Issuance API

**Data:** 2026-07-29  
**Versão:** 1.0  
**Status:** Design Aprovado

---

## 1. Objetivo

Implementar um serviço Java que integra com a API do Santander para criar e gerenciar boletos (bills), com suporte a Mock para desenvolvimento e API real para testes/produção. Toda a lógica de persistência, autenticação e orquestração é agnóstica à origem da API Santander.

---

## 2. Requisitos

### Funcionais
- Serviço `boleto-service` expõe 5 endpoints REST para gerenciar boletos
- Serviço `santander-mock` simula a API Santander com 7 endpoints
- Ambos persistem em MongoDB
- Autenticação OAuth 2.0 (Client Credentials Flow) com cache de token
- Token válido por 1 hora, renovado automaticamente 1 minuto antes de expirar

### Não-Funcionais
- Java 21
- Spring Boot 4.1.0
- MongoDB (via Docker)
- Migração para API real Santander requer **apenas mudança de URL e credenciais**
- Código agnóstico: não conhece "mock" vs "real"

---

## 3. Arquitetura

### 3.1 Componentes Principais

```
┌─────────────────────────────────────────────────────┐
│  Cliente HTTP (Postman, Frontend, etc.)             │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │  boleto-service:8080        │
        │  (API Principal)            │
        ├─────────────────────────────┤
        │  Controller                 │
        │    ↓                        │
        │  Service (BoletoService)    │
        │    ↓                        │
        │  SantanderClient (interface)│
        │    ↓                        │
        │  RestTemplate (HTTP calls)  │
        └─────────────┬───────────────┘
                      │
        ┌─────────────┴──────────────────┐
        │                                │
        ▼                                ▼
   ┌─────────────────────┐      ┌──────────────────┐
   │ santander-mock:8081 │      │ MongoDB          │
   │ (Simula Santander)  │      │ - boletos (x2)   │
   │                     │      │ - tokens (cache) │
   │ 7 Endpoints OAuth + │      └──────────────────┘
   │ Boleto CRUD         │
   └─────────────────────┘
```

### 3.2 Estrutura do Projeto

```
teste3/
├── pom.xml                                    # Parent POM
├── docker-compose.yml
│
├── santander-mock/
│   ├── pom.xml
│   ├── src/main/java/com/santander/mock/
│   │   ├── SantanderMockApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── OAuthController.java           # POST /oauth/token
│   │   │   └── BoletoController.java          # 7 endpoints
│   │   ├── service/
│   │   │   └── BoletoMockService.java
│   │   ├── repository/
│   │   │   └── BoletoMockRepository.java
│   │   └── model/
│   │       ├── Boleto.java (Record)
│   │       ├── Pagador.java (Record)
│   │       └── OAuthTokenResponse.java
│   └── src/main/resources/
│       └── application.yml
│
├── boleto-service/
│   ├── pom.xml
│   ├── src/main/java/com/projeto/boleto/
│   │   ├── BoletoServiceApplication.java
│   │   ├── config/
│   │   │   ├── SantanderClientConfig.java
│   │   │   └── MongoConfig.java
│   │   ├── controller/
│   │   │   └── BoletoController.java          # 5 endpoints
│   │   ├── service/
│   │   │   ├── BoletoService.java
│   │   │   └── OAuthManager.java
│   │   ├── client/
│   │   │   ├── SantanderClient.java           # Interface
│   │   │   └── RestSantanderClient.java       # HTTP calls genéricas
│   │   ├── repository/
│   │   │   └── BoletoRepository.java
│   │   └── model/
│   │       ├── Boleto.java (Record)
│   │       ├── Pagador.java (Record)
│   │       ├── BoletoRequest.java
│   │       └── BoletoResponse.java
│   └── src/main/resources/
│       └── application.yml
│
└── README.md
```

---

## 4. Santander Mock Service

### 4.1 Responsabilidade
Simular a API real do Santander com dados persistidos em MongoDB. Não é código de produção — é um double para desenvolvimento.

### 4.2 Endpoints (7 total)

#### OAuth
- **POST /oauth/token** — Retorna access token válido por 1 hora

#### Boleto CRUD
1. **POST /workspaces/{workspace_id}/bank_slips** — Criar boleto
2. **PATCH /workspaces/{workspace_id}/bank_slips** — Atualizar boleto
3. **GET /workspaces/{workspace_id}/bank_slips** — Listar boletos (paginado)
4. **GET /workspaces/{workspace_id}/bank_slips/{bank_slip_id}** — Obter boleto por ID
5. **GET /bills** — Listar boletos (endpoint alternativo)
6. **GET /bills/{bill_id}** — Obter boleto por ID (endpoint alternativo)
7. **POST /bills/{bill_id}/bank_slips** — Gerar PDF

### 4.3 Autenticação
- Valida `client_id` e `client_secret` em requisição OAuth
- Gera JWT ou token aleatório válido por 3600 segundos
- Endpoints de boleto exigem `Authorization: Bearer {token}`

### 4.4 Persistência
- **Collection:** `boletos_mock` em MongoDB
- Todos os boletos criados/atualizados são salvos
- Permite testes de integração verificarem dados
- Isolado da collection `boletos` do boleto-service

---

## 5. Boleto Service (API Principal)

### 5.1 Responsabilidade
Orquestra lógica de negócio, abstrai comunicação com Santander e persiste dados em MongoDB.

### 5.2 Endpoints (7 total)

```
POST   /api/boletos                 # Criar boleto
GET    /api/boletos                 # Listar boletos (paginado)
GET    /api/boletos/{id}            # Obter boleto por ID
PATCH  /api/boletos/{id}            # Atualizar boleto
POST   /api/boletos/{id}/pdf        # Gerar PDF
GET    /api/bills                   # Listar boletos (endpoint alternativo)
GET    /api/bills/{bill_id}         # Obter boleto por ID (endpoint alternativo)
```

### 5.3 Abstração Santander Client

**Interface `SantanderClient`** — Contrato genérico:
```java
public interface SantanderClient {
    BoletoResponse createBoleto(BoletoRequest request);
    BoletoResponse updateBoleto(String boletoId, BoletoRequest request);
    Page<BoletoResponse> listBoletos(int page, int size);
    BoletoResponse getBoletoById(String boletoId);
    PdfResponse generatePdf(String boletoId);
    Page<BoletoResponse> listBills(int page, int size);              // Alternativo
    BoletoResponse getBillById(String billId);                       // Alternativo
}
```

**Implementação `RestSantanderClient`** — HTTP genérica:
- Faz chamadas REST para URL configurada
- Não conhece "mock" vs "sandbox"
- Reutiliza token do `OAuthManager`
- Trata erros de API e conversão JSON

### 5.4 OAuth Manager - Cache de Token

**Responsabilidades:**
1. Obter novo token via `POST /oauth/token`
2. Cachear em memória com timestamp de expiração
3. Reutilizar token se válido (economiza 99% das chamadas OAuth)
4. Renovar automaticamente 1 minuto antes de expirar

**Fluxo:**
```
Requisição 1: OAuthManager → POST /oauth/token → Cacheia
Requisições 2-100: Reutiliza token (sem nova auth)
Na requisição ~60: Token aproxima expiração → Renovar background
```

**Configuração:**
```yaml
santander:
  auth:
    token-ttl-seconds: 3600           # 1 hora
    refresh-margin-seconds: 60        # Renovar 1 min antes
```

### 5.5 Persistência MongoDB

- **Collection:** `boletos`
- Salva todos os boletos criados/atualizados
- Modelo completo conforme Santander: pagador, beneficiário, valor, datas, status

---

## 6. Data Models

### Records Java (sem Lombok)

```java
// Boleto persistido
public record Boleto(
    @Id String id,
    String nomePagador,
    String cpfCnpjPagador,
    BigDecimal valor,
    LocalDate dataVencimento,
    String status,
    LocalDateTime dataCriacao
) {}

// Pagador (parte da requisição)
public record Pagador(
    String nome,
    String cpfCnpj,
    String logradouro,
    String numero,
    String cidade,
    String estado,
    String cep
) {}

// Request para criar boleto
public record BoletoRequest(
    Pagador pagador,
    BigDecimal valor,
    LocalDate dataVencimento,
    String descricao
) {}

// Response da API Santander
public record BoletoResponse(
    String id,
    String status,
    BigDecimal valor,
    LocalDate dataVencimento
) {}
```

---

## 7. Fluxo de Integração

### 7.1 Criar Boleto (Passo-a-passo)

```
1. Cliente: POST /api/boletos com { pagador, valor, dataVencimento, ... }
   ↓
2. BoletoController valida request
   ↓
3. BoletoService.createBoleto(request)
   ↓
4. OAuthManager.getToken()
   → Se em cache e válido: retorna cached token
   → Se expirado: POST /oauth/token → cacheia novo
   ↓
5. RestSantanderClient.createBoleto(token, request)
   → RestTemplate chama URL configurada
   → POST {SANTANDER_API_BASE_URL}/workspaces/{id}/bank_slips
   ↓
6. Mock (ou API real) responde com boleto criado
   ↓
7. BoletoService persiste em MongoDB (collection: boletos)
   ↓
8. Response retorna 201 Created com boleto criado
```

### 7.2 Listar Boletos

```
1. Cliente: GET /api/boletos?page=0&size=10
   ↓
2. OAuthManager.getToken() (reutiliza cache)
   ↓
3. RestSantanderClient.listBoletos(token, page, size)
   → GET {SANTANDER_API_BASE_URL}/workspaces/{id}/bank_slips?page=0&size=10
   ↓
4. Response retorna page com boletos
```

---

## 8. Configuração & Migração

### 8.1 Development (Mock)

**`application.yml` padrão:**
```yaml
santander:
  api:
    base-url: http://santander-mock:8081
    workspace-id: workspace-mock
    client-id: test-client
    client-secret: test-secret
  auth:
    token-ttl-seconds: 3600
    refresh-margin-seconds: 60

spring:
  data:
    mongodb:
      uri: mongodb://root:password@localhost:27017/boleto_service
```

**Roda:** `docker-compose up` — tudo local, sem dependências externas

### 8.2 Integração/Produção (API Real)

**Apenas trocar valores:**
```yaml
santander:
  api:
    base-url: https://trust-open.api.santander.com.br/collection_bill_management/v2
    workspace-id: seu_workspace_real
    client-id: ${SANTANDER_CLIENT_ID_REAL}
    client-secret: ${SANTANDER_CLIENT_SECRET_REAL}

spring:
  data:
    mongodb:
      uri: ${MONGO_URI_PROD}
```

**Nenhuma mudança de código.** Apenas env vars e configuração.

---

## 9. Dependências Principais

### Parent POM
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>4.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
    <version>4.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>4.1.0</version>
</dependency>
<!-- Jackson (serialização JSON - incluído automaticamente) -->
<!-- SLF4J + Logback (logging - incluído automaticamente) -->
<!-- JUnit 5, Mockito (testes - incluído automaticamente) -->
```

---

## 10. Testing Strategy

### 10.1 Testes Unitários
- **OAuthManager:** Cache, renovação, expiração
- **BoletoService:** Lógica de negócio, chamadas corretas
- Mocks do `SantanderClient` e `BoletoRepository`

### 10.2 Testes de Integração
- `santander-mock` rodando (testcontainers ou real)
- Testes ponta-a-ponta: criar boleto → verificar MongoDB
- Validar fluxo completo

### 10.3 Testes de API
- Curl/Postman contra endpoints
- Verificar responses, status codes, validações

---

## 11. Docker Compose Orchestration

```yaml
version: '3.8'
services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: password
    volumes:
      - mongodb_data:/data/db

  santander-mock:
    build: ./santander-mock
    ports:
      - "8081:8081"
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://root:password@mongodb:27017/santander_mock
      SANTANDER_CLIENT_ID: test-client
      SANTANDER_CLIENT_SECRET: test-secret
    depends_on:
      - mongodb
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  boleto-service:
    build: ./boleto-service
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://root:password@mongodb:27017/boleto_service
      SANTANDER_API_BASE_URL: http://santander-mock:8081
      SANTANDER_CLIENT_ID: test-client
      SANTANDER_CLIENT_SECRET: test-secret
    depends_on:
      santander-mock:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mongodb_data:
```

**Executar:**
```bash
docker-compose up
```

---

## 12. Próximos Passos (Implementação)

1. Criar estrutura multi-módulo (Parent POM + módulos)
2. Implementar `santander-mock` com 7 endpoints + MongoDB
3. Implementar `boleto-service` com controller, service, client abstraction
4. Implementar `OAuthManager` com cache de token
5. Configurar Docker Compose
6. Testes unitários e de integração
7. Validação contra API real Santander (quando ready)

---

## 13. Notas

- **Isolamento:** Cada serviço persiste em sua própria collection MongoDB
- **Agnóstico:** `boleto-service` não conhece "mock" vs "real" — apenas uma URL e credenciais
- **Cache eficiente:** Token reusado por 1 hora, ~99% menos chamadas OAuth
- **Migração simples:** Trocar URL + env vars, pronto
- **Sem Lombok:** Usando Java Records para models (mais idiomático, sem processadores)
- **Escalável:** Padrão Strategy permite adicionar novos clients no futuro

---

**Design aprovado em:** 2026-07-29  
**Próxima fase:** Invocar writing-plans skill para plano de implementação
