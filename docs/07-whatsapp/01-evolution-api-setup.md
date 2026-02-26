# 7.1 Evolution API Setup

## 📱 Configurando Evolution API

Evolution API é uma API não oficial do WhatsApp que permite enviar e receber mensagens programaticamente.

---

## 🎯 Opções de Deploy

### **Opção 1: Cloud (Recomendado para Produção)**

Usar serviço gerenciado:
- **Evolution API Cloud**: https://evolution-api.com
- **Preço**: ~$10-30/mês por instância
- **Vantagens**: Sem manutenção, suporte, uptime garantido

### **Opção 2: Self-Hosted (Desenvolvimento)**

Rodar localmente ou em VPS:
- **Custo**: Grátis (exceto servidor)
- **Vantagens**: Controle total, sem limites
- **Desvantagens**: Manutenção, uptime

---

## 🚀 Setup Cloud (Recomendado)

### **1. Criar Conta:**

1. Acesse https://evolution-api.com
2. Crie uma conta
3. Crie uma nova instância
4. Copie a API Key

### **2. Configurar Aplicação:**

```properties
# application.properties
evolution.api.url=https://api.evolution-api.com
evolution.api.key=your_api_key_here
evolution.instance.name=ai-health-agent
```

### **3. Conectar WhatsApp:**

```bash
# Obter QR Code
curl -X GET "https://api.evolution-api.com/instance/connect/ai-health-agent" \
  -H "apikey: your_api_key_here"
```

Escaneie o QR Code com seu WhatsApp.

---

## 🐳 Setup Self-Hosted (Docker)

### **1. docker-compose.yml:**

```yaml
version: '3.8'

services:
  evolution-api:
    image: atendai/evolution-api:latest
    container_name: evolution-api
    ports:
      - "8081:8080"
    environment:
      # Server
      - SERVER_URL=http://localhost:8081
      - SERVER_PORT=8080
      
      # Database (PostgreSQL)
      - DATABASE_ENABLED=true
      - DATABASE_PROVIDER=postgresql
      - DATABASE_CONNECTION_URI=postgresql://postgres:postgres@postgres:5432/evolution
      - DATABASE_SAVE_DATA_INSTANCE=true
      - DATABASE_SAVE_DATA_NEW_MESSAGE=true
      
      # Authentication
      - AUTHENTICATION_API_KEY=change_this_to_secure_key
      
      # Webhook
      - WEBHOOK_GLOBAL_ENABLED=true
      - WEBHOOK_GLOBAL_URL=http://host.docker.internal:8080/webhook/whatsapp
      - WEBHOOK_GLOBAL_WEBHOOK_BY_EVENTS=true
      
      # WhatsApp
      - QRCODE_LIMIT=30
      - QRCODE_COLOR=#198754
      
    volumes:
      - evolution_instances:/evolution/instances
      - evolution_store:/evolution/store
    
    depends_on:
      - postgres
    
    networks:
      - evolution-network

  postgres:
    image: postgres:16-alpine
    container_name: evolution-postgres
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=evolution
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - evolution-network

volumes:
  evolution_instances:
  evolution_store:
  postgres_data:

networks:
  evolution-network:
    driver: bridge
```

### **2. Iniciar:**

```bash
docker-compose up -d
```

### **3. Acessar:**

```
http://localhost:8081
```

---

## 🔗 Configurar Webhook

### **1. Criar Instância:**

```bash
curl -X POST "http://localhost:8081/instance/create" \
  -H "apikey: change_this_to_secure_key" \
  -H "Content-Type: application/json" \
  -d '{
    "instanceName": "ai-health-agent",
    "qrcode": true,
    "integration": "WHATSAPP-BAILEYS"
  }'
```

### **2. Configurar Webhook:**

```bash
curl -X POST "http://localhost:8081/webhook/set/ai-health-agent" \
  -H "apikey: change_this_to_secure_key" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "http://your-app-url.com/webhook/whatsapp",
    "webhook_by_events": true,
    "events": [
      "MESSAGES_UPSERT",
      "MESSAGES_UPDATE",
      "CONNECTION_UPDATE"
    ]
  }'
```

### **3. Conectar WhatsApp:**

```bash
# Obter QR Code
curl -X GET "http://localhost:8081/instance/connect/ai-health-agent" \
  -H "apikey: change_this_to_secure_key"
```

Resposta:
```json
{
  "qrcode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "code": "ABCD-EFGH-IJKL"
}
```

Escaneie o QR Code com WhatsApp.

---

## 📤 Testando Envio de Mensagens

### **Enviar Mensagem de Texto:**

```bash
curl -X POST "http://localhost:8081/message/sendText/ai-health-agent" \
  -H "apikey: change_this_to_secure_key" \
  -H "Content-Type: application/json" \
  -d '{
    "number": "5511999999999",
    "text": "Olá! Esta é uma mensagem de teste."
  }'
```

### **Enviar Mensagem com Mídia:**

```bash
curl -X POST "http://localhost:8081/message/sendMedia/ai-health-agent" \
  -H "apikey: change_this_to_secure_key" \
  -H "Content-Type: application/json" \
  -d '{
    "number": "5511999999999",
    "mediatype": "image",
    "media": "https://example.com/image.jpg",
    "caption": "Confira esta imagem!"
  }'
```

---

## 🔧 Configuração na Aplicação

### **application.properties:**

```properties
# ============================================
# EVOLUTION API
# ============================================
evolution.api.url=${EVOLUTION_API_URL:http://localhost:8081}
evolution.api.key=${EVOLUTION_API_KEY:change_this_to_secure_key}
evolution.instance.name=${EVOLUTION_INSTANCE_NAME:ai-health-agent}

# Webhook Security
evolution.webhook.key=${EVOLUTION_WEBHOOK_KEY:your_webhook_secret_key}
```

### **.env.local:**

```bash
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=change_this_to_secure_key
EVOLUTION_INSTANCE_NAME=ai-health-agent
EVOLUTION_WEBHOOK_KEY=your_webhook_secret_key
```

---

## 🔍 Verificar Status

### **Status da Instância:**

```bash
curl -X GET "http://localhost:8081/instance/fetchInstances" \
  -H "apikey: change_this_to_secure_key"
```

### **Status da Conexão:**

```bash
curl -X GET "http://localhost:8081/instance/connectionState/ai-health-agent" \
  -H "apikey: change_this_to_secure_key"
```

Resposta:
```json
{
  "instance": "ai-health-agent",
  "state": "open"
}
```

Estados possíveis:
- `open` - Conectado ✅
- `connecting` - Conectando...
- `close` - Desconectado ❌

---

## 🐛 Troubleshooting

### **Problema: QR Code não aparece**

```bash
# Reiniciar instância
curl -X PUT "http://localhost:8081/instance/restart/ai-health-agent" \
  -H "apikey: change_this_to_secure_key"
```

### **Problema: Webhook não recebe mensagens**

1. Verificar URL do webhook:
```bash
curl -X GET "http://localhost:8081/webhook/find/ai-health-agent" \
  -H "apikey: change_this_to_secure_key"
```

2. Testar webhook manualmente:
```bash
curl -X POST "http://localhost:8080/webhook/whatsapp" \
  -H "Content-Type: application/json" \
  -H "X-Evolution-Key: your_webhook_secret_key" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-agent",
    "data": {
      "key": {
        "remoteJid": "5511999999999@s.whatsapp.net",
        "fromMe": false,
        "id": "test123"
      },
      "message": {
        "conversation": "Teste"
      }
    }
  }'
```

### **Problema: Mensagens não são enviadas**

Verificar logs:
```bash
docker logs evolution-api -f
```

---

## 📊 Monitoramento

### **Logs em Tempo Real:**

```bash
docker logs evolution-api -f --tail 100
```

### **Métricas:**

```bash
curl -X GET "http://localhost:8081/instance/fetchInstances" \
  -H "apikey: change_this_to_secure_key" | jq
```

---

## 🎯 Próximos Passos

1. 🔔 [Check-ins Proativos](../08-checkins/01-proactive-checkins.md)
2. 📊 [Analytics](../09-analytics/01-health-analytics.md)
3. 🧪 [Testes](../10-testing/01-unit-tests.md)

---

[⬅️ Anterior: Webhook WhatsApp](../06-api/02-webhook-whatsapp.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Check-ins Proativos](../08-checkins/01-proactive-checkins.md)

