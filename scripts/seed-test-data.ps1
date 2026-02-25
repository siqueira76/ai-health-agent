# ============================================
# SCRIPT PARA POPULAR BANCO COM DADOS DE TESTE (PowerShell)
# ============================================
# Execute após subir a aplicação
# Uso: .\scripts\seed-test-data.ps1

$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:8080"
$AUTH = "admin:admin123"
$AuthBytes = [System.Text.Encoding]::ASCII.GetBytes($AUTH)
$AuthBase64 = [Convert]::ToBase64String($AuthBytes)
$Headers = @{
    "Authorization" = "Basic $AuthBase64"
    "Content-Type" = "application/json"
}

Write-Host "🌱 Populando banco de dados com dados de teste..." -ForegroundColor Green
Write-Host ""

# ============================================
# 1. CRIAR ACCOUNT (Tenant B2B - Psicólogo)
# ============================================
Write-Host "📋 1. Criando Account (Tenant B2B)..." -ForegroundColor Cyan

$AccountB2BBody = @{
    name = "Clínica Psicológica Bem-Estar"
    accountType = "B2B"
    customPrompt = "Você é um assistente terapêutico especializado em Terapia Cognitivo-Comportamental (TCC). Seja empático, acolhedor e profissional."
    isActive = $true
} | ConvertTo-Json

$AccountB2B = Invoke-RestMethod -Uri "$BASE_URL/api/accounts" -Method Post -Headers $Headers -Body $AccountB2BBody
$AccountB2BId = $AccountB2B.id

Write-Host "✅ Account B2B criada: $AccountB2BId" -ForegroundColor Green
Write-Host ""

# ============================================
# 2. CRIAR ACCOUNT (Tenant B2C - Fibromialgia)
# ============================================
Write-Host "📋 2. Criando Account (Tenant B2C)..." -ForegroundColor Cyan

$AccountB2CBody = @{
    name = "Fibromialgia Care"
    accountType = "B2C"
    customPrompt = "Você é um assistente de saúde especializado em fibromialgia. Monitore níveis de dor, qualidade do sono, fadiga e adesão à medicação."
    isActive = $true
} | ConvertTo-Json

$AccountB2C = Invoke-RestMethod -Uri "$BASE_URL/api/accounts" -Method Post -Headers $Headers -Body $AccountB2CBody
$AccountB2CId = $AccountB2C.id

Write-Host "✅ Account B2C criada: $AccountB2CId" -ForegroundColor Green
Write-Host ""

# ============================================
# 3. CRIAR PACIENTES (B2B)
# ============================================
Write-Host "👥 3. Criando pacientes (B2B)..." -ForegroundColor Cyan

$Patient1Body = @{
    name = "Maria Silva"
    whatsappNumber = "5511999990001"
    isActive = $true
} | ConvertTo-Json

$Patient1 = Invoke-RestMethod -Uri "$BASE_URL/api/patients?tenantId=$AccountB2BId" -Method Post -Headers $Headers -Body $Patient1Body
$Patient1Id = $Patient1.id

Write-Host "✅ Paciente 1 criado: Maria Silva ($Patient1Id)" -ForegroundColor Green

$Patient2Body = @{
    name = "João Santos"
    whatsappNumber = "5511999990002"
    isActive = $true
} | ConvertTo-Json

$Patient2 = Invoke-RestMethod -Uri "$BASE_URL/api/patients?tenantId=$AccountB2BId" -Method Post -Headers $Headers -Body $Patient2Body
$Patient2Id = $Patient2.id

Write-Host "✅ Paciente 2 criado: João Santos ($Patient2Id)" -ForegroundColor Green
Write-Host ""

# ============================================
# 4. CRIAR PACIENTE (B2C)
# ============================================
Write-Host "👥 4. Criando paciente (B2C)..." -ForegroundColor Cyan

$Patient3Body = @{
    name = "Ana Costa"
    whatsappNumber = "5511999990003"
    isActive = $true
} | ConvertTo-Json

$Patient3 = Invoke-RestMethod -Uri "$BASE_URL/api/patients?tenantId=$AccountB2CId" -Method Post -Headers $Headers -Body $Patient3Body
$Patient3Id = $Patient3.id

Write-Host "✅ Paciente 3 criado: Ana Costa ($Patient3Id)" -ForegroundColor Green
Write-Host ""

# ============================================
# 5. CRIAR AGENDAMENTOS DE CHECK-IN PROATIVO
# ============================================
Write-Host "🤖 5. Criando agendamentos de check-in proativo..." -ForegroundColor Cyan

$HeadersWithTenant = $Headers.Clone()
$HeadersWithTenant["X-Tenant-Id"] = $AccountB2BId

# Check-in diário para Maria (09:00)
$Schedule1Body = @{
    patientId = $Patient1Id
    scheduleType = "DAILY"
    timeOfDay = "09:00:00"
    timezone = "America/Sao_Paulo"
    useAiGeneration = $true
    maxMessagesPerDay = 3
    isActive = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/api/checkin-schedules" -Method Post -Headers $HeadersWithTenant -Body $Schedule1Body | Out-Null
Write-Host "✅ Check-in diário criado para Maria Silva (09:00)" -ForegroundColor Green

# Check-in semanal para João (14:00 - Seg, Qua, Sex)
$Schedule2Body = @{
    patientId = $Patient2Id
    scheduleType = "WEEKLY"
    timeOfDay = "14:00:00"
    daysOfWeek = @(1, 3, 5)
    timezone = "America/Sao_Paulo"
    useAiGeneration = $true
    maxMessagesPerDay = 2
    isActive = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/api/checkin-schedules" -Method Post -Headers $HeadersWithTenant -Body $Schedule2Body | Out-Null
Write-Host "✅ Check-in semanal criado para João Santos (14:00 - Seg/Qua/Sex)" -ForegroundColor Green

# Check-in diário para Ana (08:00)
$HeadersWithTenant["X-Tenant-Id"] = $AccountB2CId

$Schedule3Body = @{
    patientId = $Patient3Id
    scheduleType = "DAILY"
    timeOfDay = "08:00:00"
    timezone = "America/Sao_Paulo"
    useAiGeneration = $false
    customMessage = "🌅 Bom dia! Como você está se sentindo hoje? Como foi sua noite de sono?"
    maxMessagesPerDay = 3
    isActive = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/api/checkin-schedules" -Method Post -Headers $HeadersWithTenant -Body $Schedule3Body | Out-Null
Write-Host "✅ Check-in diário criado para Ana Costa (08:00 - mensagem fixa)" -ForegroundColor Green
Write-Host ""

# ============================================
# RESUMO
# ============================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "✅ DADOS DE TESTE CRIADOS COM SUCESSO!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""
Write-Host "📊 Resumo:" -ForegroundColor Cyan
Write-Host "  • 2 Accounts (1 B2B + 1 B2C)"
Write-Host "  • 3 Pacientes (2 B2B + 1 B2C)"
Write-Host "  • 3 Agendamentos de check-in proativo"
Write-Host ""
Write-Host "🔗 Acesse o Swagger para testar:" -ForegroundColor Cyan
Write-Host "  http://localhost:8080/swagger-ui.html"
Write-Host ""
Write-Host "🔑 Credenciais:" -ForegroundColor Cyan
Write-Host "  Username: admin"
Write-Host "  Password: admin123"
Write-Host ""

