# Santander Boleto Integration - Setup & Running Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Client (curl/Postman)                                  │
└────┬────────────────────────────────────────┬───────────┘
     │                                        │
     ▼                                        ▼
┌─────────────────────┐              ┌─────────────────────┐
│ Boleto Service      │              │ Santander Mock      │
│ (Port 8080)         │◄────────────►│ (Port 8081)         │
│ Maven Local         │              │ Maven Local         │
└────────┬────────────┘              └─────────┬───────────┘
         │                                     │
         └──────────────┬──────────────────────┘
                        │
                        ▼
              ┌──────────────────────┐
              │ MongoDB (Docker)     │
              │ Port 27017           │
              │ 2 DBs:               │
              │ - boleto_service     │
              │ - santander_mock     │
              └──────────────────────┘
```

## Prerequisites

- **Java 21+** (for Maven projects)
- **Maven 3.8+** (for building projects)
- **Docker Desktop** (for MongoDB)
- **PowerShell 7+** or **Bash** (for running scripts)

## Quick Start

### 1️⃣ Start MongoDB (Docker)

```powershell
docker run -d --name boleto-mongodb -p 27017:27017 mongo:latest
```

Verify it's running:
```powershell
docker ps | grep boleto-mongodb
```

### 2️⃣ Start Santander Mock (Local)

```powershell
cd d:\repos\java\teste3
mvn -pl santander-mock spring-boot:run
```

Expected output:
```
Tomcat started on port 8081
Started SantanderMockApplication in X.XXX seconds
```

### 3️⃣ Start Boleto Service (Local - New Terminal)

```powershell
cd d:\repos\java\teste3
mvn -pl boleto-service spring-boot:run
```

Expected output:
```
Tomcat started on port 8080
Started BoletoServiceApplication in X.XXX seconds
```

### 4️⃣ Verify Services are Running

**Get OAuth Token** (santander-mock):
```powershell
curl -X POST http://localhost:8081/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test-client&client_secret=test-secret"
```

Expected response:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

**List Boletos** (boleto-service):
```powershell
curl http://localhost:8080/v1/bank_slips?_limit=1&_offset=0&status=ATIVO
```

## API Testing

### Using Postman

1. Import the collection: `Santander-Boleto-API.postman_collection.json`
2. Run the sequence:
   - **OAuth - Get Access Token** (saves token to {{bearer_token}})
   - **Create Boleto** (creates a test boleto)
   - **List Boletos** (verifies it was saved)
   - **Update Boleto (PATCH)** (tests update via composite keys)
   - **Generate PDF** (tests PDF generation)

### Using curl

**1. Get Token:**
```powershell
$token = (curl -X POST http://localhost:8081/oauth/token `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials&client_id=test-client&client_secret=test-secret" | ConvertFrom-Json).access_token
```

**2. Create Boleto:**
```powershell
curl -X POST http://localhost:8080/v1/bank_slips `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d '{
    "covenantCode": "1234567",
    "bankNumber": "033",
    "dueDate": "2027-03-20",
    "nominalValue": "2000.00",
    "documentKind": "DUPLICATA_MERCANTIL",
    "nsuCode": "000001",
    "nsuDate": "2026-07-30",
    "issueDate": "2026-07-30",
    "paymentType": "REGISTRO",
    "payer": {
      "name": "Test Payer",
      "documentType": "CPF",
      "documentNumber": "12345678901",
      "address": "Test St, 123",
      "neighborhood": "Test",
      "city": "Test City",
      "state": "SP",
      "zipCode": "12345-678"
    },
    "beneficiary": {
      "name": "Test Beneficiary",
      "documentType": "CNPJ",
      "documentNumber": "12345678901234"
    }
  }'
```

**3. Update Boleto (PATCH):**
```powershell
curl -X PATCH http://localhost:8080/v1/bank_slips `
  -H "Content-Type: application/json" `
  -d '{
    "covenantCode": "1234567",
    "bankNumber": "033",
    "nominalValue": "2500.00",
    "dueDate": "2027-03-20",
    "documentKind": "DUPLICATA_MERCANTIL",
    "nsuCode": "000001",
    "nsuDate": "2026-07-30",
    "issueDate": "2026-07-30",
    "paymentType": "REGISTRO",
    "environment": "TESTE",
    "payer": {
      "name": "Updated Payer",
      "documentType": "CPF",
      "documentNumber": "12345678901",
      "address": "Test St, 123",
      "neighborhood": "Test",
      "city": "Test City",
      "state": "SP",
      "zipCode": "12345-678"
    },
    "beneficiary": {
      "name": "Test Beneficiary",
      "documentType": "CNPJ",
      "documentNumber": "12345678901234"
    }
  }'
```

## Running Automated Tests

### Integration Test Script

```powershell
.\test-integration.ps1
```

This runs:
1. Waits for services to be ready
2. Gets OAuth token
3. Creates a boleto
4. Lists boletos
5. Verifies MongoDB persistence

### PATCH Integration Test

```powershell
.\test-patch-integration.ps1
```

This tests the PATCH endpoint specifically.

## Environment Configuration

### Boleto Service (`application.yml`)

```yaml
santander:
  api:
    base-url: http://localhost:8081
    workspace-id: workspace-mock
    environment: TESTE
    client-id: test-client
    client-secret: test-secret
```

### Santander Mock (`application.yml`)

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/santander_mock
```

### MongoDB Databases

- **boleto_service**: Stores boletos created via boleto-service (persists local copies)
- **santander_mock**: Stores boletos created via santander-mock API

## Stopping Services

```powershell
# Stop MongoDB
docker stop boleto-mongodb
docker rm boleto-mongodb

# Kill Maven processes (Java)
Get-Process java | Stop-Process -Force
```

Or for individual terminals: `Ctrl+C`

## Troubleshooting

### "Connection refused" errors

**Problem:** Services are trying to connect but MongoDB/other service is down

**Solution:**
1. Verify MongoDB is running: `docker ps | grep boleto-mongodb`
2. Verify ports are available: `netstat -ano | findstr :8080`, `netstat -ano | findstr :8081`
3. Kill any processes using those ports if needed

### "Boleto not found" errors in PATCH

**Problem:** PATCH can't find the boleto to update

**Solution:**
1. Ensure you're using the exact composite keys (environment, nsuCode, nsuDate, covenantCode, bankNumber) that were used to create it
2. Note: boleto-service and santander-mock use separate MongoDB databases, so a boleto created in one won't be found in the other for updates

### OAuth token expired

**Problem:** "Invalid token" or "Token expired" errors

**Solution:**
1. Get a new token: run the OAuth - Get Access Token request again
2. Token TTL is 3600 seconds (1 hour)

## Project Structure

```
d:\repos\java\teste3\
├── boleto-service/           # Main boleto service (port 8080)
│   ├── src/
│   ├── pom.xml
│   └── REFACTORING_ANALYSIS.md  # Code quality notes
├── santander-mock/           # Mock Santander API (port 8081)
│   ├── src/
│   └── pom.xml
├── Santander-Boleto-API.postman_collection.json
├── test-integration.ps1      # Full integration test
├── test-patch-integration.ps1 # PATCH endpoint test
└── README.md                 # This file
```

## Key Endpoints

### Boleto Service (8080)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/v1/bank_slips` | Create boleto |
| GET | `/v1/bank_slips` | List boletos (paginated) |
| GET | `/v1/bank_slips/{id}` | Get boleto by UUID |
| PATCH | `/v1/bank_slips` | Update boleto by composite keys |
| GET | `/v1/bills?bankNumber=X&beneficiaryCode=Y` | List bills |
| GET | `/v1/bills/{id}` | Get bill by ID |
| POST | `/v1/bills/{id}/bank_slips` | Generate PDF |

### Santander Mock (8081)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/oauth/token` | Get access token |
| POST | `/collection_bill_management/v2/workspaces/{ws}/bank_slips` | Create boleto |
| GET | `/collection_bill_management/v2/workspaces/{ws}/bank_slips` | List boletos |
| PATCH | `/collection_bill_management/v2/workspaces/{ws}/bank_slips` | Update boleto |
| GET | `/collection_bill_management/v2/bills?bankNumber=X&beneficiaryCode=Y` | List bills |
| POST | `/collection_bill_management/v2/bills/{id}/bank_slips` | Generate PDF |

## Additional Resources

- **REFACTORING_ANALYSIS.md**: Code quality analysis and refactoring recommendations
- **archived_docs/**: Legacy documentation and scripts
- **Postman Collection**: Complete API testing with pre-built requests
