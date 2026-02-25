# 🚀 Setup e Teste do Webhook Evolution API

## 📋 Visão Geral

Este guia detalha como configurar e testar o fluxo completo de integração WhatsApp → Evolution API → AI Health Agent.

---

## 🔧 Passo 1: Configurar Variáveis de Ambiente

### 1.1 Criar arquivo `.env`

```bash
cp .env.example .env
```

### 1.2 Editar `.env` com suas credenciais

```bash
# DATABASE
DATABASE_URL=jdbc:postgresql://localhost:5432/ai_health_agent
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha_aqui

# OPENAI API
OPENAI_API_KEY=sk-proj-sua-chave-aqui

# EVOLUTION API
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=minha-chave-secreta-123
EVOLUTION_API_INSTANCE=ai-health-instance
EVOLUTION_WEBHOOK_KEY=webhook-secret-key-456
```

---

## 🐳 Passo 2: Subir Evolution API (Docker)

### 2.1 Iniciar containers

```bash
docker-compose up -d evolution-api
```

### 2.2 Verificar se está rodando

```bash
docker ps
```

Você deve ver:
```
CONTAINER ID   IMAGE                          STATUS         PORTS
abc123def456   atendai/evolution-api:latest   Up 10 seconds  0.0.0.0:8081->8080/tcp
```

### 2.3 Acessar logs

```bash
docker logs -f evolution-api
```

---

## 📱 Passo 3: Conectar WhatsApp

### 3.1 Criar instância

```bash
curl -X POST http://localhost:8081/instance/create \
  -H "apikey: minha-chave-secreta-123" \
  -H "Content-Type: application/json" \
  -d '{
    "instanceName": "ai-health-instance",
    "qrcode": true
  }'
```

### 3.2 Obter QR Code

```bash
curl -X GET http://localhost:8081/instance/connect/ai-health-instance \
  -H "apikey: minha-chave-secreta-123"
```

**Resposta:**
```json
{
  "instance": {
    "instanceName": "ai-health-instance",
    "status": "open"
  },
  "qrcode": {
    "code": "2@abc123...",
    "base64": "data:image/png;base64,iVBORw0KG..."
  }
}
```

### 3.3 Escanear QR Code

1. Abra o WhatsApp no celular
2. Vá em **Configurações** → **Aparelhos conectados**
3. Toque em **Conectar um aparelho**
4. Escaneie o QR Code retornado no campo `base64`

---

## 🌐 Passo 4: Expor Localhost (ngrok)

### 4.1 Instalar ngrok

```bash
# Windows (Chocolatey)
choco install ngrok

# macOS (Homebrew)
brew install ngrok

# Linux
snap install ngrok
```

### 4.2 Iniciar túnel

```bash
ngrok http 8080
```

**Saída:**
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

**Copie a URL:** `https://abc123.ngrok.io`

---

## 🔗 Passo 5: Configurar Webhook na Evolution API

### 5.1 Configurar webhook global

```bash
curl -X POST http://localhost:8081/webhook/set/ai-health-instance \
  -H "apikey: minha-chave-secreta-123" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://abc123.ngrok.io/webhook/whatsapp",
    "webhook_by_events": true,
    "events": ["messages.upsert"]
  }'
```

**Resposta:**
```json
{
  "webhook": {
    "url": "https://abc123.ngrok.io/webhook/whatsapp",
    "enabled": true,
    "events": ["messages.upsert"]
  }
}
```

---

## 🏃 Passo 6: Iniciar Aplicação Spring Boot

### 6.1 Compilar

```bash
mvnw.cmd clean install -DskipTests
```

### 6.2 Executar

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

**Logs esperados:**
```
Started AiHealthAgentApplication in 5.234 seconds
Tomcat started on port(s): 8080 (http)
```

---

## 🧪 Passo 7: Testar Fluxo Completo

### 7.1 Cadastrar paciente de teste

```bash
curl -X POST http://localhost:8080/api/patients?tenantId=<UUID_DO_TENANT> \
  -H "Content-Type: application/json" \
  -d '{
    "whatsappNumber": "5511999999999",
    "name": "João Silva",
    "email": "joao@example.com",
    "diagnosis": "Fibromialgia",
    "isActive": true
  }'
```

### 7.2 Enviar mensagem via WhatsApp

No celular conectado, envie uma mensagem para o número da Evolution API:

```
Olá! Estou com dor 8 hoje
```

### 7.3 Verificar logs da aplicação

```
📨 Webhook recebido - Event: messages.upsert, Instance: ai-health-instance
📱 Mensagem recebida de 5511999999999: "Olá! Estou com dor 8 hoje"
🔐 Tenant identificado: <UUID> | Paciente: João Silva
✅ Contexto de segurança estabelecido
🤖 Resposta da IA gerada: 245 caracteres
✅ Mensagem enviada com sucesso para 5511999999999
✅ Fluxo completo executado com sucesso
```

### 7.4 Verificar resposta no WhatsApp

Você deve receber uma resposta personalizada da IA:

```
Sinto muito que esteja com dor, João. Em uma escala de 0 a 10, 
você classificou sua dor como 8, o que é bastante intenso.

Você já tomou sua medicação hoje?
```

---

## 🐛 Troubleshooting

### Problema 1: Webhook não recebe mensagens

**Sintomas:**
- Mensagens enviadas no WhatsApp não chegam na aplicação
- Logs não mostram `📨 Webhook recebido`

**Soluções:**
1. Verificar se ngrok está rodando: `curl https://abc123.ngrok.io/webhook/whatsapp`
2. Verificar configuração do webhook:
   ```bash
   curl -X GET http://localhost:8081/webhook/find/ai-health-instance \
     -H "apikey: minha-chave-secreta-123"
   ```
3. Verificar logs da Evolution API: `docker logs -f evolution-api`

---

### Problema 2: Erro 401 Unauthorized

**Sintomas:**
```
⚠️ Tentativa de acesso não autorizado ao webhook
```

**Solução:**
Verificar se o header `X-Webhook-Key` está correto:

```bash
# Testar manualmente
curl -X POST http://localhost:8080/webhook/whatsapp \
  -H "X-Webhook-Key: webhook-secret-key-456" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

### Problema 3: Paciente não encontrado

**Sintomas:**
```
❌ Paciente não encontrado: Paciente não cadastrado: 5511999999999
```

**Solução:**
1. Verificar se o paciente está cadastrado:
   ```bash
   curl http://localhost:8080/api/patients/whatsapp/5511999999999?tenantId=<UUID>
   ```

2. Cadastrar o paciente:
   ```bash
   curl -X POST http://localhost:8080/api/patients?tenantId=<UUID> \
     -H "Content-Type: application/json" \
     -d '{
       "whatsappNumber": "5511999999999",
       "name": "João Silva",
       "isActive": true
     }'
   ```

---

### Problema 4: Erro ao enviar mensagem via Evolution API

**Sintomas:**
```
❌ Erro ao enviar mensagem via Evolution API
```

**Soluções:**
1. Verificar se a Evolution API está rodando: `docker ps`
2. Verificar se a instância está conectada:
   ```bash
   curl http://localhost:8081/instance/connectionState/ai-health-instance \
     -H "apikey: minha-chave-secreta-123"
   ```
3. Verificar API Key no `.env`

---

## 📊 Teste Manual com cURL

### Simular webhook da Evolution API

```bash
curl -X POST http://localhost:8080/webhook/whatsapp \
  -H "X-Webhook-Key: webhook-secret-key-456" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "messages.upsert",
    "instance": "ai-health-instance",
    "data": {
      "key": {
        "remoteJid": "5511999999999@s.whatsapp.net",
        "fromMe": false,
        "id": "3EB0XXXXX"
      },
      "message": {
        "conversation": "Estou com dor 8 hoje"
      },
      "messageTimestamp": 1708387200,
      "pushName": "João Silva"
    }
  }'
```

**Resposta esperada:**
```json
{
  "status": "success",
  "whatsappNumber": "5511999999999",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "patientId": "987e6543-e21b-43d2-b654-426614174111",
  "messageId": "3EB0XXXXX",
  "responseLength": 245
}
```

---

## ✅ Checklist de Validação

- [ ] Evolution API rodando (porta 8081)
- [ ] WhatsApp conectado (QR Code escaneado)
- [ ] ngrok expondo localhost:8080
- [ ] Webhook configurado na Evolution API
- [ ] Aplicação Spring Boot rodando (porta 8080)
- [ ] Paciente cadastrado no banco
- [ ] Mensagem enviada via WhatsApp
- [ ] Resposta da IA recebida no WhatsApp
- [ ] Logs mostrando fluxo completo

---

**Documentação criada em:** 2026-02-19

