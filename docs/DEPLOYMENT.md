# Deployment Guide — Vercel + Render + Neon (100% free)

End-to-end, this takes **~30–40 minutes** (plus ~15 min if you configure both OAuth
apps). No credit card is required by any of the three services.

Order matters: **database → backend → frontend → OAuth → keep-warm**.

---

## 1. Database — Neon (free serverless Postgres)

1. Sign up at [neon.tech](https://neon.tech) (GitHub login works).
2. Create a project, e.g. `bookshop` (pick the region closest to your Render region).
3. On the dashboard, open **Connection details** and note:
   - host, e.g. `ep-xxxx-yyyy.eu-central-1.aws.neon.tech`
   - database name (default `neondb`), role and password.
4. Build the JDBC values the backend expects:
   - `DATABASE_URL` = `jdbc:postgresql://<host>/<database>?sslmode=require`
   - `DATABASE_USER` = role name
   - `DATABASE_PASSWORD` = password

> Why Neon and not Render's Postgres: Render's free database **expires after 30
> days**. Neon's free tier doesn't expire, so the demo can't die mid-review.
> Neon scales to zero when idle; wake-up adds ~1 s to the first query, which is fine.

No manual SQL needed — Flyway creates the schema and seeds 20 books on first boot.

## 2. Backend — Render (free Docker web service)

1. Push this repository to GitHub.
2. Sign up at [render.com](https://render.com) with GitHub → **New → Web Service** →
   select the repo.
3. Settings:
   - **Root Directory**: `backend`
   - **Runtime**: Docker (auto-detected from the Dockerfile)
   - **Instance type**: Free
   - **Health check path**: `/api/health`
4. Environment variables:

   | Key | Value |
   |-----|-------|
   | `DATABASE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
   | `DATABASE_USER` | Neon role |
   | `DATABASE_PASSWORD` | Neon password |
   | `JWT_SECRET` | any random string ≥ 64 chars (`openssl rand -hex 48`) |
   | `FRONTEND_URL` | your Vercel URL (add after step 3; redeploy) |
   | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | from step 5 (optional) |
   | `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | from step 5 (optional) |

5. Deploy. First build takes ~5–8 min (Maven build inside Docker). Verify:
   `https://<service>.onrender.com/api/health` → `{"status":"UP", ...}` and
   `/api/books` returns the seeded catalog.

Alternatively, `render.yaml` at the repo root works with Render **Blueprints**
(New → Blueprint) and pre-declares all of the above.

## 3. Frontend — Vercel

1. Sign up at [vercel.com](https://vercel.com) with GitHub → **Add New → Project** →
   import the repo.
2. Settings:
   - **Root Directory**: `frontend`
   - Framework preset: Vite (auto-detected)
3. Environment variable: `VITE_API_URL` = `https://<service>.onrender.com`
   (no trailing slash).
4. Deploy → you get `https://<app>.vercel.app`.
5. Go back to Render and set `FRONTEND_URL=https://<app>.vercel.app`
   (drives CORS and the OAuth redirect target), then redeploy the backend.

`frontend/vercel.json` already rewrites all routes to `index.html` so deep links
like `/orders` and `/oauth2/redirect` work.

## 4. Keep-warm (important for reviewers)

Render free services sleep after ~15 min idle; the first request then takes
~50 s. Two options (the first is already in the repo):

- **GitHub Actions** (`.github/workflows/keep-warm.yml`): in the GitHub repo go to
  *Settings → Secrets and variables → Actions → Variables* and add
  `BACKEND_URL=https://<service>.onrender.com`. The workflow pings `/api/health`
  every 10 minutes. Free, no signup.
- **UptimeRobot / cron-job.org**: free external monitor hitting the same URL every
  5–10 minutes — also gives you an uptime dashboard for the review week.

The catalog page additionally shows a friendly "server may be waking up" message
if a cold start ever slips through.

## 5. Social sign-in (Google & Facebook)

The buttons appear automatically once the env vars exist — no code changes.
Both providers redirect back to the **backend** (it drives the OAuth dance):

```
Google callback:   https://<service>.onrender.com/login/oauth2/code/google
Facebook callback: https://<service>.onrender.com/login/oauth2/code/facebook
```

### Google (~10 min) — works for ANY reviewer account

1. [console.cloud.google.com](https://console.cloud.google.com) → new project.
2. **APIs & Services → OAuth consent screen**: External, fill app name + emails.
   Scopes: only `openid`, `email`, `profile` (non-sensitive — no verification needed).
   **Publish the app** (Testing mode would limit sign-in to listed test users).
3. **Credentials → Create credentials → OAuth client ID** → Web application:
   - Authorized redirect URI: the Google callback above.
4. Copy Client ID/Secret into Render env vars, redeploy.

### Facebook (~10 min) — caveat: Dev Mode limits who can log in

1. [developers.facebook.com](https://developers.facebook.com) → **Create App** →
   type "Consumer" → add the **Facebook Login** product.
2. Facebook Login → Settings → Valid OAuth Redirect URIs: the Facebook callback above.
3. App settings → Basic: copy App ID/Secret into Render env vars, redeploy.
4. **Caveat**: while the app is in **Development Mode**, only app admins/developers/
   testers can authenticate. Add reviewer accounts under *App roles → Testers*, or
   note in the submission that Facebook login is demonstrated with a test account
   (going Live requires a privacy-policy URL + Meta business verification — out of
   scope for a one-week demo). Google + email/password remain available to everyone.

## 6. Pre-submission smoke test

1. Open the Vercel URL in a private window → catalog loads with covers.
2. Register with email/password → add 2 books → change quantity → remove one.
3. Checkout → shipping → mock payment → confirmation shows `PAY-MOCK-…` reference.
4. Orders page shows the order; sign out/in → cart and history persist.
5. "Continue with Google" → consent → lands back signed-in.
6. `GET /api/health` responds fast (keep-warm working) ~1 h after setup.
