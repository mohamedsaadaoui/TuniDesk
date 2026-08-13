# TuniDesk — HelpDesk / IT Service Management Platform

[![CI](https://github.com/mohamedsaadaoui/TuniDesk/actions/workflows/ci.yml/badge.svg)](https://github.com/mohamedsaadaoui/TuniDesk/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

TuniDesk is a full-stack helpdesk platform: a Spring Boot REST API plus a React frontend for managing IT helpdesk tickets — ticket lifecycle, assignment, SLA tracking with breach monitoring, real-time notifications, audit logs, and dashboards.

## Tech Stack 

### Backend
- Java 21, Spring Boot 3.5
- Spring Security + JWT authentication
- Spring Data JPA, PostgreSQL
- MapStruct, Lombok, Validation
- WebSocket / STOMP for real-time notifications
- SpringDoc OpenAPI (Swagger UI)

### Frontend
- React 19, Vite, TypeScript
- React Router, Axios
- Recharts dashboard (status, priority & category analytics)
- STOMP (via `@stomp/stompjs`) + SockJS for live notifications
- Custom CSS design system (no UI library)

## Features

- **Authentication & Authorization** — register, login, JWT tokens; roles `ADMIN`, `AGENT`, `USER`
- **Tickets** — create, update, detail, list with filters (search, status, priority, category, assignee, SLA) and pagination/sorting
- **Assignment** — assign a ticket to an agent, or take ownership
- **Status workflow** — guarded transitions (open → in progress → resolved → closed)
- **Comments & History** — ticket comments plus a full change history per ticket
- **SLA** — per-priority resolution deadlines, approaching/breach detection by scheduled task (`0 */5 * * * *`), notifications to assignee and creator
- **Dashboard** — summary endpoint with counts by status/priority/category, SLA breaches, average resolution time, and recent tickets
- **Notifications** — in-app notifications with unread counts, persisted and pushed in real time via WebSocket
- **Categories, SLA policies & Users** — full CRUD for admins
- **Audit log** — records create/update/delete/login/register actions

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL (or run it via Docker)

### 1. Start the database

```bash
docker compose up -d
```

### 2. Configure

The `dev` profile is active by default (`application-dev.properties`):

```
spring.datasource.url=jdbc:postgresql://localhost:5432/tunidesk
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Run

Backend:

```bash
./mvnw spring-boot:run
```

Frontend (new terminal):

```bash
cd frontend
npm install
npm run dev
```

The backend starts on `http://localhost:8080` (Swagger UI: http://localhost:8080/swagger-ui.html).
The frontend starts on `http://localhost:5173` and proxies `/api` and `/ws` to the backend.

On first startup, `DataInitializer` seeds roles, demo users, categories and SLA policies.

### Default users

| Role  | Email               | Password   |
|-------|---------------------|------------|
| ADMIN | admin@tunidesk.com  | Admin@123  |
| AGENT | agent@tunidesk.com  | Agent@123  |
| USER  | user@tunidesk.com   | User@123   |

## API Overview

| Method | Endpoint                          | Description                                  |
|--------|-----------------------------------|----------------------------------------------|
| POST   | `/api/auth/register`              | Register a new user                          |
| POST   | `/api/auth/login`                 | Login, returns JWT                           |
| GET    | `/api/dashboard/summary`          | Dashboard statistics                         |
| GET/POST/PUT/DELETE | `/api/tickets`       | Ticket CRUD, list with filters               |
| POST   | `/api/tickets/{id}/assign`        | Assign ticket to an agent                    |
| POST   | `/api/tickets/{id}/take`          | Agent takes the ticket                       |
| POST   | `/api/tickets/{id}/status`        | Change ticket status                         |
| POST   | `/api/tickets/{id}/comments`      | Add a comment                                |
| GET    | `/api/categories/**`              | Ticket categories                            |
| GET/POST/PUT/DELETE | `/api/sla/**`       | SLA policies                                 |
| GET    | `/api/notifications/**`           | Notifications, mark read                     |
| GET    | `/api/users/**`                   | User management (ADMIN)                      |
| GET    | `/api/audit-logs/**`              | Audit logs (ADMIN)                           |

## WebSocket

Connect to `/ws` (SockJS/STOMP). Notifications are delivered to `/user/{userId}/queue/notifications`.

## Testing

The backend ships with 44 unit + integration tests (JUnit 5, Mockito, H2 for integration):

- **Unit tests** — services (tickets, SLA, users) with mocked repositories
- **Integration tests** — full API flows (auth, profiles, tickets) against an embedded H2 database

Run the full suite with coverage:

```bash
./mvnw verify
```

Coverage is enforced with **JaCoCo** (≥50% instruction coverage gate) and the report is generated at `target/site/jacoco/index.html`. Health is exposed via Spring Boot **Actuator** at `/actuator/health`.

## Project Structure

```
com.saadaoui.tunidesk
├── auth          # security config, JWT, authentication
├── audit         # audit log recording & queries
├── category      # ticket categories
├── config        # OpenAPI, WebSocket, data initializer
├── dashboard     # dashboard summary service
├── entity        # JPA entities
├── exception     # error handling
├── notification  # notifications + realtime push
├── sla           # SLA policies, scheduler, utilities
├── ticket        # ticket module (controller/service/mapper)
└── user          # user management
```

## Docker

Run the full stack (PostgreSQL + backend + frontend) in one command:

```bash
docker compose up -d --build
```

- Backend: http://localhost:8080 (Swagger UI: http://localhost:8080/swagger-ui.html)
- Frontend (nginx): http://localhost:5173

The frontend nginx container proxies `/api` and `/ws` to the backend, and SPA routes fall back to `index.html`. Override the published ports with `BACKEND_PORT` / `FRONTEND_PORT`, and set a production `JWT_SECRET` (valid Base64, ≥256 bits) in your environment. For local development, `docker compose up -d` alone still works — it starts only PostgreSQL so you can run the backend and frontend with hot reload.

## CI/CD

`.github/workflows/ci.yml` runs on every push/PR to `main`:

- Backend: `./mvnw verify` (tests + JaCoCo gate)
- Frontend: `npm ci && npm run build`
- Docker: builds and pushes backend & frontend images to GHCR on `main`
- Security: CodeQL static analysis (Java)
- Dependabot keeps Maven, npm, Docker and GitHub Actions dependencies up to date

## License

TuniDesk is an open-source project under the MIT License.
