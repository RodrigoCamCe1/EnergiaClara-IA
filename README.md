# EnergíaClara AI

Plataforma de gestión energética institucional con detección de anomalías, tickets de mantenimiento y retos educativos.

---

## Prerrequisitos

| Herramienta | Versión | Instalar |
|---|---|---|
| Java JDK | 21 | `winget install Microsoft.OpenJDK.21` |
| Maven | 3.9+ | `winget install Apache.Maven` |
| Node.js | 20+ | `winget install OpenJS.NodeJS.LTS` |
| PostgreSQL | 15+ | `winget install PostgreSQL.PostgreSQL` |

> Después de instalar, cerrar y reabrir la terminal para actualizar el PATH.

Verificar:
```bash
java --version
mvn --version
node --version
psql --version
```

---

## 1. Base de datos

Conectarse a PostgreSQL y ejecutar:

```sql
CREATE DATABASE energiaclara;
CREATE USER energiaclara WITH PASSWORD 'energiaclara';
GRANT ALL PRIVILEGES ON DATABASE energiaclara TO energiaclara;
```

Luego ejecutar el schema completo:

```bash
psql -U energiaclara -d energiaclara -f database/schema.sql
```

> **Importante:** Antes de ejecutar el schema, reemplazar el hash falso del seed.
> Ir a [bcrypt-generator.com](https://bcrypt-generator.com), rounds = 10, password = `Admin1234!`, copiar el resultado y pegarlo en el INSERT de `schema.sql`.

---

## 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Corre en `http://localhost:8080`

Variables de entorno opcionales (tienen defaults para desarrollo):

| Variable | Default | Descripción |
|---|---|---|
| `DB_USERNAME` | `energiaclara` | Usuario PostgreSQL |
| `DB_PASSWORD` | `energiaclara` | Contraseña PostgreSQL |
| `JWT_SECRET` | *(ver application.yml)* | Clave JWT — **cambiar en producción** |

---

## 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Corre en `http://localhost:5173`

El frontend hace proxy automático de `/api` → `http://localhost:8080`.

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
│       ├── infrastructure/         # JPA, JWT, Spring Security
│       ├── api/                    # REST controllers, DTOs, exception handler
│       └── bootstrap/              # Main class
├── frontend/                       # React 18 + Vite
│   └── src/
│       ├── context/                # AuthContext (JWT en localStorage)
│       ├── services/               # Axios + interceptor de token
│       ├── components/             # ProtectedRoute
│       └── pages/                  # LoginPage, DashboardPage
└── database/
    └── schema.sql                  # Schema + seed de tenant y admin demo
```

---

## Endpoints disponibles (MVP)

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
    "password": "Tecnico1234!",
    "roles": ["TECNICO"]
  }'
```

---

## Roles disponibles

| Rol | Nivel | Descripción |
|---|---|---|
| `ADMIN_INSTITUCION` | Estratégico | Configura tarifas, crea usuarios |
| `DIRECTOR` | Estratégico | Supervisa KPIs, aprueba campañas |
| `DOCENTE` | Táctico | Crea retos educativos |
| `TECNICO` | Operativo | Atiende tickets de mantenimiento |
| `ESTUDIANTE` | Operativo | Participa en retos |
| `AUDITOR` | Táctico | Revisa anomalías y logs |
