# 🏥 Lab Scheduler - Backend REST Service

High-performance laboratory sample scheduling and SLA monitoring REST API service built with **Java 17**, **Spring Boot 3.3.5**, **MySQL 8.0**, and **Spring Security**.

Runs independently on **port 8080** and can be started without the frontend.

---

## 📁 Folder Structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/lab/
│   │   │   ├── config/             # Spring Security & CORS configuration
│   │   │   ├── controller/         # REST API endpoints
│   │   │   ├── dao/                # Direct JDBC data access objects
│   │   │   ├── dto/                # Request & response data transfer objects
│   │   │   ├── enums/              # Priority, Status, DelayStatus enums
│   │   │   ├── exception/          # Custom exceptions & global exception handler
│   │   │   ├── model/              # Domain entities
│   │   │   ├── repository/         # Data repositories & validation
│   │   │   ├── scheduler/          # 4-tier priority comparator & scheduling engine
│   │   │   ├── service/            # Core business logic services
│   │   │   ├── util/               # DB connection & date/time utilities
│   │   │   ├── LabSchedulerApplication.java # Spring Boot entrypoint
│   │   │   └── Main.java           # Standalone CLI demo runner
│   │   └── resources/
│   │       ├── application.properties    # Spring Boot environment properties
│   │       ├── config.properties         # Local DB credentials (git-ignored)
│   │       └── config.properties.example # DB configuration template
│   └── test/
│       └── java/com/lab/...        # Unit and integration test suite
├── .env.example                    # Environment variable template
├── pom.xml                         # Maven dependencies and build plugins
├── mvnw                            # Maven wrapper script (Unix/macOS)
├── mvnw.cmd                        # Maven wrapper script (Windows)
└── .mvn/                           # Maven wrapper binaries and settings
```

---

## ⚙️ Configuration & Prerequisites

### 1. Database
1. Ensure MySQL 8.0+ is running locally on port `3306`.
2. Execute the root schema script:
   ```sql
   source ../database/schema.sql;
   ```
3. Set your credentials in `src/main/resources/config.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/lab_scheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD
   ```
   > **Tip**: Use `config.properties.example` as the starting template.

### 2. Environment Variables

All configuration has safe defaults and can be overridden with environment variables:

| Variable | Default | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP port the backend listens on |
| `DB_URL` | `jdbc:mysql://localhost:3306/lab_scheduler...` | Full JDBC connection string |
| `DB_USER` | `root` | MySQL username |
| `DB_PASSWORD` | _(empty)_ | MySQL password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000,...` | Comma-separated list of allowed frontend origins |
| `ADMIN_PASSWORD` | `admin123` | In-memory admin fallback password |
| `STAFF_PASSWORD` | `staff123` | In-memory staff fallback password |

See `.env.example` for a ready-to-use template.

### 3. CORS (Cross-Origin Resource Sharing)

CORS is configured in `SecurityConfig.java` and driven by the `cors.allowed.origins` property (set via `CORS_ALLOWED_ORIGINS`).

**Default allowed origins:**
- `http://localhost:3000` (Next.js dev server)
- `http://127.0.0.1:3000`
- `http://localhost:5500` (VS Code Live Server)
- `http://127.0.0.1:5500`

**To allow a custom origin at runtime:**

```powershell
# Windows PowerShell
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,https://myapp.example.com"
.\mvnw.cmd spring-boot:run

# Linux / macOS
CORS_ALLOWED_ORIGINS="http://localhost:3000,https://myapp.example.com" ./mvnw spring-boot:run
```

---

## 🚀 How to Run the Backend

The backend runs **independently** — no frontend needed.

### Windows PowerShell:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Linux / macOS:
```bash
cd backend
./mvnw spring-boot:run
```

### Custom Port:
```powershell
# Windows
$env:PORT=9090; .\mvnw.cmd spring-boot:run

# Linux / macOS
PORT=9090 ./mvnw spring-boot:run
```

The REST API service starts at:
```
http://localhost:8080
```

---

## 🧪 Running Automated Tests

```powershell
cd backend
.\mvnw.cmd test
```

---

## 🔐 REST API Summary

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | Public | Authenticates credentials (`admin`/`admin123` or `staff`/`staff123`) |
| `/api/auth/me` | `GET` | Authenticated | Current user profile |
| `/api/machines` | `GET` | Staff / Admin | Lists all machines with live workload |
| `/api/machines/{id}/down` | `POST` | Staff / Admin | Marks machine DOWN for maintenance |
| `/api/machines/{id}/up` | `POST` | Staff / Admin | Marks machine UP / Available |
| `/api/machines` | `POST` | Admin Only | Registers new machine |
| `/api/machines/{id}` | `DELETE` | Admin Only | Deletes machine |
| `/api/queue` | `GET` | Staff / Admin | Priority queue sorted by 4-tier algorithm |
| `/api/queue/dispatch` | `POST` | Staff / Admin | Dispatches eligible queued samples to machines |
| `/api/samples` | `GET` | Staff / Admin | Lists all samples (optional status filter) |
| `/api/samples` | `POST` | Staff / Admin | Registers a new diagnostic sample |
| `/api/samples/{id}/complete` | `POST` | Staff / Admin | Marks sample completed and frees machine slot |
| `/api/samples/{id}/hold` | `POST` | Staff / Admin | Places sample on hold |
| `/api/samples/{id}/release` | `POST` | Staff / Admin | Releases sample back to priority queue |
| `/api/samples/{id}/history` | `GET` | Staff / Admin | Audit trail for a specific sample |
| `/api/reports/daily?date=` | `GET` | Staff / Admin | Daily SLA metrics and sample breakdown |
| `/api/priorities` | `GET`, `POST`, `DELETE` | Admin Only (POST/DELETE) | SLA priority tier management |
| `/api/test-types` | `GET`, `POST`, `DELETE` | Admin Only (POST/DELETE) | Diagnostic test categories |
| `/api/users` | `GET`, `POST`, `DELETE` | Admin Only | Operator user and role management |

---

## 🔑 Default Sign-in Credentials

| Role | Username | Password |
| :--- | :--- | :--- |
| **System Admin** | `admin` | `admin123` |
| **Lab Staff** | `staff` | `staff123` |
