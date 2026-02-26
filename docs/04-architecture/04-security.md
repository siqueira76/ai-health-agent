# 4.4 Segurança

## 🔒 Arquitetura de Segurança

O AI Health Agent implementa múltiplas camadas de segurança para proteger dados sensíveis de saúde.

---

## 🛡️ Camadas de Segurança

```
┌─────────────────────────────────────────────────────────┐
│  1. HTTPS/TLS                                           │
│     Criptografia em trânsito                            │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  2. AUTENTICAÇÃO                                        │
│     Basic Auth (dev) / JWT (prod)                       │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  3. AUTORIZAÇÃO                                         │
│     Role-based access control (RBAC)                    │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  4. ISOLAMENTO MULTI-TENANT                             │
│     Filtro automático por account_id                    │
└─────────────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  5. CRIPTOGRAFIA EM REPOUSO                             │
│     Dados sensíveis criptografados no banco             │
└─────────────────────────────────────────────────────────┘
```

---

## 🔐 Autenticação

### **Desenvolvimento: Basic Auth**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/webhook/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()); // Apenas para APIs REST
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Configuração:**
```properties
# ⚠️ APENAS DESENVOLVIMENTO - NÃO USAR EM PRODUÇÃO
spring.security.user.name=${ADMIN_USERNAME:admin}
spring.security.user.password=${ADMIN_PASSWORD:change_me_in_production}
```

---

### **Produção: JWT**

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long validityInMilliseconds;
    
    public String createToken(String username, UUID tenantId, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("tenantId", tenantId.toString());
        claims.put("roles", roles);
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

---

## 👥 Autorização (RBAC)

### **Roles:**

```java
public enum Role {
    PATIENT,           // Paciente (B2C)
    HEALTH_PROFESSIONAL, // Profissional de saúde (B2B)
    ADMIN              // Administrador do sistema
}
```

### **Controle de Acesso:**

```java
@RestController
@RequestMapping("/api/patients")
public class PatientController {
    
    @GetMapping
    @PreAuthorize("hasAnyRole('HEALTH_PROFESSIONAL', 'ADMIN')")
    public List<PatientDTO> listPatients() {
        UUID tenantId = TenantContext.getTenantId();
        return patientService.findByTenant(tenantId);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('HEALTH_PROFESSIONAL')")
    public PatientDTO getPatient(@PathVariable UUID id) {
        // Paciente só pode ver seus próprios dados
        // Profissional pode ver pacientes do seu tenant
        return patientService.findById(id);
    }
}
```

---

## 🔒 Isolamento Multi-Tenant

### **Validação Automática:**

```java
@Aspect
@Component
public class TenantSecurityAspect {
    
    @Before("execution(* com.healthlink..service.*.*(..))")
    public void validateTenantContext() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new SecurityException("Tenant context not set");
        }
    }
    
    @AfterReturning(
        pointcut = "execution(* com.healthlink..repository.*.find*(..))",
        returning = "result"
    )
    public void validateTenantIsolation(Object result) {
        if (result instanceof List) {
            ((List<?>) result).forEach(this::validateEntity);
        } else {
            validateEntity(result);
        }
    }
    
    private void validateEntity(Object entity) {
        if (entity instanceof TenantAware) {
            TenantAware tenantEntity = (TenantAware) entity;
            UUID currentTenant = TenantContext.getTenantId();
            
            if (!tenantEntity.getAccountId().equals(currentTenant)) {
                throw new SecurityException("Cross-tenant access detected!");
            }
        }
    }
}
```

---

## 🔐 Criptografia de Dados Sensíveis

### **Criptografia de Campos:**

```java
@Entity
public class Patient {
    
    @Id
    private UUID id;
    
    private String name;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String cpf; // CPF criptografado
    
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Email criptografado
}
```

### **Converter:**

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

---

## 🛡️ Proteção contra Ataques

### **1. SQL Injection**

```java
// ✅ SEGURO - Usa prepared statements
@Query("SELECT p FROM Patient p WHERE p.name = :name")
List<Patient> findByName(@Param("name") String name);

// ❌ INSEGURO - Nunca faça isso
@Query(value = "SELECT * FROM patients WHERE name = '" + name + "'", nativeQuery = true)
```

### **2. XSS (Cross-Site Scripting)**

```java
@RestController
public class PatientController {
    
    @PostMapping
    public PatientDTO create(@RequestBody @Valid PatientRequest request) {
        // Sanitizar entrada
        String safeName = HtmlUtils.htmlEscape(request.getName());
        return patientService.create(safeName, request.getPhone());
    }
}
```

### **3. CSRF (Cross-Site Request Forgery)**

```java
// Para APIs REST, CSRF pode ser desabilitado
http.csrf(csrf -> csrf.disable());

// Para aplicações web com sessões, manter habilitado
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
);
```

### **4. Rate Limiting**

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        
        String tenantId = request.getHeader("X-Tenant-ID");
        RateLimiter limiter = limiters.computeIfAbsent(
            tenantId, 
            k -> RateLimiter.create(100.0) // 100 req/s
        );
        
        if (!limiter.tryAcquire()) {
            response.setStatus(429); // Too Many Requests
            return false;
        }
        
        return true;
    }
}
```

---

## 🔑 Gestão de Secrets

### **Variáveis de Ambiente:**

```bash
# .env.local (NUNCA commitar!)
DATABASE_URL=jdbc:postgresql://localhost:5432/ai_health_agent
DB_USER=postgres
DB_PASSWORD=your_secure_password_here
OPENAI_API_KEY=sk-proj-xxxxxxxx
JWT_SECRET=your_jwt_secret_min_256_bits
ENCRYPTION_KEY=your_encryption_key_32_bytes
```

### **application.properties:**

```properties
# Usar placeholders
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
openai.api-key=${OPENAI_API_KEY}
jwt.secret=${JWT_SECRET}
encryption.key=${ENCRYPTION_KEY}
```

---

## ✅ Checklist de Segurança

### **Desenvolvimento:**
- [ ] Variáveis de ambiente configuradas
- [ ] `.env.local` no `.gitignore`
- [ ] Senhas fortes (mínimo 12 caracteres)
- [ ] HTTPS local (opcional)

### **Produção:**
- [ ] HTTPS obrigatório
- [ ] JWT implementado
- [ ] Secrets em vault (Railway/AWS Secrets Manager)
- [ ] Rate limiting ativo
- [ ] Logs de auditoria
- [ ] Backup automático
- [ ] Monitoramento de segurança
- [ ] Compliance LGPD/HIPAA

---

## 🎯 Próximos Passos

1. 🤖 [Spring AI Setup](../05-ai/01-spring-ai-setup.md)
2. 🔧 [Function Calling](../05-ai/02-function-calling.md)
3. 💬 [WhatsApp Integration](../07-whatsapp/01-evolution-api-setup.md)

---

[⬅️ Anterior: Design Patterns](03-design-patterns.md) | [⬆️ Índice](../README.md) | [➡️ Próximo: Spring AI Setup](../05-ai/01-spring-ai-setup.md)

