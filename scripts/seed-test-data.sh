#!/bin/bash

# ============================================
# SCRIPT PARA POPULAR BANCO COM DADOS DE TESTE
# ============================================
# Este script insere dados de teste via API REST
# Execute após subir a aplicação

set -e

BASE_URL="http://localhost:8080"
AUTH="admin:admin123"

echo "🌱 Populando banco de dados com dados de teste..."
echo ""

# ============================================
# 1. CRIAR ACCOUNT (Tenant B2B - Psicólogo)
# ============================================
echo "📋 1. Criando Account (Tenant B2B)..."

ACCOUNT_B2B=$(curl -s -X POST "$BASE_URL/api/accounts" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Clínica Psicológica Bem-Estar",
    "accountType": "B2B",
    "customPrompt": "Você é um assistente terapêutico especializado em Terapia Cognitivo-Comportamental (TCC). Seja empático, acolhedor e profissional. Faça perguntas abertas para entender melhor o estado emocional do paciente. Registre sintomas, humor e eventos importantes.",
    "isActive": true
  }')

ACCOUNT_B2B_ID=$(echo $ACCOUNT_B2B | jq -r '.id')
echo "✅ Account B2B criada: $ACCOUNT_B2B_ID"
echo ""

# ============================================
# 2. CRIAR ACCOUNT (Tenant B2C - Fibromialgia)
# ============================================
echo "📋 2. Criando Account (Tenant B2C)..."

ACCOUNT_B2C=$(curl -s -X POST "$BASE_URL/api/accounts" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fibromialgia Care",
    "accountType": "B2C",
    "customPrompt": "Você é um assistente de saúde especializado em fibromialgia. Monitore níveis de dor, qualidade do sono, fadiga e adesão à medicação. Seja compassivo e incentive hábitos saudáveis.",
    "isActive": true
  }')

ACCOUNT_B2C_ID=$(echo $ACCOUNT_B2C | jq -r '.id')
echo "✅ Account B2C criada: $ACCOUNT_B2C_ID"
echo ""

# ============================================
# 3. CRIAR PACIENTES (B2B)
# ============================================
echo "👥 3. Criando pacientes (B2B)..."

PATIENT_1=$(curl -s -X POST "$BASE_URL/api/patients?tenantId=$ACCOUNT_B2B_ID" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Silva",
    "whatsappNumber": "5511999990001",
    "isActive": true
  }')

PATIENT_1_ID=$(echo $PATIENT_1 | jq -r '.id')
echo "✅ Paciente 1 criado: Maria Silva ($PATIENT_1_ID)"

PATIENT_2=$(curl -s -X POST "$BASE_URL/api/patients?tenantId=$ACCOUNT_B2B_ID" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Santos",
    "whatsappNumber": "5511999990002",
    "isActive": true
  }')

PATIENT_2_ID=$(echo $PATIENT_2 | jq -r '.id')
echo "✅ Paciente 2 criado: João Santos ($PATIENT_2_ID)"
echo ""

# ============================================
# 4. CRIAR PACIENTE (B2C)
# ============================================
echo "👥 4. Criando paciente (B2C)..."

PATIENT_3=$(curl -s -X POST "$BASE_URL/api/patients?tenantId=$ACCOUNT_B2C_ID" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ana Costa",
    "whatsappNumber": "5511999990003",
    "isActive": true
  }')

PATIENT_3_ID=$(echo $PATIENT_3 | jq -r '.id')
echo "✅ Paciente 3 criado: Ana Costa ($PATIENT_3_ID)"
echo ""

# ============================================
# 5. CRIAR AGENDAMENTOS DE CHECK-IN PROATIVO
# ============================================
echo "🤖 5. Criando agendamentos de check-in proativo..."

# Check-in diário para Maria (09:00)
curl -s -X POST "$BASE_URL/api/checkin-schedules" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $ACCOUNT_B2B_ID" \
  -d "{
    \"patientId\": \"$PATIENT_1_ID\",
    \"scheduleType\": \"DAILY\",
    \"timeOfDay\": \"09:00:00\",
    \"timezone\": \"America/Sao_Paulo\",
    \"useAiGeneration\": true,
    \"maxMessagesPerDay\": 3,
    \"isActive\": true
  }" > /dev/null

echo "✅ Check-in diário criado para Maria Silva (09:00)"

# Check-in semanal para João (14:00 - Seg, Qua, Sex)
curl -s -X POST "$BASE_URL/api/checkin-schedules" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $ACCOUNT_B2B_ID" \
  -d "{
    \"patientId\": \"$PATIENT_2_ID\",
    \"scheduleType\": \"WEEKLY\",
    \"timeOfDay\": \"14:00:00\",
    \"daysOfWeek\": [1, 3, 5],
    \"timezone\": \"America/Sao_Paulo\",
    \"useAiGeneration\": true,
    \"maxMessagesPerDay\": 2,
    \"isActive\": true
  }" > /dev/null

echo "✅ Check-in semanal criado para João Santos (14:00 - Seg/Qua/Sex)"

# Check-in diário para Ana (08:00)
curl -s -X POST "$BASE_URL/api/checkin-schedules" \
  -u "$AUTH" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $ACCOUNT_B2C_ID" \
  -d "{
    \"patientId\": \"$PATIENT_3_ID\",
    \"scheduleType\": \"DAILY\",
    \"timeOfDay\": \"08:00:00\",
    \"timezone\": \"America/Sao_Paulo\",
    \"useAiGeneration\": false,
    \"customMessage\": \"🌅 Bom dia! Como você está se sentindo hoje? Como foi sua noite de sono?\",
    \"maxMessagesPerDay\": 3,
    \"isActive\": true
  }" > /dev/null

echo "✅ Check-in diário criado para Ana Costa (08:00 - mensagem fixa)"
echo ""

# ============================================
# RESUMO
# ============================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ DADOS DE TESTE CRIADOS COM SUCESSO!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Resumo:"
echo "  • 2 Accounts (1 B2B + 1 B2C)"
echo "  • 3 Pacientes (2 B2B + 1 B2C)"
echo "  • 3 Agendamentos de check-in proativo"
echo ""
echo "🔗 Acesse o Swagger para testar:"
echo "  http://localhost:8080/swagger-ui.html"
echo ""
echo "🔑 Credenciais:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""

