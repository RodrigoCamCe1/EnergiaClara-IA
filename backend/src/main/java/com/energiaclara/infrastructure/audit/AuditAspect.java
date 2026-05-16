package com.energiaclara.infrastructure.audit;

import com.energiaclara.api.rest.audit.Audited;
import com.energiaclara.application.port.out.AuditPort;
import com.energiaclara.domain.model.audit.AuditEvent;
import com.energiaclara.domain.model.audit.AuditEvent.AuditStatus;
import com.energiaclara.domain.model.vo.TenantId;
import com.energiaclara.domain.model.vo.UserId;
import com.energiaclara.infrastructure.security.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final SpelExpressionParser SPEL = new SpelExpressionParser();

    private final AuditPort auditPort;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditPort auditPort, ObjectMapper objectMapper) {
        this.auditPort = auditPort;
        this.objectMapper = objectMapper;
    }

    /**
     * Pointcut: TODO método de controller en api.rest anotado con @PostMapping, @PutMapping o @DeleteMapping.
     * Capturamos los tres por separado en lugar de un solo "execution(* api.rest..*(..))" para no auditar GETs.
     */
    @Around(
        "execution(* com.energiaclara.api.rest..*(..)) && " +
        "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
        " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
        " @annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
        " @annotation(org.springframework.web.bind.annotation.PatchMapping))"
    )
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        Object result = null;
        Throwable thrown = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            try {
                AuditEvent event = buildEvent(pjp, method, audited, request, result, thrown,
                        System.currentTimeMillis() - start);
                auditPort.record(event); // async fire-and-forget
            } catch (Exception buildErr) {
                // Auditar nunca debe romper la request.
                log.error("No se pudo construir el AuditEvent", buildErr);
            }
        }
    }

    private AuditEvent buildEvent(ProceedingJoinPoint pjp,
                                  Method method,
                                  Audited audited,
                                  HttpServletRequest request,
                                  Object result,
                                  Throwable thrown,
                                  long durationMs) {

        // ── Contexto de seguridad ──
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (auth != null && auth.isAuthenticated()) ? String.valueOf(auth.getPrincipal()) : null;
        UserId userId = extractUserId(auth);

        // ── Tenant ──
        String tenantStr = TenantContextHolder.get();
        TenantId tenantId = (tenantStr != null) ? safeTenant(tenantStr) : null;

        // ── HTTP ──
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String endpoint = request != null ? request.getRequestURI() : "";
        String ip = extractIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : null;

        // ── Acción / entidad ──
        String action = audited != null ? audited.action() : defaultAction(httpMethod, method.getName());
        String entityName = audited != null ? audited.entity() : "";
        String entityId = resolveEntityId(audited, pjp, result);

        // ── Payloads ──
        // new_state: para POST/PUT/PATCH usamos el body (primer @RequestBody) o el result en POSTs que devuelven el id.
        String newStateJson = toJsonSafe(pickRequestBody(method, pjp.getArgs()));
        // previous_state queda nulo aquí; para PUT/DELETE se sugiere registrar manualmente desde el use case (ver nota).
        String previousStateJson = null;

        AuditStatus status = (thrown == null) ? AuditStatus.SUCCESS : AuditStatus.FAILURE;
        String errorMessage = (thrown == null) ? null : (thrown.getClass().getSimpleName() + ": " + thrown.getMessage());

        return new AuditEvent(
                tenantId, userId, userEmail,
                action, entityName, entityId,
                httpMethod, endpoint,
                previousStateJson, newStateJson,
                ip, ua,
                status, errorMessage,
                durationMs, Instant.now()
        );
    }

    private static String defaultAction(String httpMethod, String methodName) {
        return httpMethod + "_" + methodName.toUpperCase();
    }

    private String resolveEntityId(Audited audited, ProceedingJoinPoint pjp, Object result) {
        if (audited == null || audited.entityIdExpression().isBlank()) return null;
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            Parameter[] params = ((MethodSignature) pjp.getSignature()).getMethod().getParameters();
            Object[] args = pjp.getArgs();
            for (int i = 0; i < params.length; i++) {
                ctx.setVariable(params[i].getName(), args[i]);
            }
            ctx.setVariable("result", result);
            Expression exp = SPEL.parseExpression(audited.entityIdExpression());
            Object value = exp.getValue(ctx);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("No se pudo resolver entityIdExpression='{}': {}",
                    audited.entityIdExpression(), e.getMessage());
            return null;
        }
    }

    private Object pickRequestBody(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(org.springframework.web.bind.annotation.RequestBody.class)) {
                return scrub(args[i]);
            }
        }
        return null;
    }

    /** Evita serializar campos sensibles (password, secret, token). */
    private Object scrub(Object body) {
        if (body == null) return null;
        try {
            var node = objectMapper.valueToTree(body);
            if (node.isObject()) {
                var obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
                for (String sensitive : new String[]{"password", "rawPassword", "secret", "token"}) {
                    if (obj.has(sensitive)) obj.put(sensitive, "***REDACTED***");
                }
            }
            return node;
        } catch (Exception e) {
            return "<<unserializable>>";
        }
    }

    private String toJsonSafe(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "\"<<serialization-error>>\"";
        }
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }

    private static String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private TenantId safeTenant(String s) {
        try { return TenantId.of(UUID.fromString(s)); } catch (Exception e) { return null; }
    }

    /**
     * El JwtAuthFilter actual pone el email como principal y NO el userId.
     * Aquí dejamos un hook: si en un futuro Sebastián expone userId en el token,
     * se puede leer desde Authentication.getDetails() o un custom principal.
     */
    private UserId extractUserId(Authentication auth) {
        if (auth == null) return null;
        Object details = auth.getDetails();
        if (details instanceof UserId uid) return uid;
        return null;
    }
}