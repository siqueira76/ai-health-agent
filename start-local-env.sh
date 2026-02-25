#!/bin/bash

# ============================================
# SCRIPT DE INICIALIZAÇÃO RÁPIDA
# ============================================
# Sobe todo o ambiente de desenvolvimento local

set -e

echo "🚀 AI Health Agent - Inicialização do Ambiente Local"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# ============================================
# 1. VERIFICAR PRÉ-REQUISITOS
# ============================================
echo "📋 1. Verificando pré-requisitos..."

# Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não encontrado. Instale: https://www.docker.com/products/docker-desktop/"
    exit 1
fi
echo "✅ Docker: $(docker --version)"

# Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose não encontrado."
    exit 1
fi
echo "✅ Docker Compose: $(docker-compose --version)"

# Java
if ! command -v java &> /dev/null; then
    echo "❌ Java não encontrado. Instale Java 21+."
    exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -n 1)"

echo ""

# ============================================
# 2. VERIFICAR ARQUIVO .env
# ============================================
echo "📋 2. Verificando configuração..."

if [ ! -f .env ]; then
    echo "⚠️  Arquivo .env não encontrado. Criando a partir do template..."
    cp .env.example .env
    echo "⚠️  ATENÇÃO: Edite o arquivo .env com suas credenciais reais!"
    echo "   Especialmente: OPENAI_API_KEY"
    echo ""
    read -p "Pressione ENTER para continuar ou Ctrl+C para cancelar..."
fi

echo "✅ Arquivo .env encontrado"
echo ""

# ============================================
# 3. SUBIR DOCKER COMPOSE
# ============================================
echo "🐳 3. Subindo containers Docker..."
echo "   • PostgreSQL (porta 5432)"
echo "   • Evolution API (porta 8081)"
echo ""

docker-compose -f docker-compose.test.yml up -d

echo ""
echo "✅ Containers iniciados com sucesso!"
echo ""

# ============================================
# 4. AGUARDAR POSTGRESQL
# ============================================
echo "⏳ 4. Aguardando PostgreSQL ficar pronto..."

MAX_TRIES=30
TRIES=0

while [ $TRIES -lt $MAX_TRIES ]; do
    if docker exec ai-health-postgres-test pg_isready -U postgres > /dev/null 2>&1; then
        echo "✅ PostgreSQL está pronto!"
        break
    fi
    
    TRIES=$((TRIES + 1))
    echo "   Tentativa $TRIES/$MAX_TRIES..."
    sleep 2
done

if [ $TRIES -eq $MAX_TRIES ]; then
    echo "❌ PostgreSQL não ficou pronto a tempo."
    echo "   Verifique os logs: docker-compose -f docker-compose.test.yml logs postgres"
    exit 1
fi

echo ""

# ============================================
# 5. COMPILAR APLICAÇÃO
# ============================================
echo "🔨 5. Compilando aplicação..."

./mvnw clean install -DskipTests

echo "✅ Aplicação compilada com sucesso!"
echo ""

# ============================================
# 6. RODAR MIGRATIONS
# ============================================
echo "🗄️  6. Executando migrations do banco de dados..."

export SPRING_PROFILES_ACTIVE=docker
./mvnw flyway:migrate

echo "✅ Migrations executadas com sucesso!"
echo ""

# ============================================
# 7. INICIAR APLICAÇÃO
# ============================================
echo "🚀 7. Iniciando aplicação Spring Boot..."
echo "   (Pressione Ctrl+C para parar)"
echo ""

export SPRING_PROFILES_ACTIVE=docker
./mvnw spring-boot:run

