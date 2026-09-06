## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Build & verify

- Backend is Spring Boot 3.5 / Java 21 + Maven (wrapper). On Windows run `.\mvnw.cmd`, never `./mvnw`. CI runs `./mvnw -B verify`.
- Full verification: `.\mvnw.cmd verify` — runs all 51 tests AND the JaCoCo gate (>=50% instruction coverage). Tests alone: `.\mvnw.cmd test`.
- Single test (note: no `test` goal is required before it on this wrapper): `.\mvnw.cmd -Dtest=TicketServiceImplTest test` or `-Dtest=TicketServiceImplTest#method`.
- If `mvnw test` throws "Unresolved compilation problems: ... builder()/getId() undefined" at runtime, it's a stale incremental `target/` — run `.\mvnw.cmd clean test` before suspecting real code issues.
- Frontend (React 19 + Vite) in `frontend/` has NO test or lint script. The only checks are `npm run build` (tsc -b + vite build) and actual browser use. Dev server proxies `/api` and `/ws` to backend :8080.

## Profiles & config

- `spring.profiles.active=dev` is the default (set in base application.properties, overridable via env). Backend auth JWT secret lives per-profile: dev+test use the committed dev secret, prod reads `${JWT_SECRET}`.
- Seeding is tri-state via `application.seed-enabled` (dev/test `true`, prod `${SEED_ENABLED:false}`). Integration tests (AuthApiIntegrationTest, ProfileApiIntegrationTest, TicketApiIntegrationTest) REQUIRE seeded users (`admin@tunidesk.com` / `Admin@123` etc.) — break seeding in test profile only if you update those tests too.
- Prod compose requires envs: `JWT_SECRET` (base64 >= 256 bits), `APP_BASE_URL`; `CORS_ALLOWED_ORIGINS` defaults to `APP_BASE_URL`. `JwtService` has a `@PostConstruct` guard that fails startup on missing/short secrets. For local backend dev, `docker compose up -d` starts PostgreSQL only.
- CORS: a single `application.cors.allowed-origins` property (comma-separated) feeds BOTH the REST `CorsConfigurationSource` (CorsConfig) and the WebSocket `setAllowedOriginPatterns` (WebSocketConfig). If one forgets the other, WS handshakes fail while REST looks fine.

## Architecture notes

- Domain entities are split: shared/tenant data (Ticket, TicketCategory, SlaPolicy, TicketComment, TicketHistory, TicketAttachment, Notification) live in `entity/` at the root, NOT inside `ticket/` or `sla/`. Security entities (User, Role, PasswordResetToken) live in `auth/entity/`. Grep for a class before assuming its package from the folder it's used in.
- Contained domain seams: `ticket/`, `sla/`, `auth/`, `notification/`, `audit/`, `dashboard/` each keep their own dto/entity(only auth)/mapper/service/controller. Shared entities and config live at root.
- Frontend talks to the backend only through `/api` (REST) and `/ws` (STOMP over SockJS); JWT lives in localStorage with a 24h expiration (86400000ms). No refresh-token flow yet: expiry = re-login.