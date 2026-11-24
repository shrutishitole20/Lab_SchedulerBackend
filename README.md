# 🏥 Lab Sample Scheduler

A high-performance laboratory sample scheduling and SLA monitoring system built with a **Spring Boot 3.3.5 REST Backend** and a decoupled **Next.js 14 (React 18) Frontend**, backed by **MySQL 8.0**.

---

## 🏗️ Architecture Overview

The system is split into two **completely independent** applications communicating through RESTful HTTP APIs with Cross-Origin Resource Sharing (CORS) configured:

```text
lab-scheduler/
│
├── backend/                                  # Spring Boot REST API Service (Port 8080)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lab/                 # Controllers, Services, DAOs, Schedulers
│   │   │   └── resources/
│   │   │       ├── application.properties    # Server port, datasource, CORS config
│   │   │       ├── config.properties         # Local DB credentials (git-ignored)
│   │   │       ├── config.properties.example # Credentials template
│   │   │       └── .env.example              # Backend environment variable template
│   │   └── test/                             # 9 JUnit 5 & Security test classes
│   ├── pom.xml                               # Backend Maven POM
│   ├── mvnw / mvnw.cmd                       # Maven wrappers
│   └── README.md                             # Backend documentation
│
├── frontend/                                 # Next.js 14 (React 18) Web Application (Port 3000)
│   ├── app/                                  # Next.js App Router (layout, page, styles)
│   ├── components/                           # Modular UI Components (Navbar, Tabs, Modals)
│   ├── lib/                                  # Centralized API client & date formatting
│   ├── static-archive/                       # Archive of original static HTML/CSS/JS
│   ├── .env.local                            # Frontend environment variables (git-ignored)
│   ├── .env.local.example                    # Frontend environment variable template
│   ├── package.json                          # Next.js, React, Bootstrap dependencies
│   └── README.md                             # Frontend documentation
│
├── database/
│   └── schema.sql                            # MySQL 8.0 schema DDL & seed data
│
├── docs/                                     # System documentation
│   ├── DEMO_SCRIPT.md                        # Step-by-step evaluation guide
│   ├── EXPLANATION.md                        # Design & scheduling algorithm details
│   └── TEST_CASES.md                         # Edge cases & functional test cases
│
├── Lab_Scheduler_APIs.postman_collection.json# Postman REST API test collection
├── .gitignore                                # Excludes build targets and credentials
└── README.md                                 # Project overview & quick-start guide
```

---

## 🛠️ Technologies Used

| Domain | Technology | Details |
| :--- | :--- | :--- |
| **Frontend Framework** | Next.js 14, React 18 | App Router, Client Components, Fast Refresh |
| **Frontend UI** | Bootstrap 5.3.3 & Icons | Responsive grid, cards, modals, SLA indicators |
| **Backend Framework** | Spring Boot 3.3.5 | Spring MVC REST, Spring Security, Validation |
| **Language & Runtime** | Java 17 (LTS), Node.js 22 | Strict compile targets & modern JS/React |
| **Persistence / DB** | MySQL 8.0+ | Direct JDBC with `PreparedStatement` & Connection Pool |
| **API Architecture** | RESTful HTTP + CORS | Centralized client, JSON payloads, Basic Auth |

---

## 🗄️ Database Setup

1. **Prerequisites**: Ensure MySQL 8.0+ service is active on `localhost:3306`.
2. **Execute the Database Schema**:
   ```sql
   source database/schema.sql;
   ```
   This creates the `lab_scheduler` database, all 7 tables, constraints, foreign keys, and seeds baseline data.

3. **Configure Database Credentials**:
   In `backend/src/main/resources/config.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/lab_scheduler?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=YOUR_MYSQL_ROOT_PASSWORD
   ```

---

## 🚀 Running the Applications Independently

Both applications run independently in separate terminals. **Start the backend first.**

### 1. Start the Backend (Port 8080)

Open a terminal in the `backend/` folder:

**Windows:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
cd backend
./mvnw spring-boot:run
```

Backend REST API available at → **http://localhost:8080**

---

### 2. Start the Frontend (Port 3000)

Open a **separate terminal** in the `frontend/` folder:

```powershell
cd frontend
npm run dev
```

Frontend application available at → **http://localhost:3000**

> The frontend reads the backend URL from `.env.local`. The default value is `http://localhost:8080`.
> To use a different backend URL, edit `frontend/.env.local`:
> ```env
> NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
> ```

---

## ⚙️ Environment Variables

### Backend (`backend/`)

| Variable | Default | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP port the Spring Boot server listens on |
| `DB_URL` | `jdbc:mysql://localhost:3306/lab_scheduler...` | Full JDBC connection URL |
| `DB_USER` | `root` | MySQL username |
| `DB_PASSWORD` | _(empty)_ | MySQL password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000,...` | Comma-separated list of allowed frontend origins |
| `ADMIN_PASSWORD` | `admin123` | In-memory admin fallback password (DB unavailable) |
| `STAFF_PASSWORD` | `staff123` | In-memory staff fallback password (DB unavailable) |

See `backend/.env.example` for a ready-to-use template.

### Frontend (`frontend/`)

| Variable | Default | Description |
| :--- | :--- | :--- |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Base URL of the Spring Boot REST API |

See `frontend/.env.local.example` for a ready-to-use template.

---

## 🔗 Communication & CORS

- **Frontend** communicates with the backend via `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`).
- **Backend** CORS allows `http://localhost:3000` (and `http://127.0.0.1:3000`) by default.
- To allow additional origins, set `CORS_ALLOWED_ORIGINS` on the backend:
  ```bash
  # Windows PowerShell
  $env:CORS_ALLOWED_ORIGINS="http://localhost:3000,https://myapp.example.com"
  .\mvnw.cmd spring-boot:run

  # Linux / macOS
  CORS_ALLOWED_ORIGINS="http://localhost:3000,https://myapp.example.com" ./mvnw spring-boot:run
  ```

---

## 🔑 Default Sign-in Credentials

| Role | Username | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` | `admin123` | Full access: Equipment, User security, Priority SLAs, Diagnostic categories |
| **Lab Staff** | `staff` | `staff123` | Daily operations: Register samples, view queue, hold/release/complete samples |
