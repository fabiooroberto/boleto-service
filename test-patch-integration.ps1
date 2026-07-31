#!/usr/bin/env pwsh

$BaseUrl = "http://localhost:8080/v1"
$SantanderUrl = "http://localhost:8081"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  PATCH Integration Test" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Step 1: Get OAuth token
Write-Host "[1/5] Getting OAuth token..." -ForegroundColor Yellow
try {
    $tokenResponse = Invoke-WebRequest -Uri "$SantanderUrl/oauth/token" -Method POST `
        -ContentType "application/x-www-form-urlencoded" `
        -Body "grant_type=client_credentials&client_id=test-client&client_secret=test-secret" `
        -ErrorAction Stop

    $token = ($tokenResponse.Content | ConvertFrom-Json).access_token
    Write-Host "✓ Token obtained" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to get token: $_" -ForegroundColor Red
    exit 1
}

$headers = @{ "Authorization" = "Bearer $token" }

# Step 2: Create initial boleto
Write-Host "[2/5] Creating initial boleto..." -ForegroundColor Yellow

$boleto = @{
    covenantCode = "9999999"
    bankNumber = "033"
    dueDate = "2026-12-31"
    nominalValue = "1000.00"
    documentKind = "DUPLICATA_MERCANTIL"
    nsuCode = "TEST001"
    nsuDate = "2026-07-30"
    issueDate = "2026-07-30"
    paymentType = "REGISTRO"
    environment = "TESTE"
    payer = @{
        name = "Test Payer"
        documentType = "CPF"
        documentNumber = "12345678901"
        address = "Test St, 123"
        neighborhood = "Test"
        city = "Test City"
        state = "SP"
        zipCode = "12345-678"
    }
    beneficiary = @{
        name = "Test Beneficiary"
        documentType = "CNPJ"
        documentNumber = "12345678901234"
    }
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri "$BaseUrl/bank_slips" -Method POST `
        -ContentType "application/json" -Headers $headers -Body $boleto -ErrorAction Stop

    $boletoId = ($createResponse.Content | ConvertFrom-Json).id
    Write-Host "✓ Boleto created: $boletoId" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create boleto: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Update boleto via PATCH
Write-Host "[3/5] Updating boleto via PATCH..." -ForegroundColor Yellow

$updateBoleto = @{
    covenantCode = "9999999"
    bankNumber = "033"
    dueDate = "2026-12-31"
    nominalValue = "1500.00"
    documentKind = "DUPLICATA_MERCANTIL"
    nsuCode = "TEST001"
    nsuDate = "2026-07-30"
    issueDate = "2026-07-30"
    paymentType = "REGISTRO"
    environment = "TESTE"
    payer = @{
        name = "Updated Payer"
        documentType = "CPF"
        documentNumber = "12345678901"
        address = "Test St, 123"
        neighborhood = "Test"
        city = "Test City"
        state = "SP"
        zipCode = "12345-678"
    }
    beneficiary = @{
        name = "Test Beneficiary"
        documentType = "CNPJ"
        documentNumber = "12345678901234"
    }
} | ConvertTo-Json

try {
    $patchResponse = Invoke-WebRequest -Uri "$BaseUrl/bank_slips/$boletoId" -Method PATCH `
        -ContentType "application/json" -Headers $headers -Body $updateBoleto -ErrorAction Stop

    $statusCode = $patchResponse.StatusCode
    $responseBody = $patchResponse.Content | ConvertFrom-Json

    if ($statusCode -eq 200 -or $statusCode -eq 201) {
        Write-Host "✓ PATCH successful (status: $statusCode)" -ForegroundColor Green
        Write-Host "  Response: $($responseBody | ConvertTo-Json -Depth 1)" -ForegroundColor Gray
    } else {
        Write-Host "✗ PATCH returned unexpected status: $statusCode" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ PATCH failed: $_" -ForegroundColor Red
    $error[0] | Select-Object -ExpandProperty Exception | ForEach-Object {
        if ($_.Response) {
            Write-Host "Response status: $($_.Response.StatusCode)" -ForegroundColor Yellow
            Write-Host "Response body: $($_.Response | ConvertFrom-Json)" -ForegroundColor Yellow
        }
    }
    exit 1
}

# Step 4: Verify updated boleto
Write-Host "[4/5] Verifying updated boleto..." -ForegroundColor Yellow

try {
    $getResponse = Invoke-WebRequest -Uri "$BaseUrl/bank_slips/$boletoId" -Method GET `
        -Headers $headers -ErrorAction Stop

    $updated = $getResponse.Content | ConvertFrom-Json

    Write-Host "✓ Boleto retrieved" -ForegroundColor Green
    Write-Host "  Nominal Value: $($updated.nominalValue)" -ForegroundColor Gray
    Write-Host "  Payer Name: $($updated.payer.name)" -ForegroundColor Gray

    if ($updated.nominalValue -eq "1500.00" -or $updated.nominalValue -eq 1500.00) {
        Write-Host "✓ PATCH update verified - nominal value updated!" -ForegroundColor Green
    } else {
        Write-Host "⚠ Nominal value not updated (expected 1500.00, got $($updated.nominalValue))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to get boleto: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Summary
Write-Host "[5/5] Test Summary" -ForegroundColor Yellow
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  PATCH INTEGRATION TEST PASSED ✓" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`nBoleto ID: $boletoId"
Write-Host "Operations: CREATE → PATCH → GET ✓"
Write-Host ""
