# EnergíaClara AI

Plataforma de gestión energética institucional con detección de anomalías, tickets de mantenimiento y retos educativos.

---

## Prerrequisitos

| Herramienta | Versión | Instalar |
|---|---|---|
| Java JDK | 21 | `winget install Microsoft.OpenJDK.21` |
| Maven | 3.9+ | `winget install Apache.Maven` |
| Node.js | 20+ | `winget install OpenJS.NodeJS.LTS` |
| SQL Server | 2019+ | `winget install Microsoft.SQLServer.2022.Developer` |
| SSMS | Última | `winget install Microsoft.SQLServerManagementStudio` |

> Después de instalar, cerrar y reabrir la terminal para actualizar el PATH.

Verificar:
```bash
java --version
mvn --version
node --version
```

---

## 1. Base de datos (SQL Server)

El schema canónico es `database/script.sql` (mantenido por el DBA del equipo). Es un dump completo generado desde SSMS con todos los schemas: `iam`, `core`, `consumo`, `energiaops`, `audit`, `analitica`, `educacion`, `mantenimiento`.

**Crear la DB y ejecutar el script:**

1. Abrir SSMS, conectar al servidor local.
2. Crear DB: `CREATE DATABASE EnergiaClaraDB;`
3. Abrir `database/script.sql` (encoding UTF-16 LE — SSMS lo lee directo).
4. Seleccionar la DB `EnergiaClaraDB` y ejecutar (F5).

**Seeds mínimos requeridos antes de levantar el backend:**

```sql
USE EnergiaClaraDB;

-- 1. Catálogo de roles (el backend mapea estos nombres a su enum)
INSERT INTO [iam].[rol] (rol_id, nombre, descripcion, nivel_alcance) VALUES
  (NEWID(), 'ADMIN_INSTITUCION', 'Configurador global de la institución', 'INSTITUCION'),
  (NEWID(), 'DIRECTOR',          'Supervisor de KPIs',                    'INSTITUCION'),
  (NEWID(), 'DOCENTE',           'Gestor cultural / retos',               'EDIFICIO'),
  (NEWID(), 'ESTUDIANTE',        'Participante operativo',                'EDIFICIO'),
  (NEWID(), 'TECNICO',           'Ejecutor de mantenimiento',             'EDIFICIO'),
  (NEWID(), 'AUDITOR',           'Validador de cumplimiento',             'INSTITUCION');

-- 2. Tenant demo
DECLARE @tenantId UNIQUEIDENTIFIER = '11111111-1111-1111-1111-111111111111';
INSERT INTO [core].[inquilino]
  (inquilino_id, nombre, nombre_legal, nit_rut, tipo_plan, factor_co2, codigo_moneda, esta_activo, creado_en, actualizado_en)
VALUES
  (@tenantId, 'Instituto Tecnológico Demo', 'Instituto Tecnológico Demo SA', '0000000000', 'FREEMIUM',
   0.250000, 'BOB', 1, SYSUTCDATETIME(), SYSUTCDATETIME());

-- 3. Admin demo (password: Admin1234!)
DECLARE @adminId UNIQUEIDENTIFIER = NEWID();
INSERT INTO [iam].[usuario]
  (usuario_id, inquilino_id, correo, nombre_completo, contrasena_hash, esta_activo, creado_en, actualizado_en)
VALUES
  (@adminId, @tenantId, 'admin@demo.edu', 'Administrador Demo',
   '$2a$10$kdFT40lwlms9N5VJiQ7ES.4a2it/uhBEGlZco19apZw3Y/3CIgmQW',
   1, SYSUTCDATETIME(), SYSUTCDATETIME());

-- 4. Asignar rol ADMIN_INSTITUCION al admin
INSERT INTO [iam].[usuario_rol]
  (usuario_rol_id, usuario_id, rol_id, inquilino_id, edificio_id, asignado_el, asignado_por)
SELECT NEWID(), @adminId, rol_id, @tenantId, NULL, SYSUTCDATETIME(), @adminId
FROM [iam].[rol] WHERE nombre = 'ADMIN_INSTITUCION';
```

> Para regenerar el hash de la contraseña: [bcrypt-generator.com](https://bcrypt-generator.com), rounds = 10.

---

## 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Corre en `http://localhost:8080`.

Variables de entorno (defaults para desarrollo local):

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=EnergiaClaraDB;encrypt=false;trustServerCertificate=true` | JDBC URL |
| `DB_USERNAME` | `sa` | Usuario SQL Server |
| `DB_PASSWORD` | `YourStrong!Passw0rd` | Contraseña SQL Server |
| `JWT_SECRET` | *(ver application.yml)* | Clave JWT — **cambiar en producción** |

> **Nota:** `ddl-auto: none` está activo. Hibernate no valida ni modifica schema en el arranque — el DBA es la fuente de verdad.

---

## 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Corre en `http://localhost:5173`. Hace proxy de `/api` → `http://localhost:8080`.

---

## Credenciales de prueba

| Campo | Valor |
|---|---|
| ID de Institución | `11111111-1111-1111-1111-111111111111` |
| Email | `admin@demo.edu` |
| Contraseña | `Admin1234!` |

---

## Estructura del proyecto

```
EnergiaClara-IA/
├── backend/                        # Spring Boot 3.2, Java 21
│   └── src/main/java/com/energiaclara/
│       ├── domain/                 # Aggregates, Value Objects, puertos
│       ├── application/            # Casos de uso, DTOs, servicios
│       ├── infrastructure/         # JPA (schema [iam]), JWT, Spring Security
│       ├── api/                    # REST controllers, DTOs, exception handler
│       └── bootstrap/              # Main class
├── frontend/                       # React 18 + Vite
│   └── src/
│       ├── context/                # AuthContext (JWT en localStorage)
│       ├── services/               # Axios + interceptor de token
│       ├── components/             # ProtectedRoute
│       └── pages/                  # LoginPage, DashboardPage
├── database/
│   └── script.sql                  # Schema canónico SQL Server (UTF-16 LE)
└── Pantallas/                      # Mockups HTML estáticos (dashboard, lecturas, etc.)
```

---

## Endpoints disponibles (MVP Auth)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Retorna JWT |
| POST | `/api/auth/register` | ADMIN_INSTITUCION | Crea usuario en el tenant |

### Ejemplo login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "email": "admin@demo.edu",
    "password": "Admin1234!"
  }'
```

### Ejemplo register (requiere token de admin)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "email": "tecnico@demo.edu",
    "fullName": "Juan Técnico Pérez",
    "password": "Tecnico1234!",
    "roles": ["TECNICO"]
  }'
```

---

## Roles disponibles

Los nombres deben coincidir EXACTAMENTE con `[iam].[rol].nombre` en la DB.

| Rol | Nivel | Descripción |
|---|---|---|
| `ADMIN_INSTITUCION` | Estratégico | Configura tarifas, crea usuarios |
| `DIRECTOR` | Estratégico | Supervisa KPIs, aprueba campañas |
| `DOCENTE` | Táctico | Crea retos educativos |
| `TECNICO` | Operativo | Atiende tickets de mantenimiento |
| `ESTUDIANTE` | Operativo | Participa en retos |
| `AUDITOR` | Táctico | Revisa anomalías y logs |
