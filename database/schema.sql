-- ============================================================
-- EnergíaClara AI — Schema inicial (MVP Auth)
-- PostgreSQL 15+
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Tenants ──────────────────────────────────────────────────
CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Users ─────────────────────────────────────────────────────
-- tenant_id + email es la clave natural (un email puede existir en distintos tenants)
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email_tenant UNIQUE (email, tenant_id)
);

-- ── User Roles ────────────────────────────────────────────────
-- Collection table para Set<Role> del aggregate User
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT chk_role CHECK (role IN (
        'ADMIN_INSTITUCION',
        'DIRECTOR',
        'DOCENTE',
        'ESTUDIANTE',
        'TECNICO',
        'AUDITOR'
    ))
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_users_tenant_id       ON users(tenant_id);
CREATE INDEX idx_users_email_tenant    ON users(email, tenant_id);
CREATE INDEX idx_user_roles_user_id    ON user_roles(user_id);

-- ============================================================
-- SEED — datos mínimos para arrancar
-- ============================================================

-- Tenant demo
INSERT INTO tenants (id, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'Instituto Tecnológico Demo');

-- Admin inicial
-- Contraseña: Admin1234!
-- Hash generado con BCrypt strength=10
-- Para regenerar: https://bcrypt-generator.com  (rounds=10)
INSERT INTO users (id, tenant_id, email, hashed_password, active, created_at)
VALUES (
    uuid_generate_v4(),
    '11111111-1111-1111-1111-111111111111',
    'admin@demo.edu',
    '$2a$10$7QJ7gQ1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcde',  -- REEMPLAZAR con hash real
    true,
    NOW()
);

-- Rol del admin (insertar después del usuario)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN_INSTITUCION'
FROM users
WHERE email = 'admin@demo.edu'
  AND tenant_id = '11111111-1111-1111-1111-111111111111';
