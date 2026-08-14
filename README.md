# 📚 The Online Bookshop — Full-Stack Home Assignment

> **⚠️ DISCLAIMER — HOME ASSIGNMENT PROJECT**
> This repository was built as a **take-home interview assignment**. It is intentionally
> small in scope, and payment and shipping are mocked by design.
> See [docs/DISCLAIMER.md](docs/DISCLAIMER.md) for details.

A full-stack online bookshop built as a **REST API web application**: a React single-page
client talks to a stateless RESTful JSON API backed by PostgreSQL. Browse a paginated
catalog, create an account, sign in, manage a cart, and complete a multi-step (mock)
checkout — with light/dark theming and toast feedback. Tokens are issued by a
self-hosted OAuth2 authorization server embedded in the backend.

## Live application

| Piece      | Technology                              | Hosted on                     | URL |
|------------|-----------------------------------------|-------------------------------|-----|
| Frontend   | React 18 + TypeScript 5.6 (Vite, Zustand) | Vercel (free tier)            | _fill in after deploy_ |
| Backend    | Java 21, Spring Boot 3 (Spring Web MVC REST API) | Render free web service (Docker) | _fill in after deploy_ |
| Database   | PostgreSQL 16                           | Neon (free serverless Postgres) | — |
| Cache      | Redis 7 (catalog read-cache, optional)  | Docker locally / any Redis URL  | — |

Architecture, decisions and deployment topology: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Security posture (SQL injection, XSS, CSRF, IDOR, rate limiting…): [docs/SECURITY.md](docs/SECURITY.md).
API endpoint hierarchy at a glance: [API-Reference.pdf](API-Reference.pdf).

## Features

- **Catalog** — paginated book list with search (title/author) and sorting; nullable
  cover images fall back to a generated cover. Sold-out books stay visible with an
  **OUT OF STOCK** badge and a disabled add-to-cart button. Reads are served through a
  Redis cache for fast responses.
- **Accounts & tokens** — sign-in and registration happen in the app's own UI. Passwords
  are BCrypt-hashed (cost 12); the API returns an RS256 access token plus a rotating
  refresh token, issued and signed by the **self-hosted OAuth2 authorization server**
  embedded in this backend (Spring Authorization Server — no third-party or paid identity
  provider). Renewal rotates the refresh token via `POST /api/auth/refresh`, and the standards-compliant
  Authorization Code + PKCE endpoints remain available for any non-first-party client. **Email is the login identity**: each user has exactly one email and one
  password, so email is `UNIQUE` in the database. Full names are validated (two words,
  letters only, single space) and reused as the shipping recipient at checkout.
- **Cart** — add / remove / change quantity, persisted server-side per user, with
  **optimistic UI updates** (instant feedback, automatic rollback on stock errors).
- **Checkout** — multi-step flow (shipping → payment → confirmation). Payment is
  **mocked**: nothing is validated, charged, or stored; the API generates a
  `PAY-MOCK-xxxxxxxx` reference. Orders snapshot title + unit price at purchase time.
- **Order history** — per-user list of past orders, with a 24-hour cancellation window
  (bin button) that restores stock and keeps the order as CANCELLED for audit.
- **UI** — liquid-glass design with an iOS-style light/dark switch, green/red/orange
  toast notifications, and a rainbow confetti celebration when an order completes.
- **No overselling** — stock is validated in the UI, again in the service layer, and
  finally under `SELECT … FOR UPDATE` row locks at checkout, so concurrent buyers can
  never take the same last copy.

## Repository layout

```
backend/    Spring Boot 3 REST API (Spring Web MVC; Controller → Service → Repository, DTOs, Flyway)
            plus a self-hosted OAuth2 authorization server (Spring Authorization Server)
frontend/   React + TypeScript SPA (Vite, Zustand stores, React Router)
docs/       ARCHITECTURE.md · SECURITY.md · DISCLAIMER.md
render.yaml Render blueprint for the backend service
.github/workflows/keep-warm.yml  Pings the API every 10 min so the free tier stays warm
```

## Running locally

Prerequisites: Java 21, Maven 3.9+, Node 20+, Docker (for Postgres).

```bash
# 1. Database + cache
docker compose up -d postgres redis   # Postgres on :5432, Redis on :6379

# 2. Backend  (http://localhost:8080)
cd backend
mvn spring-boot:run           # Flyway creates the schema and seeds 20 books

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Alternatively, `docker compose up --build` runs PostgreSQL, Redis and the backend in
Docker (no local Maven needed) — then only the frontend runs via npm. Redis is
optional: if it isn't reachable, the API logs a warning and serves from PostgreSQL.

## API overview

| Method & path                  | Auth | Description |
|--------------------------------|------|-------------|
| `GET  /api/books`              | –    | Paginated catalog (`page,size,search,sort,direction`) |
| `GET  /api/books/{id}`         | –    | Single book |
| `POST /api/auth/register`      | –    | Create account → tokens |
| `POST /api/auth/login`         | –    | Sign in → tokens |
| `POST /api/auth/refresh`       | –    | Rotate refresh token → new token pair |
| `GET  /api/auth/me`            | 🔒   | Current user profile |
| `GET  /api/cart`               | 🔒   | Current cart |
| `POST /api/cart/items`         | 🔒   | Add book to cart |
| `PUT  /api/cart/items/{bookId}`| 🔒   | Change quantity |
| `DELETE /api/cart/items/{bookId}` | 🔒 | Remove from cart |
| `POST /api/orders`             | 🔒   | Checkout (mock payment) |
| `GET  /api/orders`             | 🔒   | Order history |
| `DELETE /api/orders/{id}`      | 🔒   | Cancel an order (within 24h; restores stock) |
| `GET  /api/health`             | –    | Health/keep-warm endpoint |

🔒 = requires `Authorization: Bearer <access token>`.

OAuth2 endpoints (standard, served by the same application):

| Endpoint | Purpose |
|----------|---------|
| `POST /oauth2/token`     | Authorization-code→token exchange |
| `GET  /oauth2/authorize` | Authorization Code + PKCE endpoint (for non-first-party clients) |
| `GET  /oauth2/jwks`      | Public keys used to verify access tokens |
| `GET  /.well-known/oauth-authorization-server` | Discovery document |

## Mocked functionality & assumptions

- **Payment** — mocked, **and intended to stay mocked in production**. This is a
  product decision rather than a shortcut: the card field is decorative, the server
  generates a `PAY-MOCK-…` reference, and no card data is ever collected, validated,
  stored, or transmitted — which keeps the service entirely out of PCI-DSS scope.
- **Shipping** — mocked. The recipient is the account's full name; only the address is
  collected. Nothing ships.
- **Authentication** — email/password only (mock-free, fully functional). The `users`
  table keeps multi-provider columns (`auth_provider`, `provider_id`) for forward
  compatibility, but social login is not part of the application.
- **Tokens** — access tokens last 24 hours and are refreshed transparently; refresh tokens
  last 7 days and rotate on every use. The RSA signing key is generated at startup and
  never persisted, so restarting the backend requires signing in again.
- **Security** — no SQL is assembled from strings, output is escaped, a strict CSP and
  HSTS are sent on every response, credential endpoints are rate limited, and every
  user-scoped query filters by the authenticated user. See [docs/SECURITY.md](docs/SECURITY.md).
- Stock is decremented at checkout inside a single DB transaction (and restored on
  cancellation); carts are server-side so they survive devices/sessions.
