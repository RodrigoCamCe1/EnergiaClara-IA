-- ============================================================
-- EnergíaClara AI — Auditoría Forense
-- Tabla append-only para trazabilidad multi-tenant
-- ============================================================

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID,                              -- nullable: login fallido aún no tiene tenant resuelto
    user_id         UUID,                              -- nullable: acciones anónimas (intento de login)
    user_email      VARCHAR(255),                      -- desnormalizado para forense (sobrevive a borrado de user)
    action          VARCHAR(100) NOT NULL,             -- CREATE_TICKET, UPDATE_USER, DELETE_METER, etc.
    entity_name     VARCHAR(100),                      -- Ticket, User, Meter...
    entity_id       VARCHAR(100),                      -- string para soportar UUIDs y IDs compuestos
    http_method     VARCHAR(10) NOT NULL,              -- POST | PUT | DELETE
    endpoint        VARCHAR(500) NOT NULL,             -- /api/tickets/{id}
    previous_state  JSONB,                             -- estado antes (solo en PUT/DELETE)
    new_state       JSONB,                             -- payload del request o entidad resultante
    ip_address      INET,                              -- tipo nativo de PG, soporta IPv4/IPv6
    user_agent      VARCHAR(500),
    status          VARCHAR(20) NOT NULL,              -- SUCCESS | FAILURE
    error_message   TEXT,                              -- solo si status = FAILURE
    duration_ms     BIGINT,                            -- útil para correlacionar performance
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_audit_method CHECK (http_method IN ('POST', 'PUT', 'DELETE', 'PATCH')),
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS', 'FAILURE'))
);

-- ── Índices para consultas forenses típicas ─────────────────
CREATE INDEX idx_audit_tenant_time   ON audit_logs(tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_user_time     ON audit_logs(user_id, occurred_at DESC);
CREATE INDEX idx_audit_entity        ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_action        ON audit_logs(action);
CREATE INDEX idx_audit_occurred_at   ON audit_logs(occurred_at DESC);

-- Búsqueda dentro del JSONB (ej. WHERE new_state @> '{"status":"CRITICAL"}')
CREATE INDEX idx_audit_new_state_gin ON audit_logs USING GIN (new_state);

-- ── Inmutabilidad: protección contra tampering ──────────────
-- La auditoría forense NUNCA debe actualizarse ni borrarse.
-- El rol de aplicación solo recibirá INSERT y SELECT.
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs es append-only: operación % bloqueada', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_no_update
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();