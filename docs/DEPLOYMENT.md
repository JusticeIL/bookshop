# Deployment Guide — Vercel + Render + Neon (100% free)

End-to-end this takes **~40 minutes**, most of it waiting for the first backend
build. No credit card is required by any of the services.

**Order matters:** GitHub → database → backend → frontend → link the two → keep-warm.

> **Do I need to reset the database?** No. Production starts against a brand-new
> empty Neon database, and Flyway creates the schema and seeds the 20 books on
> first boot. Nothing from your local Docker database travels to production —
> your local test accounts stay local. (If you want a clean local machine too,
> `docker compose down -v` wipes the local volume and re-seeds on next start.)

---

## 0. Push the repository to GitHub

Render and Vercel both deploy *from* GitHub, so this comes first.

```bash
cd C:\Users\User\Documents\Coding\Projects\Bookshop
git add .
git commit -m "Online Bookshop - full-stack home assignment"
git branch -M main
git remote add origin https://github.com/<you>/online-bookshop.git
git push -u origin main
```

Check that `_to_delete/` is gone and that no `.env` file was committed.

## 1. Database — Neon (free serverless PostgreSQL)

1. Sign up at [neon.tech](https://neon.tech) (GitHub login works).
2. **Create project** → name `bookshop`, PostgreSQL 16, pick the region closest
   to the Render region you'll choose in step 2.
3. Open **Connection details** and copy the host, database name, role and
   password.
4. Assemble the three values Render will need:

   - `DATABASE_URL` = `jdbc:postgresql://<host>/<database>?sslmode=require`
     (note the `jdbc:` prefix — Neon shows a `postgresql://…` URL, which is
     *not* the same thing)
   - `DATABASE_USER` = the role name
   - `DATABASE_PASSWORD` = the password

> **Why Neon and not Render's PostgreSQL:** Render's free database expires after
> 30 days. Neon's free tier doesn't, so the demo can't die mid-review. Neon
> scales to zero when idle; the first query after a pause adds about a second.

No manual SQL is needed — Flyway creates all five tables and seeds the catalog.

## 2. Backend — Render (free Docker web service)

1. Sign up at [render.com](https://render.com) with GitHub.
2. **New → Web Service** → select the repository.
3. Settings:
   - **Root Directory**: `backend`
   - **Runtime**: Docker (auto-detected)
   - **Instance type**: Free
   - **Health check path**: `/api/health`
4. Add the environment variables:

   | Key | Value |
   |-----|-------|
   | `DATABASE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
   | `DATABASE_USER` | Neon role |
   | `DATABASE_PASSWORD` | Neon password |
   | `FRONTEND_URL` | `https://<app>.vercel.app` — **you only get this in step 3**, so set a placeholder now and correct it in step 4 |
   | `ISSUER_URL` | `https://<service>.onrender.com` (this service's own URL) |
   | `COOKIE_SECURE` | `true` |
   | `REDIS_URL` | *optional* — see step 5. Leave unset to run without a cache. |

5. **Create Web Service.** The first build takes 5–8 minutes (Maven runs inside
   Docker). Then verify:
   - `https://<service>.onrender.com/api/health` → `{"status":"UP",…}`
   - `https://<service>.onrender.com/api/books` → the seeded catalog JSON

Alternatively `render.yaml` at the repo root works with **New → Blueprint** and
pre-declares all of the above.

## 3. Frontend — Vercel

1. Sign up at [vercel.com](https://vercel.com) with GitHub.
2. **Add New → Project** → import the repository.
3. Settings:
   - **Root Directory**: `frontend`
   - Framework preset: **Vite** (auto-detected)
4. Environment variable: `VITE_API_URL` = `https://<service>.onrender.com`
   — no trailing slash.
5. **Deploy** → you get `https://<app>.vercel.app`.

`frontend/vercel.json` already rewrites every route to `index.html`, so deep
links like `/orders` and `/credits` survive a refresh.

## 4. Link the two (don't skip — nothing works without it)

Back in Render, set `FRONTEND_URL` to the real Vercel URL and **redeploy the
backend**. That value drives the CORS allow-list; until it matches exactly,
every API call from the deployed site fails in the browser.

Quick check: open the Vercel URL, press F12 → Console. No CORS errors, catalog
renders.

## 5. Redis cache (optional, ~5 minutes)

The app runs fine without it — if `REDIS_URL` is unset or unreachable the API
logs a warning and serves straight from PostgreSQL.

To enable: create a free database at [upstash.com](https://upstash.com), copy
its `redis://…` (or `rediss://…`) connection URL into Render's `REDIS_URL`, and
redeploy.

## 6. Keep-warm (important for reviewers)

Render free services sleep after ~15 minutes idle, and the next visitor then
waits ~50 seconds. Pick one:

- **GitHub Actions** (already in the repo): in GitHub go to *Settings → Secrets
  and variables → Actions → Variables* and add
  `BACKEND_URL = https://<service>.onrender.com`. The workflow pings
  `/api/health` every 10 minutes. Free, no signup.
- **UptimeRobot** or **cron-job.org**: a free external monitor hitting the same
  URL every 5–10 minutes, with an uptime dashboard as a bonus.

The catalog page also shows a friendly "the server may be waking up" message if
a cold start ever slips through.

## 7. Pre-submission smoke test

Run this in a **private window** against the live Vercel URL:

1. Catalog loads with cover images.
2. **Register** — full name must be two words, letters only (e.g. `Gal
   Rubinstein`); a single word or a name with digits must be rejected. You land
   signed in.
3. **Sign out**, then **sign in** again with the same credentials.
4. Add two books, change a quantity, remove one — the cart badge tracks it.
5. **Checkout** → address → mock payment → confirmation shows a `PAY-MOCK-…`
   reference and the rainbow confetti fires.
6. **Orders** page lists the order. Cancel it with the bin button → the tag
   turns red `CANCELLED` and the stock is restored.
7. Buy out a low-stock title (*Introduction to Algorithms* starts at 3) → it
   stays listed with an **OUT OF STOCK** badge and a disabled button.
8. Flip the light/dark switch, then reload — the choice persists.
9. Refresh directly on `/orders` — no 404 (the Vercel rewrite works).
10. An hour later, confirm `/api/health` still answers instantly (keep-warm).

## 8. Fill in the submission

Update the table at the top of `README.md` with the two live URLs, then send:

- **Live application**: `https://<app>.vercel.app`
- **Repository**: `https://github.com/<you>/online-bookshop`
- **Short description**: frontend React + TypeScript on Vercel; backend Java 21
  / Spring Boot 3 REST API on Render (Docker); database PostgreSQL 16 on Neon;
  Redis read-cache; payment and shipping mocked.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Catalog empty, console shows a CORS error | `FRONTEND_URL` on Render doesn't exactly match the Vercel origin | Correct it (no trailing slash) and redeploy |
| Backend fails at boot with a Flyway or JDBC error | `DATABASE_URL` missing the `jdbc:` prefix or `?sslmode=require` | Fix the value and redeploy |
| Login returns 401 for a correct password | The backend restarted (signing key is regenerated at startup) | Sign in again |
| Login returns 429 | Rate limiter — 10 attempts per IP per minute | Wait a minute |
| First visit takes ~50 s | Render cold start | Set up keep-warm (step 6) |
| `/orders` 404s on refresh | Vercel root directory isn't `frontend` | Re-check the project settings |
