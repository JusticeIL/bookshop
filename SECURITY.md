# Security posture

How this REST API web application defends against the common attack classes,
and where each defence lives in the code. Written against the OWASP Top 10.

---

## 1. SQL injection — structurally impossible

There is **no dynamically assembled SQL anywhere in the codebase**. Every read
and write goes through Spring Data JPA, which uses JDBC `PreparedStatement`
parameter binding, so user input is transmitted as a *value* and can never be
parsed as SQL:

- **Derived queries** (`findByEmailIgnoreCase`, `findByUserIdAndBookId`, …) —
  the query is generated from the method signature; arguments are bound.
- **The single explicit query** (`BookRepository.findByIdForUpdate`) is JPQL
  with a named parameter (`:id`), never string concatenation.
- **Search** (`?search=`) reaches the database as a bound `LIKE` parameter.
  A payload such as `'; DROP TABLE books; --` is searched for as literal text
  and returns zero rows.
- **Sorting** (`?sort=`, `?direction=`) is the one place where input could
  become part of a query structurally, since column names cannot be bound as
  parameters. It is therefore **allow-listed** against a fixed `Set` of five
  column names in `BookService`; anything else silently falls back to `id`.
- **Pagination** is coerced to integers and clamped (`page >= 0`,
  `1 <= size <= 50`), which also caps result-set size.
- **Schema** is created by static Flyway migration files, never generated at
  runtime; Hibernate runs in `ddl-auto: validate`, so the application cannot
  alter the schema even if it tried.
- **Least privilege**: the app connects with an ordinary application role, not
  a superuser.

## 2. Authentication & session security

- Passwords hashed with **BCrypt, cost factor 12** — never stored or logged in
  plaintext, and slow enough to make offline cracking expensive.
- Sign-in returns an **RS256-signed JWT access token** (24 h) plus a **rotating
  refresh token** (7 days, 96 bytes of CSPRNG output, single-use — the old
  authorization is consumed on renewal, so a replayed token is rejected).
  Tokens are signed
  with a key generated at startup and published as a JWK set; the API verifies
  every request's signature.
- **No account enumeration**: an unknown email and a wrong password return the
  identical 401 message, and the password is verified against a dummy hash
  when no account matches so the two paths take the same time (closing the
  timing side channel).
- **Rate limiting**: `RateLimitFilter` allows 10 POSTs per IP per minute to
  `/api/auth/login`, `/api/auth/register`, `/oauth2/token` and `/login`,
  answering `429` beyond that. This blunts online password guessing and signup
  floods; the tracking table is bounded so the limiter cannot be used to
  exhaust memory.
- **Full-name and email inputs are length-bounded and pattern-validated**
  server-side (`@Pattern`, `@Size`, `@Email`), so oversized or malformed input
  is rejected before it reaches the domain.

## 3. Authorization — no IDOR

Every user-scoped query is filtered by the **authenticated** user id, taken
from the verified token's `uid` claim — never from a path variable, query
parameter or request body:

```java
orderRepository.findByIdAndUserId(orderId, user.id())   // not findById(orderId)
cartItemRepository.findByUserIdAndBookId(user.id(), bookId)
```

Requesting `DELETE /api/orders/42` for someone else's order 42 returns `404`,
because the row is scoped away before it is ever loaded. There is no endpoint
that accepts a user id as input.

## 4. Cross-site scripting (XSS)

- React escapes all interpolated content by default, and the codebase contains
  **no `dangerouslySetInnerHTML`**, no `eval`, and no HTML built from strings.
- A strict **Content-Security-Policy** is sent on every response:
  `default-src 'none'` for the API (it serves only JSON), and a `'self'`-based
  policy with `object-src 'none'` for the one server-rendered page.
- `X-Content-Type-Options: nosniff` prevents MIME-type confusion.

## 5. Cross-site request forgery (CSRF)

The API authenticates with an `Authorization` header, **not cookies**. A
malicious page can make a browser send cookies automatically, but it cannot
make it attach a bearer token, so there is no CSRF surface — which is why CSRF
protection is deliberately disabled on `/api/**` and left **enabled** on the
session-backed sign-in page, where it matters.

## 6. Clickjacking and framing

`X-Frame-Options: DENY` plus `frame-ancestors 'none'` in the CSP: the
application cannot be embedded in an iframe, so UI-redress attacks fail.

## 7. Transport and headers

- **HSTS** (`max-age` 1 year, `includeSubDomains`) — browsers refuse plaintext
  HTTP after the first visit. Both Vercel and Render terminate TLS.
- **Referrer-Policy: no-referrer** — URLs never leak to third parties.
- **Permissions-Policy** disables geolocation, microphone, camera, payment and
  USB, none of which the app uses.
- Session cookies (used only by the sign-in page) are `HttpOnly` and
  `SameSite=Lax`, and `Secure` in production via `COOKIE_SECURE=true`.

## 8. CORS

A strict allow-list: only the deployed SPA origin and the local dev server may
call the API, with only the methods and headers actually needed.
`allowCredentials` is `false` — bearer tokens, never cookies. No wildcard
origin is ever used.

## 9. Information disclosure

- A catch-all exception handler logs the failure server-side and returns a
  generic `"Something went wrong"` — **stack traces, SQL fragments and class
  names never reach the client**. Spring's error attributes are configured to
  `include-message: never`, `include-stacktrace: never`.
- No Actuator endpoints are exposed.
- No secrets in the repository: database credentials, Redis URL and frontend
  URL all arrive as environment variables, and the token signing key is
  generated in memory at startup.

## 10. Denial of service / resource exhaustion

- Page size clamped to 50 rows; request header limit 16 KB; form POST limit
  256 KB; multipart uploads disabled entirely (the API accepts no files).
- Rate limiting on the credential endpoints (above).
- Redis caching absorbs repeated catalog reads, and its failure mode is to
  fall through to PostgreSQL rather than error.

## 11. Business-logic integrity

Overselling is prevented in depth: the UI disables add-to-cart at zero stock,
the service validates every quantity change, and checkout re-reads each book
with `SELECT … FOR UPDATE` inside the transaction so simultaneous buyers
serialize — with a `CHECK (stock >= 0)` constraint as the final backstop.
Order history is immutable: `order_items` snapshot the title and unit price at
purchase time, and cancelling flips a status rather than deleting rows.

## 12. Dependencies and supply chain

Dependency versions are managed by the Spring Boot BOM, so the whole server
stack moves together with one version bump. No unmaintained or single-author
authentication libraries are used — the OAuth2/JWT machinery is Spring
Security itself.

---

## Known limitations (deliberate, for a demo of this scope)

- Payment is **mocked by design** — no card data is ever collected, stored or
  transmitted, which keeps the service entirely out of PCI-DSS scope.
- Rate-limit counters live in process memory; a multi-instance deployment
  would move them into Redis.
- Tokens and the signing key are held in memory, so restarting the backend
  signs everyone out.
- No email verification or password reset yet; both need an email provider.
