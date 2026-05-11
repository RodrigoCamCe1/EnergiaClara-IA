package com.energiaclara.infrastructure.persistence.audit;

import com.energiaclara.application.port.out.AuditPort;
import com.energiaclara.domain.model.audit.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditAdapter.class);

    private final AuditLogJpaRepository repository;

    public AuditAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Async("auditExecutor")
    public void record(AuditEvent event) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setId(UUID.randomUUID());
            entity.setTenantId(event.tenantId() != null ? event.tenantId().value() : null);
            entity.setUserId(event.userId() != null ? event.userId().value() : null);
            entity.setUserEmail(event.userEmail());
            entity.setAction(event.action());
            entity.setEntityName(event.entityName());
            entity.setEntityId(event.entityId());
            entity.setHttpMethod(event.httpMethod());
            entity.setEndpoint(event.endpoint());
            entity.setPreviousState(event.previousStateJson());
            entity.setNewState(event.newStateJson());
            entity.setIpAddress(event.ipAddress());
            entity.setUserAgent(event.userAgent());
            entity.setStatus(event.status().name());
            entity.setErrorMessage(event.errorMessage());
            entity.setDurationMs(event.durationMs());
            entity.setOccurredAt(event.occurredAt());

            repository.save(entity);
        } catch (Exception ex) {
            // Nunca propagar: la auditoría no puede tumbar la respuesta al usuario.
            log.error("Fallo persistiendo audit_log action={} entity={} endpoint={}",
                    event.action(), event.entityName(), event.endpoint(), ex);
        }
    }
}