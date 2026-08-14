# 📚 The Online Bookshop — Full-Stack Home Assignment

> **⚠️ DISCLAIMER — HOME ASSIGNMENT PROJECT**
> This repository was built as a **take-home interview assignment**. It is intentionally
> simple, is **not production-ready**, and mocks payment and shipping by design.
> See [DISCLAIMER.md](DISCLAIMER.md) for details.

A simple full-stack online bookshop: browse a paginated book catalog, create an account
(email/password **or** Google / Facebook sign-in), manage a cart, and complete a
multi-step (mock) checkout.

## Live application

| Piece      | Technology                              | Hosted on                     | URL |
|------------|-----------------------------------------|-------------------------------|-----|
| Frontend   | React 18 + TypeScript (Vite, Zustand)   | Vercel (free tier)            | _fill in after deploy_ |
| Backend    | Java 21, Spring Boot 3 (REST API)       | Render free web service (Docker) | _fill in after deploy_ |
| Database   | PostgreSQL 16                           | Neon (free serverless Postgres) | — |

Deployment steps: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).
Architecture rationale: [ARCHITECTURE.txt](ARCHITECTURE.txt).

## Features

- **Catalog** — paginated book list with search (title/author) and sorting; nullable
  cover images fall back to a generated cover.
- **Accounts** — email/password registration and login (BCrypt-hashed), plus
  **hand-rolled OAuth2 sign-in with Google and Facebook** (Spring Security
  `oauth2-client`, no managed-auth SaaS). The API issues its own stateless JWT either way.
- **Cart** — add / remove / change quantity, persisted server-side per user, with
  **optimistic UI updates** (instant feedback, automatic rollback on stock errors).
- **Checkout** — multi-step flow (shipping → payment → confirmation). Payment is
  **mocked**: nothing is validated, charged, or stored; the API generates a
  `PAY-MOCK-xxxxxxxx` reference. Orders snapshot title + unit price at purchase time.
- **Order history** — per-user list of past orders.

## Repository layout

```
backend/    Spring Boot 3 REST API (Controller → Service → Repository, DTOs, Flyway)
frontend/   React + TypeScript SPA (Vite, Zustand stores, React Router)
docs/       Deployment guide (Vercel + Render + Neon + OAuth app setup)
render.yaml Render blueprint for the backend service
.github/workflows/keep-warm.yml  Pings the API every 10 min so the free tier stays warm
```

## Running locally

Prerequisites: Java 21, Maven 3.9+, Node 20+, Docker (for Postgres).

```bash
# 1. Database
docker compose up -d          # Postgres 16 on localhost:5432 (bookshop/bookshop)

# 2. Backend  (http://localhost:8080)
cd backend
mvn spring-boot:run           # Flyway creates the schema and seeds 20 books

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Social login buttons appear only when provider credentials are configured
(`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`, `FACEBOOK_CLIENT_ID`/`FACEBOOK_CLIENT_SECRET`
environment variables — see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#5-social-sign-in-google--facebook)).
Everything else works without them.

## API overview

| Method & path                  | Auth | Description |
|--------------------------------|------|-------------|
| `GET  /api/books`              | –    | Paginated catalog (`page,size,search,sort,direction`) |
| `GET  /api/books/{id}`         | –    | Single book |
| `POST /api/auth/register`      | –    | Create local account → JWT |
| `POST /api/auth/login`         | –    | Local sign-in → JWT |
| `GET  /api/auth/me`            | JWT  | Current user profile |
| `GET  /api/auth/providers`     | –    | Which social providers are configured |
| `GET  /oauth2/authorization/{google\|facebook}` | – | Starts the OAuth2 handshake |
| `GET  /api/cart`               | JWT  | Current cart |
| `POST /api/cart/items`         | JWT  | Add book to cart |
| `PUT  /api/cart/items/{bookId}`| JWT  | Change quantity |
| `DELETE /api/cart/items/{bookId}` | JWT | Remove from cart |
| `POST /api/orders`             | JWT  | Checkout (mock payment) |
| `GET  /api/orders`             | JWT  | Order history |
| `GET  /api/health`             | –    | Health/keep-warm endpoint |

## Mocked functionality & assumptions

- **Payment** — mocked. The card field is decorative; the server generates a fake
  payment reference. No card data is validated, stored, or transmitted.
- **Shipping** — mocked. Name + address are stored on the order; nothing ships.
- **Facebook review caveat** — a Facebook app in Dev Mode only authenticates its
  admins/testers. Reviewers should use Google sign-in or email/password;
  Facebook works for accounts added as testers (documented in the deployment guide).
- **JWT lifetime** is 7 days to cover the review window without re-login friction.
- Stock is decremented at checkout inside a single DB transaction; carts are
  server-side so they survive devices/sessions.
