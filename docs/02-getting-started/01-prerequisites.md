# 2.1 Pré-requisitos

## 📋 Requisitos do Sistema

Antes de começar, certifique-se de ter os seguintes softwares instalados:

---

## ☕ Java Development Kit (JDK)

### **Versão Requerida:** Java 21+

### **Instalação**

#### **Windows**
```bash
# Via Chocolatey
choco install openjdk21

# Ou baixe manualmente
https://adoptium.net/temurin/releases/
```

#### **macOS**
```bash
# Via Homebrew
brew install openjdk@21

# Adicionar ao PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
```

#### **Linux (Ubuntu/Debian)**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

### **Verificação**
```bash
java -version
# Saída esperada: openjdk version "21.x.x"
```

---

## 🐘 PostgreSQL

### **Versão Requerida:** PostgreSQL 16+

### **Opção 1: Docker (Recomendado)**

```bash
# Subir PostgreSQL via Docker Compose
docker-compose -f docker-compose.test.yml up -d ai-health-postgres-test

# Verificar se está rodando
docker ps | grep postgres
```

**Vantagens:**
- ✅ Não polui o sistema
- ✅ Fácil de resetar
- ✅ Mesma versão em todos os ambientes

### **Opção 2: Instalação Local**

#### **Windows**
```bash
# Via Chocolatey
choco install postgresql16

# Ou baixe o instalador
https://www.postgresql.org/download/windows/
```

#### **macOS**
```bash
# Via Homebrew
brew install postgresql@16
brew services start postgresql@16
```

#### **Linux (Ubuntu/Debian)**
```bash
sudo apt update
sudo apt install postgresql-16 postgresql-contrib-16
sudo systemctl start postgresql
```

### **Verificação**
```bash
psql --version
# Saída esperada: psql (PostgreSQL) 16.x
```

---

## 🐳 Docker & Docker Compose

### **Versão Requerida:** Docker 24+, Docker Compose 2.20+

### **Instalação**

#### **Windows/macOS**
Baixe o Docker Desktop:
```
https://www.docker.com/products/docker-desktop/
```

#### **Linux (Ubuntu/Debian)**
```bash
# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Adicionar usuário ao grupo docker
sudo usermod -aG docker $USER

# Instalar Docker Compose
sudo apt install docker-compose-plugin
```

### **Verificação**
```bash
docker --version
# Saída esperada: Docker version 24.x.x

docker compose version
# Saída esperada: Docker Compose version v2.20.x
```

---

## 🔨 Maven

### **Versão Requerida:** Maven 3.9+

### **Instalação**

#### **Windows**
```bash
# Via Chocolatey
choco install maven
```

#### **macOS**
```bash
# Via Homebrew
brew install maven
```

#### **Linux (Ubuntu/Debian)**
```bash
sudo apt update
sudo apt install maven
```

### **Verificação**
```bash
mvn -version
# Saída esperada: Apache Maven 3.9.x
```

**Nota:** O projeto inclui Maven Wrapper (`mvnw`), então Maven não é estritamente necessário.

---

## 🔑 Chaves de API

### **OpenAI API Key**

1. Crie uma conta em: https://platform.openai.com/
2. Navegue até: API Keys
3. Clique em "Create new secret key"
4. Copie a chave (começa com `sk-...`)
5. **IMPORTANTE:** Guarde em local seguro (não será mostrada novamente)

**Custo estimado:**
- GPT-4o-mini: ~$0.15 por 1M tokens de entrada
- Uso médio: ~$5-10/mês para testes

### **Evolution API (WhatsApp)**

**Opção 1: Self-hosted (Gratuito)**
```bash
# Clone o repositório
git clone https://github.com/EvolutionAPI/evolution-api.git
cd evolution-api

# Configure e suba
docker-compose up -d
```

**Opção 2: Cloud (Pago)**
- https://evolution-api.com/
- Planos a partir de $9/mês

---

## 💻 IDE (Opcional, mas Recomendado)

### **IntelliJ IDEA**

**Versão:** Community (gratuita) ou Ultimate

**Download:** https://www.jetbrains.com/idea/download/

**Plugins Recomendados:**
- Lombok
- Spring Boot
- Database Navigator
- GitToolBox

### **VS Code (Alternativa)**

**Download:** https://code.visualstudio.com/

**Extensões Recomendadas:**
- Extension Pack for Java
- Spring Boot Extension Pack
- Docker
- PostgreSQL

---

## 🌐 Ferramentas Adicionais

### **Postman ou Insomnia**
Para testar endpoints da API

**Postman:** https://www.postman.com/downloads/  
**Insomnia:** https://insomnia.rest/download

### **DBeaver (Opcional)**
Cliente SQL universal para gerenciar PostgreSQL

**Download:** https://dbeaver.io/download/

### **Git**
Para controle de versão

```bash
# Verificar instalação
git --version

# Instalar se necessário
# Windows: https://git-scm.com/download/win
# macOS: brew install git
# Linux: sudo apt install git
```

---

## ✅ Checklist de Pré-requisitos

Antes de prosseguir, verifique:

- [ ] Java 21+ instalado e no PATH
- [ ] PostgreSQL 16+ rodando (Docker ou local)
- [ ] Docker e Docker Compose instalados
- [ ] Maven instalado (ou usar mvnw)
- [ ] OpenAI API Key obtida
- [ ] Evolution API configurada (ou planejada)
- [ ] IDE instalada (IntelliJ ou VS Code)
- [ ] Git instalado
- [ ] Postman/Insomnia instalado

---

## 🎯 Próximos Passos

Agora que você tem todos os pré-requisitos:

1. 📥 [Instalação do Projeto](02-installation.md)
2. ⚙️ [Configuração](03-configuration.md)
3. 🚀 [Primeiro Deploy](04-first-deploy.md)

---

[⬅️ Anterior: Modelos de Negócio](../01-overview/04-business-models.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Instalação](02-installation.md)

