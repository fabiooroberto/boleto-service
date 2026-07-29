# Integração Santander Bill Issuance API - Plano de Implementação

> **Para execução com agentes:** USE superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para implementar tarefa-a-tarefa.

**Objetivo:** Implementar um serviço Java que integra com a API do Santander (mock ou real) para criar e gerenciar boletos, com autenticação OAuth, cache de token, e persistência em MongoDB.

**Arquitetura:** Projeto multi-módulo Maven com dois serviços: `santander-mock` (simula API) e `boleto-service` (API principal agnóstica).

**Stack:** Java 21, Spring Boot 4.1.0, MongoDB 7, Maven, Docker

## Global Constraints

- **Java:** 21 LTS
- **Spring Boot:** 4.1.0
- **MongoDB:** 7 (via Docker)
- **Sem Lombok:** Usar Java Records
- **2 Collections:** `boletos_mock` (mock) e `boletos` (service)
- **7 Endpoints Mock:** 1 OAuth + 6 CRUD
- **7 Endpoints Service:** 5 CRUD + 2 /bills

---

## Task 1: Criar Parent POM Multi-Módulo

- [ ] Criar `pom.xml` com `<packaging>pom</packaging>` e módulos
- [ ] Incluir dependências: Spring Boot 4.1.0, MongoDB, Security, JWT, Jackson
- [ ] Verificar estrutura
- [ ] Commit: "setup: create parent pom multi-module structure"

---

## Task 2: Criar santander-mock estrutura base

- [ ] Criar `santander-mock/pom.xml` referenciando parent
- [ ] Criar `SantanderMockApplication.java`
- [ ] Criar `application.yml` (port 8081, MongoDB uri)
- [ ] Verificar estrutura
- [ ] Commit: "setup: create santander-mock module base"

---

## Task 3: Criar boleto-service estrutura base

- [ ] Criar `boleto-service/pom.xml`
- [ ] Criar `BoletoServiceApplication.java`
- [ ] Criar `application.yml` (port 8080, MongoDB, Santander config)
- [ ] Verificar estrutura
- [ ] Commit: "setup: create boleto-service module base"

---

## Task 4: Implementar Models (Records) - Mock

- [ ] Criar `Boleto.java` (@Document collection="boletos_mock")
- [ ] Criar `Pagador.java`
- [ ] Criar `BoletoRequest.java`
- [ ] Criar `BoletoResponse.java`
- [ ] Criar `OAuthTokenResponse.java`
- [ ] Commit: "feat(mock): add boleto data models as records"

---

## Task 5: Implementar Models (Records) - Service

- [ ] Criar mesmos 5 Records em `boleto-service/src/main/java/com/projeto/boleto/model/`
- [ ] Mudar @Document para collection="boletos"
- [ ] Commit: "feat(service): add boleto data models as records"

---

## Task 6: Implementar Repositórios

- [ ] Criar `santander-mock/src/main/java/com/santander/mock/repository/BoletoMockRepository.java`
- [ ] Criar `boleto-service/src/main/java/com/projeto/boleto/repository/BoletoRepository.java`
- [ ] Commit: "feat: add MongoRepository interfaces"

---

## Task 7: Implementar BoletoMockService

- [ ] Criar service com métodos: createBoleto, listBoletos, getBoletoById, updateBoleto, deleteBoleto
- [ ] Usar repository para persistência
- [ ] Gerar IDs com UUID
- [ ] Commit: "feat(mock): add BoletoMockService CRUD logic"

---

## Task 8: Implementar OAuthController (Mock)

- [ ] Criar endpoint POST `/oauth/token`
- [ ] Validar client_id e client_secret
- [ ] Gerar JWT com 1h expiry
- [ ] Criar `SecurityConfig` para permitir acesso público
- [ ] Adicionar JJWT dependency ao parent pom
- [ ] Commit: "feat(mock): add OAuth token endpoint"

---

## Task 9: Implementar BoletoController (Mock - 7 endpoints)

- [ ] POST `/workspaces/{workspace_id}/bank_slips` - criar
- [ ] PATCH `/workspaces/{workspace_id}/bank_slips` - atualizar
- [ ] GET `/workspaces/{workspace_id}/bank_slips` - listar paginado
- [ ] GET `/workspaces/{workspace_id}/bank_slips/{bank_slip_id}` - obter por ID
- [ ] GET `/bills` - listar (alternativo)
- [ ] GET `/bills/{bill_id}` - obter (alternativo)
- [ ] POST `/bills/{bill_id}/bank_slips` - gerar PDF (mock retorna URL)
- [ ] Todos exigem token válido
- [ ] Commit: "feat(mock): add BoletoController with 7 endpoints"

---

## Task 10: Testes Mock

- [ ] Criar `BoletoMockServiceTest.java` (testCreateBoleto, testListBoletos, testGetById)
- [ ] Criar `OAuthControllerTest.java` (testTokenValid, testInvalidCredentials)
- [ ] Executar: `mvn test`
- [ ] Commit: "test(mock): add unit tests"

---

## Task 11: Implementar SantanderClient Interface (Service)

- [ ] Criar interface com 7 métodos (agnóstica)
- [ ] createBoleto, updateBoleto, listBoletos, getBoletoById, listBills, getBillById, generatePdf
- [ ] Commit: "feat(service): add SantanderClient abstraction interface"

---

## Task 12: Implementar RestSantanderClient

- [ ] Implementar SantanderClient com RestTemplate
- [ ] Fazer HTTP calls para URL configurada em `application.yml`
- [ ] Reutilizar token do OAuthManager em todas as requisições
- [ ] Commit: "feat(service): add RestSantanderClient HTTP implementation"

---

## Task 13: Implementar OAuthManager (Service)

- [ ] Cache em memória com LocalDateTime tokenExpiry
- [ ] Método getToken(): retorna cached ou refresh
- [ ] isTokenValid(): verifica se não expira nos próximos 60s
- [ ] refreshToken(): POST `/oauth/token` com grant_type=client_credentials
- [ ] ReentrantReadWriteLock para thread-safety
- [ ] Adicionar RestTemplate @Bean em BoletoServiceApplication
- [ ] Commit: "feat(service): add OAuthManager with token caching"

---

## Task 14: Implementar BoletoService (Service)

- [ ] 7 métodos: createBoleto, updateBoleto, listBoletos, getBoletoById, listBills, getBillById, generatePdf
- [ ] Cada método: 1) chama SantanderClient, 2) persiste em MongoDB
- [ ] Commit: "feat(service): add BoletoService orchestration"

---

## Task 15: Implementar BoletoController (Service - 5 endpoints)

- [ ] POST `/api/boletos` - criar
- [ ] GET `/api/boletos?page=0&size=10` - listar
- [ ] GET `/api/boletos/{id}` - obter
- [ ] PATCH `/api/boletos/{id}` - atualizar
- [ ] POST `/api/boletos/{id}/pdf` - gerar PDF
- [ ] Commit: "feat(service): add BoletoController"

---

## Task 16: Implementar BillController (Service - 2 endpoints)

- [ ] GET `/api/bills?page=0&size=10` - listar
- [ ] GET `/api/bills/{bill_id}` - obter
- [ ] Commit: "feat(service): add BillController"

---

## Task 17: Testes Service

- [ ] Criar `BoletoServiceTest.java` (testCreateBoleto, testGetById)
- [ ] Criar `OAuthManagerTest.java` (testGetToken, testCacheHit)
- [ ] Executar: `mvn test`
- [ ] Commit: "test(service): add unit tests"

---

## Task 18: Docker & Integração

- [ ] Criar `docker-compose.yml` (MongoDB, santander-mock:8081, boleto-service:8080)
- [ ] Criar `santander-mock/Dockerfile` (multi-stage build)
- [ ] Criar `boleto-service/Dockerfile` (multi-stage build)
- [ ] Verificar builds: `docker-compose build`
- [ ] Commit: "setup: add Docker orchestration"

---

## Task 19: Teste E2E

- [ ] `docker-compose up -d`
- [ ] Aguardar 30s
- [ ] `curl POST /oauth/token` → validar token
- [ ] `curl POST /api/boletos` com token → validar 201 Created
- [ ] `curl GET /api/boletos` → validar page com boleto
- [ ] `docker-compose down -v`
- [ ] Commit: "test: e2e validation with docker-compose"

---

**Total: 19 tasks bite-sized, todas testáveis e commitáveis**

