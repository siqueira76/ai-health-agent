#!/bin/bash

# ============================================
# Script de Teste do Webhook Evolution API
# ============================================

echo "🧪 Testando Webhook do AI Health Agent"
echo "========================================"
echo ""

# Configurações
WEBHOOK_URL="http://localhost:8080/webhook/whatsapp"
WEBHOOK_KEY="webhook-secret-key-456"
WHATSAPP_NUMBER="5511999999999"

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============================================
# Teste 1: Webhook sem autenticação
# ============================================
echo "📝 Teste 1: Webhook sem autenticação (deve falhar)"
echo "---------------------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $WEBHOOK_URL \
  -H "Content-Type: application/json" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-instance",
    "data": {
      "key": {
        "remoteJid": "'$WHATSAPP_NUMBER'@s.whatsapp.net",
        "fromMe": false,
        "id": "TEST001"
      },
      "message": {
        "conversation": "Teste sem autenticação"
      }
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "401" ]; then
  echo -e "${GREEN}✅ PASSOU${NC} - Retornou 401 Unauthorized"
else
  echo -e "${RED}❌ FALHOU${NC} - Esperado 401, recebido $HTTP_CODE"
fi

echo ""

# ============================================
# Teste 2: Webhook com autenticação válida
# ============================================
echo "📝 Teste 2: Webhook com autenticação válida"
echo "-------------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $WEBHOOK_URL \
  -H "X-Webhook-Key: $WEBHOOK_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-instance",
    "data": {
      "key": {
        "remoteJid": "'$WHATSAPP_NUMBER'@s.whatsapp.net",
        "fromMe": false,
        "id": "TEST002"
      },
      "message": {
        "conversation": "Olá! Estou com dor 8 hoje"
      },
      "messageTimestamp": 1708387200,
      "pushName": "João Silva"
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "404" ]; then
  echo -e "${GREEN}✅ PASSOU${NC} - Retornou $HTTP_CODE"
  echo "Resposta: $BODY"
else
  echo -e "${RED}❌ FALHOU${NC} - Esperado 200 ou 404, recebido $HTTP_CODE"
  echo "Resposta: $BODY"
fi

echo ""

# ============================================
# Teste 3: Mensagem fromMe=true (deve ignorar)
# ============================================
echo "📝 Teste 3: Mensagem fromMe=true (deve ignorar)"
echo "-----------------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $WEBHOOK_URL \
  -H "X-Webhook-Key: $WEBHOOK_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-instance",
    "data": {
      "key": {
        "remoteJid": "'$WHATSAPP_NUMBER'@s.whatsapp.net",
        "fromMe": true,
        "id": "TEST003"
      },
      "message": {
        "conversation": "Mensagem enviada por nós"
      }
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "200" ] && echo "$BODY" | grep -q "ignored"; then
  echo -e "${GREEN}✅ PASSOU${NC} - Mensagem ignorada corretamente"
else
  echo -e "${RED}❌ FALHOU${NC} - Mensagem não foi ignorada"
fi

echo ""

# ============================================
# Teste 4: Webhook com dados inválidos
# ============================================
echo "📝 Teste 4: Webhook com dados inválidos"
echo "---------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $WEBHOOK_URL \
  -H "X-Webhook-Key: $WEBHOOK_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-instance",
    "data": {}
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "400" ]; then
  echo -e "${GREEN}✅ PASSOU${NC} - Retornou 400 Bad Request"
else
  echo -e "${YELLOW}⚠️  AVISO${NC} - Esperado 400, recebido $HTTP_CODE"
fi

echo ""
echo "========================================"
echo "🏁 Testes concluídos!"
echo "========================================"

