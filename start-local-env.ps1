# ============================================
# SCRIPT DE INICIALIZAÇÃO RÁPIDA (PowerShell)
# ============================================
# Sobe todo o ambiente de desenvolvimento local
# Uso: .\start-local-env.ps1

$ErrorActionPreference = "Stop"

Write-Host "🚀 AI Health Agent - Inicialização do Ambiente Local" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""

# ============================================
# 1. VERIFICAR PRÉ-REQUISITOS
# ============================================
Write-Host "📋 1. Verificando pré-requisitos..." -ForegroundColor Cyan

# Docker
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker não encontrado. Instale: https://www.docker.com/products/docker-desktop/" -ForegroundColor Red
    exit 1
}

# Docker Compose
try {
    $composeVersion = docker-compose --version
    Write-Host "✅ Docker Compose: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker Compose não encontrado." -ForegroundColor Red
    exit 1
}

# Java
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java não encontrado. Instale Java 21+." -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================
# 2. VERIFICAR ARQUIVO .env
# ============================================
Write-Host "📋 2. Verificando configuração..." -ForegroundColor Cyan

if (-not (Test-Path .env)) {
    Write-Host "⚠️  Arquivo .env não encontrado. Criando a partir do template..." -ForegroundColor Yellow
    Copy-Item .env.example .env
    Write-Host "⚠️  ATENÇÃO: Edite o arquivo .env com suas credenciais reais!" -ForegroundColor Yellow
    Write-Host "   Especialmente: OPENAI_API_KEY" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Pressione ENTER para continuar ou Ctrl+C para cancelar"
}

Write-Host "✅ Arquivo .env encontrado" -ForegroundColor Green
Write-Host ""

# ============================================
# 3. SUBIR DOCKER COMPOSE
# ============================================
Write-Host "🐳 3. Subindo containers Docker..." -ForegroundColor Cyan
Write-Host "   • PostgreSQL (porta 5432)"
Write-Host "   • Evolution API (porta 8081)"
Write-Host ""

docker-compose -f docker-compose.test.yml up -d

Write-Host ""
Write-Host "✅ Containers iniciados com sucesso!" -ForegroundColor Green
Write-Host ""

# ============================================
# 4. AGUARDAR POSTGRESQL
# ============================================
Write-Host "⏳ 4. Aguardando PostgreSQL ficar pronto..." -ForegroundColor Cyan

$maxTries = 30
$tries = 0

while ($tries -lt $maxTries) {
    try {
        docker exec ai-health-postgres-test pg_isready -U postgres 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ PostgreSQL está pronto!" -ForegroundColor Green
            break
        }
    } catch {
        # Continuar tentando
    }
    
    $tries++
    Write-Host "   Tentativa $tries/$maxTries..."
    Start-Sleep -Seconds 2
}

if ($tries -eq $maxTries) {
    Write-Host "❌ PostgreSQL não ficou pronto a tempo." -ForegroundColor Red
    Write-Host "   Verifique os logs: docker-compose -f docker-compose.test.yml logs postgres" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# ============================================
# 5. COMPILAR APLICAÇÃO
# ============================================
Write-Host "🔨 5. Compilando aplicação..." -ForegroundColor Cyan

.\mvnw.cmd clean install -DskipTests

Write-Host "✅ Aplicação compilada com sucesso!" -ForegroundColor Green
Write-Host ""

# ============================================
# 6. RODAR MIGRATIONS
# ============================================
Write-Host "🗄️  6. Executando migrations do banco de dados..." -ForegroundColor Cyan

$env:SPRING_PROFILES_ACTIVE = "docker"
.\mvnw.cmd flyway:migrate

Write-Host "✅ Migrations executadas com sucesso!" -ForegroundColor Green
Write-Host ""

# ============================================
# 7. INICIAR APLICAÇÃO
# ============================================
Write-Host "🚀 7. Iniciando aplicação Spring Boot..." -ForegroundColor Cyan
Write-Host "   (Pressione Ctrl+C para parar)" -ForegroundColor Yellow
Write-Host ""

$env:SPRING_PROFILES_ACTIVE = "docker"
.\mvnw.cmd spring-boot:run

