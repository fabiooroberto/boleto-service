# Análise de Refatoração — boleto-service

**Data**: 2026-07-31  
**Escopo**: Identificação de pontos candidatos a refatoração, melhoria organizacional, gaps de teste e código morto.

---

## Resumo Executivo

O módulo `boleto-service` (~1250 linhas em 29 arquivos Java) é compacto e funcional, mas apresenta oportunidades claras de refatoração em três eixos:

1. **Duplicação de código** (reconstrução de objetos, montagem de URLs, parsing de IDs)
2. **Confusão de camadas** (passthrough de serviço, injeção de config em múltiplas camadas, validação inconsistente)
3. **Cobertura de testes** (apenas 3 arquivos testados; controladores, serviço, mapper e repositório sem testes)

### ✅ Correções Implementadas

- **PATCH endpoint** agora funciona end-to-end com santander-mock
  - `BoletoController.patchBoleto` recebe `{bank_slip_id}` e chama `BoletoService.updateBoleto`
  - `RestSantanderClient.updateBoleto` usa PATCH (alinhado com santander-mock) em vez de PUT
  - Status HTTP mudado de 201 CREATED para 200 OK (PATCH é idempotente)
  - Teste disponível em `test-patch-integration.ps1`

---

## 1. Duplicação de Código

### 1.1 Reconstrução de BoletoRequest (30 campos)

**Arquivo**: [BoletoController.java:35-67, 102-134](boleto-service/src/main/java/com/projeto/boleto/controller/BoletoController.java#L35-L67)

**Problema**:  
A mesma construção de `BoletoRequest` com injeção do `environment` é repetida verbatim em `createBoleto` e `patchBoleto`:

```java
BoletoRequest withEnvironment = new BoletoRequest(
    request.covenantCode(),
    request.bankNumber(),
    // ... 28 mais campos
    request.messages()
);
```

**Impacto**: 33 linhas duplicadas; risco de inconsistência se um campo for adicionado ou removido.

**Recomendação**:
- Extrair um método estático no record `BoletoRequest`: `static BoletoRequest withEnvironment(BoletoRequest req, String env)`
- Ou um mapper dedicado (seguindo precedente de `BoletoMapper.java`)
- Nível de esforço: **baixo** (~10 min)

---

### 1.2 Parsing de ID Composto (Composite Key)

**Arquivo**: [BoletoService.java:41-51](boleto-service/src/main/java/com/projeto/boleto/service/BoletoService.java#L41-L51) vs [BillsController.java:42-45](boleto-service/src/main/java/com/projeto/boleto/controller/BillsController.java#L42-L45)

**Problema**:  
Dois componentes independentes lidam com IDs compostos (formato `environment.nsuCode.nsuDate.covenantCode.bankNumber`) com lógica diferente:

- **BoletoService.updateBoleto**: detecta o dot (`if (boletoId.contains("."))`) e tenta resolver via Mongo; **fallback silencioso** se não encontrar
- **BillsController.getBillById**: faz split e valida `parts.length == 2`; lança **erro 400** se inválido

**Impacto**: Comportamento imprevisível; código frágil dependente de magic strings; sem teste unitário para o parsing.

**Recomendação**:
- Criar uma classe de valor `CompositeId` ou `DotSeparatedId` com parsing validado
- Usar em ambos os locais para garantir consistência
- Nível de esforço: **médio** (~30 min, incluindo testes)

---

### 1.3 Montagem de URLs com Hardcoding

**Arquivo**: [RestSantanderClient.java:58, 74, 88, 125, 140, 154, 169, 188](boleto-service/src/main/java/com/projeto/boleto/client/RestSantanderClient.java)

**Problema**:  
O path base `"collection_bill_management/v2"` é repetido 8+ vezes via concatenação manual de strings:

```java
// Exemplo 1 (linha 58)
String url = baseUrl + "/collection_bill_management/v2/workspaces/" + workspaceId + "/bank_slips";

// Exemplo 2 (linha 140)
String url = baseUrl + "/collection_bill_management/v2/bills?page=" + page + "&size=" + size;

// Query strings (linhas 87-111)
StringBuilder url = new StringBuilder(baseUrl + "/collection_bill_management/v2/...");
url.append("?_limit=").append(query.limit());
url.append("&_offset=").append(query.offset());
// ... sem URL encoding, sem SafeURI
```

**Impacto**: 
- Código frágil; uma mudança de versão requer edits em múltiplos locais
- Query strings montadas manualmente sem `UriComponentsBuilder` — sem URL encoding automático (risco de caracteres especiais corromperem a URL)
- Difícil de testar; sem constants reutilizáveis

**Recomendação**:
- Criar uma classe de suporte `SantanderApiUrls` ou similar com constantes e builders:
  ```java
  public class SantanderApiUrls {
      static final String BASE_PATH = "/collection_bill_management/v2";
      static final String buildBankSlipsUrl(String baseUrl, String workspaceId) { ... }
      static final String buildBillsUrl(String baseUrl) { ... }
      // Query builder para reutilização
  }
  ```
- Trocar concatenação manual por `UriComponentsBuilder` para query strings:
  ```java
  UriComponentsBuilder.fromUriString(url)
      .queryParam("_limit", query.limit())
      .queryParam("_offset", query.offset())
      .build().encode().toUriString()
  ```
- Nível de esforço: **médio** (~45 min, inclui testes de URL)

---

## 2. Confusão de Camadas

### 2.1 BoletoService: 6 de 8 métodos são Passthrough puro

**Arquivo**: [BoletoService.java:59-81](boleto-service/src/main/java/com/projeto/boleto/service/BoletoService.java#L59-L81)

**Problema**:  
Métodos que apenas delegam a `SantanderClient` sem agregar lógica:

```java
public BankSlipListResponse listBoletos(BankSlipListQuery query) {
    return santanderClient.listBoletos(query);  // 1:1 passthrough
}

public BoletoResponse getBoletoById(String boletoId) {
    return santanderClient.getBoletoById(boletoId);  // 1:1 passthrough
}

public Page<BoletoResponse> listBills(int page, int size) {
    return santanderClient.listBills(page, size);  // 1:1 passthrough
}

// ... e mais 3 iguais
```

Apenas `createBoleto` e `updateBoleto` agregam valor (mapeamento + persistência em Mongo).

**Impacto**: 
- Camada de serviço oferece falsa abstração
- Controladores poderiam falar direto com o client (reduzir indireção)
- Ou completar o padrão de service agregando mais lógica (caching, validação, etc.)

**Recomendação**:
- **Opção A (curto prazo)**: Remover os métodos passthrough; fazer controllers chamarem `SantanderClient` diretamente para leitura, deixar apenas `createBoleto`/`updateBoleto`/`findBoletoByIdentifiers` no service (os que tocam Mongo).
- **Opção B (longo prazo)**: Completar o service com lógica real — caching de leitura, auditoria de escrita, validação de domínio — depois restaurar os passthrough como wrappers com valor.
- Nível de esforço: **baixo (opção A)** ou **alto (opção B)**

---

### 2.2 workspaceId: injeção duplicada

**Arquivo**: [BoletoController.java:20](boleto-service/src/main/java/com/projeto/boleto/controller/BoletoController.java#L20) + [RestSantanderClient.java:44](boleto-service/src/main/java/com/projeto/boleto/client/RestSantanderClient.java#L44)

**Problema**:  
`${santander.api.workspace-id}` é injetado via `@Value` em dois locais:

- Controller: `@Value("${santander.api.workspace-id}") private String workspaceId`
- Client: `@Value("${santander.api.workspace-id}") String workspaceId` (construtor)

**Impacto**: 
- Configuração vazada em múltiplas camadas
- Difícil mudança: se a config virar dinâmica, ambos os locais precisam de atualização
- Controlador não deveria conhecer detalhes de Santander

**Recomendação**:
- Remover a injeção do controller; deixar apenas no client (camada apropriada)
- Se o controller precisar do workspace-id para montar respostas, passá-lo via service como parâmetro derivado ou header
- Nível de esforço: **baixo** (~10 min)

---

### 2.3 Validação Inconsistente

**Arquivo**: [BoletoController.java:34](boleto-service/src/main/java/com/projeto/boleto/controller/BoletoController.java#L34) vs [BillsController.java:28-31, 40](boleto-service/src/main/java/com/projeto/boleto/controller/BillsController.java#L28-L31)

**Problema**:  
Dois estilos de validação convivem sem consistência:

- **BoletoController**: Bean Validation declarativa (`@Valid @RequestBody BoletoRequest`)
  - Usa anotações `@NotBlank`, `@NotNull` no record
  - Tratamento centralizado via `GlobalExceptionHandler`
  
- **BillsController**: validação imperativa manual (ad-hoc regex + `ResponseStatusException`)
  ```java
  if (bankNumber == null || !bankNumber.matches("^[0-9]{1,13}$"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bankNumber");
  ```
  - Sem reutilização; sem test coverage explícito para os regex

**Impacto**: 
- Manutenção difícil; regras de validação espalhadas
- Testes precisam abarcar dois idiomas diferentes
- Novos endpoints tendem a copiar um padrão ou o outro arbitrariamente

**Recomendação**:
- Padronizar em **Bean Validation** (mais declarativa e testável)
- Mover regexes (`^[0-9]{1,13}$`, etc.) para custom validators ou anotações:
  ```java
  @BankNumber  // custom constraint
  String bankNumber;
  ```
- Atualizar `BillsController` para usar `@Valid` como `BoletoController`
- Nível de esforço: **médio** (~30 min, inclui custom validators)

---

### 2.4 Exceção Genérica + Mapeamento de Status Código

**Arquivo**: [GlobalExceptionHandler.java:34-39](boleto-service/src/main/java/com/projeto/boleto/config/GlobalExceptionHandler.java#L34-L39) + [RestSantanderClient.java:177](boleto-service/src/main/java/com/projeto/boleto/client/RestSantanderClient.java#L177)

**Problema**:  
Qualquer `RuntimeException` é mapeada para HTTP 500 genérico:

```java
// GlobalExceptionHandler
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(ex.getMessage()));
}

// RestSantanderClient
if (result != null && result.data() != null && !result.data().isEmpty()) {
    return result.data().get(0);
}
throw new RuntimeException("Bill not found...");  // ← mapeado para 500, deveria ser 404
```

**Impacto**: 
- Client recebe 500 para um cenário de "not found" (deve ser 404)
- Sem domínio-specific exceptions, é impossível distinguir erros reais de erros lógicos
- Fuga de stacktraces/mensagens técnicas para o cliente

**Recomendação**:
- Criar exceções de domínio:
  ```java
  public class BoletoNotFoundException extends RuntimeException { ... }
  public class SantanderApiException extends RuntimeException { ... }
  ```
- Estender `GlobalExceptionHandler` para mapeá-las corretamente:
  ```java
  @ExceptionHandler(BoletoNotFoundException.class)
  public ResponseEntity<?> handleNotFound(...) { return 404; }
  ```
- Usar em `RestSantanderClient.getBillByNumber:177`
- Nível de esforço: **médio** (~45 min)

---

## 3. Configuração Morta

### 3.1 Token TTL e Refresh Margin não utilizados

**Arquivo**: [application.yml:29-31](boleto-service/src/main/resources/application.yml#L29-L31) vs [OAuthManagerImpl.java:96](boleto-service/src/main/java/com/projeto/boleto/client/OAuthManagerImpl.java#L96)

**Problema**:  
Propriedades definidas em `application.yml` mas nunca lidas:

```yaml
# application.yml
santander.api.auth:
  token-ttl-seconds: 3600        # Nunca usado
  refresh-margin-seconds: 60     # Nunca usado
```

```java
// OAuthManagerImpl.java:96
// Hardcoded 60, independente da config
LocalDateTime.now().plusSeconds(60)
```

**Impacto**: 
- Config morta cria confusão (alguém muda o yaml esperando comportamento diferente, sem resultado)
- Inconsistência entre config e código
- Token TTL é ignorado completamente (só usa `expiresIn` da API)

**Recomendação**:
- **Opção A**: Remover as linhas da `application.yml` (se realmente não são usadas)
- **Opção B**: Injeta-las em `OAuthManagerImpl` e usar de verdade:
  ```java
  @Value("${santander.api.auth.refresh-margin-seconds:60}")
  private long refreshMarginSeconds;
  ```
- Confirmar com o time se `token-ttl-seconds` deve ser lido ou se é obsoleto
- Nível de esforço: **muito baixo** (5 min se remover, 15 min se usar)

---

## 4. Código Potencialmente Morto

> ⚠️ **Recomendação**: Confirmar com o time antes de remover. Usar um analisador de call-graph (IDE "Find Usages") para garantir nenhum caller externo.

### 4.1 listBills / getBillById (operações de leitura)

**Arquivo**: 
- Interface: [SantanderClient.java](boleto-service/src/main/java/com/projeto/boleto/client/SantanderClient.java)
- Implementação: [RestSantanderClient.java:139-158](boleto-service/src/main/java/com/projeto/boleto/client/RestSantanderClient.java#L139-L158)
- Service passthrough: [BoletoService.java:67-73](boleto-service/src/main/java/com/projeto/boleto/service/BoletoService.java#L67-L73)

**Problema**:  
Nenhum controller chama estes métodos. `BillsController` prefere usar `getBillByNumber` (que busca pelo query param, não por ID):

- `SantanderClient.listBills(int, int)` — declarado, implementado, **nunca chamado**
- `SantanderClient.getBillById(String)` — declarado, implementado, **nunca chamado**

**Recomendação**:
- Verificar histórico de git para entender se eram parte de um plano abandonado
- Se não forem usados externamente (fora do módulo), considerar remover
- Ou documentar por que estão presentes (reserva para futuro)
- Nível de esforço: **muito baixo** (confirmação) ou **baixo** (remoção)

---

### 4.2 generateBankSlipPdf: Mock Hardcoded

**Arquivo**: [BillsController.java:49-58](boleto-service/src/main/java/com/projeto/boleto/controller/BillsController.java#L49-L58)

**Problema**:  
O endpoint fabricas uma URL hardcoded em vez de chamar o serviço:

```java
@PostMapping("/{bill_id}/bank_slips")
public ResponseEntity<Map<String, String>> generateBankSlipPdf(...) {
    // Ignora boletoService.generatePdf totalmente!
    response.put("link", "https://mock-boleto-pdfs.santander.local/boletos/" + billId + "/boleto.pdf");
    return ResponseEntity.ok(response);
}

// Mas BoletoService.generatePdf (linhas 79-81) existe e chama RestSantanderClient.generatePdf
public String generatePdf(String billId) {
    return santanderClient.generatePdf(billId);  // Nunca invocado pelo controller
}
```

**Impacto**: 
- Endpoint retorna URL mock hardcoded, nunca chama a API Santander de verdade
- `BoletoService.generatePdf` + `RestSantanderClient.generatePdf` são código morto
- Comportamento confuso: parece funcionar localmente, falha em produção se URL mock não existir

**Recomendação**:
- Decidir com o time: é intencional (stub para testes)? Ou deve chamar `boletoService.generatePdf`?
- Se intencional, documentar e adicionar um comentário claro
- Se não, corrigir para usar o service de verdade
- Nível de esforço: **muito baixo** (se remover) ou **baixo** (se wir para serviço)

---

## 5. Gaps de Cobertura de Testes

**Arquivo**: Estrutura atual de testes (apenas 3 arquivos, 501 linhas totais)

### O que está testado:
- ✅ `client/OAuthManagerImpl.java` (402 linhas de testes)
- ✅ `client/OAuthClientHttpInterceptor.java` (70 linhas de testes)
- ✅ `BoletoServiceApplication.java` (smoke test, 29 linhas)

### O que **não** está testado:
- ❌ [BoletoController.java](boleto-service/src/main/java/com/projeto/boleto/controller/BoletoController.java) (147 linhas) — sem `@WebMvcTest`, sem MockMvc, sem testes de validação
- ❌ [BillsController.java](boleto-service/src/main/java/com/projeto/boleto/controller/BillsController.java) (61 linhas) — regex validators, ID parsing, nenhum teste
- ❌ [BoletoService.java](boleto-service/src/main/java/com/projeto/boleto/service/BoletoService.java) (82 linhas) — orquestração, composite ID lookup, nenhum teste
- ❌ [BoletoMapper.java](boleto-service/src/main/java/com/projeto/boleto/mapper/BoletoMapper.java) (57 linhas) — mapeamento, nenhum teste
- ❌ [RestSantanderClient.java](boleto-service/src/main/java/com/projeto/boleto/client/RestSantanderClient.java) (193 linhas) — **maior classe sem testes**, contém lógica de URL propensa a bugs
- ❌ [GlobalExceptionHandler.java](boleto-service/src/main/java/com/projeto/boleto/config/GlobalExceptionHandler.java) (41 linhas) — sem testes de mapeamento
- ❌ [BoletoRepository.java](boleto-service/src/main/java/com/projeto/boleto/repository/BoletoRepository.java) — sem `@DataMongoTest`

### Recomendação de Priorização:

| Prioridade | Arquivo | Esforço | Razão |
|-----------|---------|--------|-------|
| **P0** | RestSantanderClient | Médio (1-2h) | Maior classe sem testes; lógica de URL crítica; 8 métodos |
| **P1** | BoletoController | Médio (1h) | Validação, criação, PATCH (agora corrigido) — 3 endpoints |
| **P1** | BoletoService | Baixo (30 min) | Orquestração simples, composite ID lookup |
| **P2** | BoletoMapper | Muito Baixo (15 min) | Unitário simples; alta ROI |
| **P2** | BillsController | Médio (45 min) | Validação regex, ID parsing — 3 endpoints |
| **P3** | BoletoRepository | Muito Baixo (15 min) | Derivado; SQL gerada pelo Spring |

---

## 6. Resumo de Recomendações por Esforço

### 🟢 Muito Baixo (<15 min)
- [ ] Remover configuração morta (token-ttl-seconds, refresh-margin-seconds)
- [ ] Adicionar teste unitário para `BoletoMapper`
- [ ] Confirmar e documentar/remover métodos de leitura mortos (`listBills`, `getBillById`)

### 🟡 Baixo (15-45 min)
- [ ] Extrair método `BoletoRequest.withEnvironment(...)` ou mapper
- [ ] Remover injeção de `workspaceId` do controller
- [ ] Decidir sobre `generateBankSlipPdf` (mock vs real) e corrigir/documentar

### 🟠 Médio (45 min - 2h)
- [ ] Criar classe `CompositeId` para parsing unificado
- [ ] Extrair `SantanderApiUrls` com constantes + `UriComponentsBuilder`
- [ ] Padronizar validação em Bean Validation (criar custom validators)
- [ ] Adicionar testes para `BoletoController` (MockMvc)
- [ ] Adicionar testes para `RestSantanderClient` (mock, sem HTTP real)

### 🔴 Alto (2h+)
- [ ] Remover passthrough de service (6 métodos) ou completar com lógica real
- [ ] Criar exceções de domínio + estender `GlobalExceptionHandler`
- [ ] Suite completa de testes para `BillsController` (validação regex, parsing)

---

## 7. Próximas Etapas Sugeridas

1. **Revisão com o time** (30 min):
   - Confirmar se `listBills`/`getBillById` são código morto
   - Confirmar se `generateBankSlipPdf` deve usar o serviço ou permanecer mock
   - Priorizar os gaps de teste (qual módulo é crítico?)

2. **Executar em iterações**:
   - **Iteração 1**: Remover config morta + testes unitários simples (muito baixo)
   - **Iteração 2**: Extrair duplicação `BoletoRequest` + `CompositeId` (baixo + médio)
   - **Iteração 3**: Testes de controller (P0 = `RestSantanderClient`, P1 = `BoletoController`)
   - **Iteração 4+**: Padronização de validação, exceções de domínio (refactor maior)

3. **Documentação**:
   - Atualizar ADR (Architecture Decision Record) se houver, ou criar `REFACTORING.md` com decisões tomadas

---

## Apêndice: Checklist de Verificação

- [ ] Compilação: `mvn -pl boleto-service clean compile`
- [ ] Testes: `mvn -pl boleto-service test`
- [ ] Cobertura: `mvn -pl boleto-service jacoco:report` (se configurado)
- [ ] Call-graph: IDE "Find Usages" em `listBills`, `getBillById`, `generatePdf`
- [ ] Review com o time: confirmar escopo e prioridades acima
