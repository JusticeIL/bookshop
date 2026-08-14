# Architecture — The Online Bookshop

Architectural propositions: the decisions behind this build and the reasoning
for each one. **Every line in bold is a choice and why it was made.**

---

## Summary

| Question | Answer |
|----------|--------|
| **Frontend technology and hosting location** | **React 18 + TypeScript 5.6** (Vite build, Zustand state, React Router), deployed as a static SPA on **Vercel** |
| **Backend technology and hosting location** | **Java 21 + Spring Boot 3 REST API** (Spring Web MVC, Spring Security, Spring Data JPA, Flyway), containerised with a multi-stage **Docker** build and deployed as a web service on **Render** |
| **Database technology and hosting location** | **PostgreSQL 16** hosted on **Neon** (serverless Postgres, same region as the backend), with an optional **Redis** cache in front of catalog reads |
| **Mocked functionality and assumptions** | **Payment and shipping are mocked; everything else is real working functionality** |

What "mocked" means precisely:

- **Payment** — the checkout card field is decorative; nothing entered there is
  validated, stored or transmitted, and the confirmation reference
  (`PAY-MOCK-XXXXXXXX`) is generated server-side. **No card data is ever
  collected, which keeps the service out of PCI-DSS scope entirely.**
- **Shipping** — the recipient (the account's validated full name) and the
  address are recorded on the order; no fulfilment integration exists.
- **Tokens live in memory.** **The RSA signing key is generated at startup and
  never persisted or committed, so a restart signs users out — a fair trade
  against storing private key material for a shop this size.**
- **Rate-limit counters are in-process**, because one instance serves this
  deployment; a multi-instance deployment would move them into Redis.
- **Catalog data is seeded demo content** — prices and stock are not real.
  Cover images come from OpenLibrary's public cover CDN.

Accounts, authentication, catalog, cart, checkout, stock control, order history
and cancellation are all genuinely implemented.

---

## 1. Overall shape

A REST API web application: a single-page React client talks to a stateless
RESTful HTTP/JSON API — resource-oriented URLs, HTTP verbs carrying the
semantics, standard status codes — which owns the data.

```
  React + TypeScript SPA  --HTTPS/JSON-->  Spring Boot 3 REST API  --JDBC-->  PostgreSQL 16
        (Vercel)                              (Render, Docker)                  (Neon)
                                                     |
                                                     +--RESP--> Redis (catalog read-cache)
```

Three cleanly separated tiers, deployed independently, plus a cache:

- **The frontend never touches the database** — it holds no connection string
  and no SQL, so its only contact with the system is the HTTP API, and it could
  be swapped for a mobile app without the backend changing at all.
- **The backend is the single owner of every business rule** — stock
  validation, pricing, cancellation windows, authorisation — **because the
  browser is not trustworthy and nothing important may be enforced only there.**
- **The database schema is owned by versioned Flyway migrations rather than by
  the ORM** (Hibernate runs in `ddl-auto: validate`), **so the application can
  never silently alter the schema and every environment gets an identical one.**

Each tier can be replaced or scaled without touching the others.

## 2. Backend — Java 21 / Spring Boot 3 REST API (MVC)

The backend is a REST API implementing the MVC pattern, built on Spring Web
MVC (the framework's name is literal):

| Role | What plays it here |
|------|--------------------|
| **Model** | JPA entities and Spring Data repositories (state) plus the service layer (business rules) — everything that knows and changes domain state |
| **View** | The representation returned to the client: JSON rendered from DTO records. In a SPA architecture the React frontend is the rendered view; the API's JSON is the view model feeding it |
| **Controller** | `@RestController` classes — thin HTTP adapters doing binding, validation and status codes only, delegating everything to services |

**Why Java and Spring Boot: strong compile-time typing, a mature security
ecosystem, first-class PostgreSQL and Redis support, and write-once-run-anywhere
— the identical jar runs locally and inside the Render container.**

### RESTful conventions

Resources are nouns (`/api/books`, `/api/cart/items/{id}`, `/api/orders/{id}`);
the verb carries the intent — `GET` read, `POST` create, `PUT` replace, `DELETE`
remove, so cancelling an order is `DELETE` on the order resource. Responses use
standard status codes (200, 201 on create, 400 validation, 401 unauthenticated,
404, 409 conflict, 429 rate limited) with a uniform JSON error envelope.

**The API is deliberately stateless — every request carries its own bearer
token and nothing is held in a server-side session — which is what lets the
backend scale horizontally behind a load balancer.**

`API-Reference.pdf` at the repository root charts the full endpoint hierarchy.

### Layering and code structure

**Code is organised by feature with MVC layering inside each feature, rather
than by technical type, so a change to "orders" touches one package instead of
four:**

```
com.bookshop
├── auth        AuthController · AuthService · AuthDtos
├── book        BookController · BookService · BookRepository · Book · BookDto
├── cart        CartController · CartService · CartItemRepository · CartItem · CartDtos
├── order       OrderController · OrderService · OrderRepository · Order · OrderItem · OrderDtos
├── user        User · UserRepository · AuthProvider
├── security    SecurityConfig · AuthorizationServerConfig · TokenIssuer · RateLimitFilter · …
├── config      CacheConfig
└── common      PageResponse · exceptions · GlobalExceptionHandler · HealthController
```

Within every feature the chain is **Controller → Service → Repository**:

- Controllers are thin: HTTP concerns only (binding, validation, status codes).
- Services own transactions and business rules (stock checks, checkout
  atomicity). **They are written in a functional style — streams, `Optional`
  chains, `ifPresentOrElse` — which keeps the happy path readable and pushes
  failure handling to the edges instead of nesting conditionals.**
- Repositories are Spring Data JPA interfaces. **No SQL is hand-written in Java
  code, so every query is parameter-bound and injection has no surface.**
- **DTOs are Java records, so the API contract never leaks JPA entities and the
  schema can evolve without breaking clients — with immutability and exhaustive
  constructors for free.**
- **All errors funnel through one `@RestControllerAdvice` producing a single
  JSON error shape, so unexpected failures are logged server-side and answered
  generically instead of leaking stack traces to clients.**

### Caching

Redis fronts the hot catalog reads (paginated list 60 s TTL, single book 5 min
TTL) through Spring's cache abstraction, with typed JSON serializers per cache.
Every stock-changing operation — checkout, cancellation — evicts both caches.

**Why the catalog and nothing else: it is the most-requested and least-volatile
data in the app, so it gives the largest latency win for the least staleness
risk.** **Why a custom `CacheErrorHandler`: if Redis is down or was never
provisioned, requests fall through to PostgreSQL with a logged warning instead
of failing, so the cache can never take the site down.**

## 3. Authentication — self-hosted OAuth2

Authentication is run entirely in-house: the backend embeds **Spring
Authorization Server** (Apache 2.0). The same Spring Boot process plays three
roles:

| Role | Responsibility |
|------|----------------|
| **Authorization server** | Issues tokens at the standard endpoints `/oauth2/authorize`, `/oauth2/token`, `/oauth2/jwks`, `/.well-known/oauth-authorization-server` |
| **Resource server** | The REST API validates the bearer tokens it receives |
| **Client-facing app** | Serves the one sign-in page the authorization-code flow needs |

**Why self-hosted rather than a managed identity provider: no third party, no
paid tier and no external service to sign up for — the whole flow stays
inspectable inside this repository.**

**Sign-in happens in the application's own UI.** The React login form posts the
credentials to `POST /api/auth/login` over TLS, and the response carries the
tokens:

1. The SPA posts `{email, password}` to `/api/auth/login`.
2. The backend verifies the BCrypt hash and mints an RS256 access token (24 h)
   plus a rotating refresh token (7 days) — the same tokens the authorization
   server issues, signed with the same key and registered in the same
   authorization store.
3. Every REST call carries `Authorization: Bearer <access token>`; the resource
   server verifies the signature against the published JWK set.
4. On a 401 the SPA silently rotates its refresh token at
   `POST /api/auth/refresh` and replays the request once.

**Why this "first-party trusted client" shape: the shop and the identity store
are the same product owned by the same team, so there is no third party to
delegate to and a redirect dance would only add friction — while the full
Authorization Code + PKCE endpoints stay live and standards-compliant for any
client that genuinely is separate from us.**

**Why bearer tokens rather than cookies: a header cannot be attached
automatically by an attacker's page, which removes the CSRF surface from the
API entirely** — hence CSRF protection is disabled on `/api/**` and left
enabled on the session-backed sign-in page, where it matters.

Injection, XSS, CSRF, IDOR, clickjacking, enumeration, brute force and
information disclosure are addressed explicitly; `docs/SECURITY.md` documents
each defence and where it lives in the code. In short: no SQL is ever assembled
from strings (every query is parameter-bound, and the one input that cannot be
bound — the sort column — is allow-listed); React escapes all output and a
strict CSP is sent on every response; and every user-scoped query filters by
the token's `uid`, so one customer cannot reach another's cart or orders.

## 4. Database design — PostgreSQL

**Why a relational engine: the domain is inherently relational — users own
carts, carts reference books, orders own line items — and checkout needs ACID
guarantees, since decrementing stock, snapshotting prices, creating an order and
emptying the cart must either all happen or none of them.** A document store
would make that atomicity the application's problem. PostgreSQL also brings
exact decimal arithmetic for money, rich constraints and row-level locking, all
of which this design leans on.

### Tables

| Table | Columns and intent |
|-------|--------------------|
| `users` | `id`, `email` (**UNIQUE**), `display_name`, `password_hash`, `auth_provider`, `provider_id`, `created_at` |
| `books` | `id`, `title`, `author`, `description` (**required**), `pages`, `image_url` (**nullable by requirement**), `price NUMERIC(10,2)`, `stock`, `created_at` |
| `cart_items` | One row per `(user, book)` — `UNIQUE` constraint, `quantity > 0` CHECK. **The active cart lives server-side, so it survives devices and sessions** |
| `orders` | `user_id`, `status` CHECK IN (`CONFIRMED`/`SHIPPED`/`CANCELLED`), `total_amount`, shipping fields, `payment_reference` (mock), `created_at` |
| `order_items` | **Immutable snapshot** of `title` + `unit_price` + `quantity` at purchase time |

**Email is the login identity.** Every user has exactly one email and exactly
one password, and the email is what they sign in with — **so email carries a
`UNIQUE` constraint at the database level, not merely a service-layer check.**
Consequences:

- registration rejects an already-registered email;
- a lookup by email always resolves to at most one row, which is what makes
  "find user, then verify password hash" a correct and unambiguous
  authentication step;
- the surrogate key (`id`) remains the stable foreign-key target, so a user
  could change their email later without breaking their carts and orders;
- lookups are case-insensitive and emails are normalised to lower case on
  registration, so `Gal@x.com` and `gal@x.com` cannot become two accounts.

`display_name` holds the validated full name (two or more names separated by
spaces, letters and hyphens only) and is reused as the shipping recipient at
checkout.

**Why `order_items` snapshot the title and unit price instead of joining back
to `books`: later catalog edits must never rewrite what a customer was
charged.** **Why cancelling flips a status rather than deleting rows: history
and auditability survive.**

**Money is `NUMERIC(10,2)`, never floating point, because binary floats cannot
represent decimal currency exactly.** **All constraints — CHECKs, UNIQUEs,
foreign keys — live in the database rather than only in application code,
because the database is the last line of defence.**

### Stock safety — no overselling, in three layers

1. **UI** — a book with `stock = 0` renders an **OUT OF STOCK** badge with the
   add-to-cart button disabled; the button also disables once the quantity
   already in the cart reaches available stock.
2. **Service** — every add-to-cart and quantity change validates against
   current stock and returns 400 with the exact remaining count.
3. **Database** — checkout re-reads each book with `SELECT … FOR UPDATE`
   (`@Lock(PESSIMISTIC_WRITE)`) inside the transaction, so two shoppers buying
   the last copy simultaneously serialize on the row: the first wins, the
   second is rejected. A `CHECK (stock >= 0)` constraint makes negative stock
   physically impossible even if application logic were bypassed.

**Why pessimistic row locks rather than a plain read-then-write: without them
two concurrent checkouts can both read the last copy as available and both
succeed, which is exactly how real shops oversell.**

Checkout is a single transaction: lock rows → validate stock → decrement →
snapshot prices → create order → empty cart; a failure anywhere rolls
everything back. Cancellation restores stock under the same row locks.

### Migrations

`V1__init.sql` creates the five tables with every constraint; `V2__seed_books.sql`
seeds a 20-book catalog, two of them deliberately without a cover image to
exercise the nullable-image path; `V3__add_book_description.sql` adds the
required per-book description. **Why Flyway: a brand-new empty database becomes
a working one on first boot with no manual SQL, and the migration history is
versioned in the repository alongside the code that depends on it.**

**Why V3 adds the column in three steps — nullable, backfill, then `SET NOT
NULL` — rather than declaring it `NOT NULL` outright: the live database already
holds rows, and a bare `NOT NULL` addition would fail on them.** A catch-all
`UPDATE` fills anything the per-title backfill missed, so the migration cannot
break on unexpected data.

## 5. Frontend — React 18 + TypeScript 5.6 (Vite, Zustand)

- **Why Vite: instant dev feedback and a small static production bundle that a
  CDN can serve with no server at all.**
- **Why TypeScript with an API layer mirroring the backend DTOs 1:1: a contract
  change becomes a compile error rather than a runtime surprise.**
- **Why Zustand for the three genuinely global slices (auth session, cart,
  theme): it avoids Context re-render cascades with near-zero boilerplate,
  while component-local state stays in `useState` where it belongs.**
- **Why token handling is hand-written rather than pulled from an auth library:
  it is about 80 lines — storage, transparent refresh-token rotation on a 401,
  and coalescing parallel renewals into one request — and keeping it visible is
  worth more than the dependency.**
- **Why cart operations are optimistic: the UI updates instantly and rolls back
  to the previous server state if the API rejects (for example out of stock),
  which makes the shop feel immediate without ever lying about the outcome.**
- Route guards send unauthenticated users to the sign-in page and return them
  to where they were heading once tokens are in hand.
- Feedback runs through a **toast system**: green = success, red = error
  (including "server unreachable", throttled so bursts cannot spam),
  orange = everything else.
- **Navigating between routes resets the scroll position to the top, because
  browsers otherwise preserve it and mobile users land mid-page on the screen
  they just opened.**
- Presentation is a "liquid glass" theme — translucent blurred surfaces, pill
  controls, custom-rendered selects — with an iOS-style light/dark switch in
  the navbar, persisted and defaulting to the OS preference. Completing an
  order fires a dependency-free canvas confetti celebration.

## 6. Deployment — 100% free, stable for the review period

| Tier | Platform | Notes |
|------|----------|-------|
| **Frontend** | **Vercel** | Static CDN, zero cold starts, free TLS, deploys on git push. `vercel.json` rewrites all routes to `index.html` so deep links survive a refresh |
| **Backend** | **Render** (free web service) | Docker multi-stage build → slim JRE image; `/api/health` as the health check |
| **Database** | **Neon** (free serverless Postgres) | TLS required; the **direct, non-pooled endpoint** is used |
| **Cache** | **Redis** | Locally a `redis:7-alpine` container; hosted, any Redis endpoint via `REDIS_URL` |

**Why Neon rather than Render's own PostgreSQL: Render's free database expires
after 30 days, while Neon's free tier does not — so the deployment cannot die
mid-review.** **Why the direct rather than the pooled Neon endpoint: the pooler
is PgBouncer in transaction mode, and Flyway's session-level advisory lock plus
JDBC's server-side prepared statements do not survive it.** **Why the cache is
optional by design: absent or unreachable, the app logs a warning and reads
from PostgreSQL, so the deployment never depends on Redis being up.**

**Configuration is entirely environment-driven, so no secret is ever
committed:** `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`FRONTEND_URL` (drives the CORS allow-list), `ISSUER_URL`, `COOKIE_SECURE` and
the optional `REDIS_URL`. `render.yaml` declares them for blueprint deploys.

**Cold-start mitigation:** Render free instances sleep after ~15 minutes idle
and the first hit then takes ~50 s, so a GitHub Actions cron pings
`/api/health` every 10 minutes and visitors always land on a warm service.
Neon's scale-to-zero adds only about a second and needs no mitigation. Expected
load — well under 100 concurrent visitors — is far below every free-tier
ceiling.

**The end-to-end path a visitor exercises:** browse the paginated catalog
(served from Redis when warm, PostgreSQL otherwise) → register with a validated
full name → receive tokens → add books to a server-side cart with optimistic UI
→ check out through shipping and mock payment, which locks stock rows,
decrements them, snapshots prices and empties the cart in one transaction →
see the order in history → cancel it within 24 hours, which restores stock and
marks the order `CANCELLED`. Every one of those steps crosses all three tiers.
