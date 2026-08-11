# TuniDesk — HelpDesk / IT Service Management Platform

TuniDesk is a Spring Boot REST API for managing IT helpdesk tickets: ticket lifecycle, assignment, SLA tracking with breach monitoring, notifications, audit logs, and dashboards.

## Tech Stack

- Java 21, Spring Boot 3.5
- Spring Security + JWT authentication
- Spring Data JPA, PostgreSQL
- MapStruct, Lombok, Validation
- WebSocket / STOMP for real-time notifications
- SpringDoc OpenAPI (Swagger UI)

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

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

Swagger UI: http://localhost:8080/swagger-ui.html

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

## License

TuniDesk is an open-source project under the MIT License.
