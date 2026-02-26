# 4.2 Multi-Tenancy

## 🏢 Arquitetura Multi-Tenant

O AI Health Agent usa **Shared Database, Shared Schema** com isolamento via `account_id`.

---

## 📊 Estratégias de Multi-Tenancy

### **Opções Disponíveis:**

```
┌─────────────────────────────────────────────────────────┐
│  1. SEPARATE DATABASE (Banco separado por tenant)       │
│     ✅ Isolamento máximo                                │
│     ❌ Custo alto, difícil de escalar                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  2. SHARED DATABASE, SEPARATE SCHEMA (Schema separado)  │
│     ✅ Bom isolamento                                   │
│     ❌ Complexidade média                               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  3. SHARED DATABASE, SHARED SCHEMA (Nossa escolha) ✅   │
│     ✅ Custo baixo, fácil de escalar                    │
│     ✅ Simples de implementar                           │
│     ⚠️  Requer cuidado com isolamento                   │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 Implementação do Isolamento

### **1. TenantContext (ThreadLocal)**

```java
@Component
public class TenantContext {
    
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(UUID tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static UUID getTenantId() {
        UUID tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new TenantNotFoundException("No tenant context set");
        }
        return tenantId;
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

---

### **2. TenantInterceptor (Extração do Tenant)**

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        
        // Opção 1: Via header
        String tenantId = request.getHeader("X-Tenant-ID");
        
        // Opção 2: Via JWT token
        // String tenantId = extractFromJwt(request);
        
        // Opção 3: Via subdomain
        // String tenantId = extractFromSubdomain(request);
        
        if (tenantId != null) {
            TenantContext.setTenantId(UUID.fromString(tenantId));
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        TenantContext.clear();
    }
}
```

---

### **3. Registro do Interceptor**

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private TenantInterceptor tenantInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/webhook/**"); // Webhook usa phone number
    }
}
```

---

## 🗄️ Isolamento no Banco de Dados

### **Todas as tabelas têm `account_id`:**

```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id), -- 🔐 Isolamento
    name VARCHAR(255),
    whatsapp_number VARCHAR(20),
    ...
);

CREATE TABLE health_logs (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id), -- 🔐 Isolamento
    patient_id UUID NOT NULL,
    pain_level INTEGER,
    ...
);
```

---

### **Queries sempre filtram por tenant:**

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    // ❌ ERRADO - Sem filtro de tenant
    @Query("SELECT p FROM Patient p WHERE p.id = :id")
    Optional<Patient> findById(@Param("id") UUID id);
    
    // ✅ CORRETO - Com filtro de tenant
    @Query("SELECT p FROM Patient p WHERE p.id = :id AND p.account.id = :tenantId")
    Optional<Patient> findByIdAndTenant(
        @Param("id") UUID id, 
        @Param("tenantId") UUID tenantId
    );
}
```

---

## 🛡️ Proteção Automática com Aspect

### **TenantAspect (AOP)**

```java
@Aspect
@Component
public class TenantAspect {
    
    @Around("execution(* com.healthlink..repository.*.*(..))")
    public Object enforceTenantIsolation(ProceedingJoinPoint joinPoint) throws Throwable {
        
        UUID tenantId = TenantContext.getTenantId();
        
        // Verificar se query inclui tenantId
        Object[] args = joinPoint.getArgs();
        boolean hasTenantId = Arrays.stream(args)
            .anyMatch(arg -> arg != null && arg.equals(tenantId));
        
        if (!hasTenantId) {
            throw new SecurityException("Query must include tenant isolation");
        }
        
        return joinPoint.proceed();
    }
}
```

---

## 🔍 Identificação do Tenant

### **Opção 1: Header HTTP**

```bash
curl -H "X-Tenant-ID: 123e4567-e89b-12d3-a456-426614174000" \
     http://localhost:8080/api/patients
```

### **Opção 2: JWT Token**

```java
public UUID extractTenantFromJwt(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(secretKey)
        .parseClaimsJws(token)
        .getBody();
    
    return UUID.fromString(claims.get("tenantId", String.class));
}
```

### **Opção 3: Subdomain**

```java
public UUID extractTenantFromSubdomain(HttpServletRequest request) {
    String host = request.getServerName(); // clinic1.healthlink.com
    String subdomain = host.split("\\.")[0]; // clinic1
    
    return accountRepository.findBySubdomain(subdomain)
        .map(Account::getId)
        .orElseThrow(() -> new TenantNotFoundException());
}
```

### **Opção 4: WhatsApp Number (Webhook)**

```java
@PostMapping("/webhook/whatsapp")
public ResponseEntity<Void> handleWebhook(@RequestBody WebhookPayload payload) {
    
    // Buscar tenant pelo número do WhatsApp
    Patient patient = patientRepository.findByWhatsappNumber(payload.getPhone())
        .orElseThrow(() -> new PatientNotFoundException());
    
    // Definir contexto do tenant
    TenantContext.setTenantId(patient.getAccount().getId());
    
    // Processar mensagem
    messageService.processIncomingMessage(payload);
    
    return ResponseEntity.ok().build();
}
```

---

## 🧪 Testando Isolamento

### **Teste de Isolamento:**

```java
@Test
void shouldNotAccessOtherTenantData() {
    // Tenant 1
    UUID tenant1 = UUID.randomUUID();
    TenantContext.setTenantId(tenant1);
    Patient patient1 = patientRepository.save(new Patient("João"));
    
    // Tenant 2
    UUID tenant2 = UUID.randomUUID();
    TenantContext.setTenantId(tenant2);
    
    // Tentar acessar paciente do Tenant 1
    Optional<Patient> result = patientRepository.findByIdAndTenant(
        patient1.getId(), 
        tenant2
    );
    
    // Deve estar vazio (isolamento funcionando)
    assertThat(result).isEmpty();
}
```

---

## ⚠️ Cuidados Importantes

### **1. Sempre limpar contexto:**

```java
try {
    TenantContext.setTenantId(tenantId);
    // Processar requisição
} finally {
    TenantContext.clear(); // ⚠️ IMPORTANTE!
}
```

### **2. Validar tenant em todas as queries:**

```java
// ❌ NUNCA faça isso
@Query("SELECT p FROM Patient p")
List<Patient> findAll();

// ✅ SEMPRE faça isso
@Query("SELECT p FROM Patient p WHERE p.account.id = :tenantId")
List<Patient> findAllByTenant(@Param("tenantId") UUID tenantId);
```

### **3. Testes de segurança:**

```java
@Test
void shouldPreventCrossTenantAccess() {
    // Criar dados em tenant1
    // Tentar acessar de tenant2
    // Deve falhar
}
```

---

## 🎯 Próximos Passos

1. 🎨 [Design Patterns](03-design-patterns.md)
2. 🔒 [Segurança](04-security.md)
3. 🤖 [Spring AI Setup](../05-ai/01-spring-ai-setup.md)

---

[⬅️ Anterior: Arquitetura em Camadas](01-layered-architecture.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Design Patterns](03-design-patterns.md)

