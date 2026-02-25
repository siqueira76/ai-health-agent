#!/bin/bash

# ========================================
# Script de Teste - Dashboard API
# AI Health Agent - Fase 4
# ========================================

# Configurações
BASE_URL="http://localhost:8080"
TENANT_ID="your-tenant-id-here"
PATIENT_ID="your-patient-id-here"
ALERT_ID="your-alert-id-here"

echo "🧪 Testando Dashboard API - AI Health Agent"
echo "============================================"
echo ""

# ========================================
# 1. Listar Todos os Pacientes
# ========================================
echo "📊 1. Listando todos os pacientes..."
curl -X GET "${BASE_URL}/api/dashboard/patients?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 1 concluído"
echo ""

# ========================================
# 2. Estatísticas de Um Paciente
# ========================================
echo "📊 2. Buscando estatísticas de um paciente..."
curl -X GET "${BASE_URL}/api/dashboard/patients/${PATIENT_ID}?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 2 concluído"
echo ""

# ========================================
# 3. Resumo de Conversas
# ========================================
echo "💬 3. Buscando resumo de conversas (últimos 30 dias)..."
START_DATE=$(date -u -d '30 days ago' +"%Y-%m-%dT00:00:00")
END_DATE=$(date -u +"%Y-%m-%dT23:59:59")

curl -X GET "${BASE_URL}/api/dashboard/patients/${PATIENT_ID}/conversations?tenantId=${TENANT_ID}&startDate=${START_DATE}&endDate=${END_DATE}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 3 concluído"
echo ""

# ========================================
# 4. Listar Todos os Alertas Ativos
# ========================================
echo "🚨 4. Listando todos os alertas ativos..."
curl -X GET "${BASE_URL}/api/dashboard/alerts?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 4 concluído"
echo ""

# ========================================
# 5. Listar Alertas Críticos
# ========================================
echo "🚨 5. Listando alertas críticos..."
curl -X GET "${BASE_URL}/api/dashboard/alerts/critical?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 5 concluído"
echo ""

# ========================================
# 6. Alertas de Um Paciente
# ========================================
echo "🚨 6. Listando alertas de um paciente..."
curl -X GET "${BASE_URL}/api/dashboard/patients/${PATIENT_ID}/alerts?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  | jq '.'

echo ""
echo "✅ Teste 6 concluído"
echo ""

# ========================================
# 7. Reconhecer Alerta
# ========================================
echo "✅ 7. Reconhecendo um alerta..."
curl -X POST "${BASE_URL}/api/dashboard/alerts/${ALERT_ID}/acknowledge?tenantId=${TENANT_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "acknowledgedBy": "Dr. Maria Santos"
  }' \
  | jq '.'

echo ""
echo "✅ Teste 7 concluído"
echo ""

# ========================================
# Resumo
# ========================================
echo "============================================"
echo "🎉 Todos os testes concluídos!"
echo "============================================"
echo ""
echo "Endpoints testados:"
echo "  ✅ GET  /api/dashboard/patients"
echo "  ✅ GET  /api/dashboard/patients/{id}"
echo "  ✅ GET  /api/dashboard/patients/{id}/conversations"
echo "  ✅ GET  /api/dashboard/alerts"
echo "  ✅ GET  /api/dashboard/alerts/critical"
echo "  ✅ GET  /api/dashboard/patients/{id}/alerts"
echo "  ✅ POST /api/dashboard/alerts/{id}/acknowledge"
echo ""
echo "📝 Nota: Substitua os IDs de exemplo pelos IDs reais do seu sistema"
echo ""

