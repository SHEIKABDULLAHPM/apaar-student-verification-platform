# Deploying the Platform: Vercel + Render

This guide deploys the React console to **Vercel** and the Spring Boot API plus
PostgreSQL database to **Render**. The Vercel frontend talks directly to the
Render API over HTTPS at `/api/v1`.

```
Browser ──▶ Vercel (static React build, https://your-app.vercel.app)
                 │  fetch https://apaar-backend.onrender.com/api/v1/*
                 ▼
           Render Web Service (Docker, Spring Boot, port from $PORT)
                 │
                 ▼
           Render PostgreSQL (Flyway migrations + demo seed)
```

The AI service is **not required** for this deployment: the backend currently
has no live call path to it, and all anomaly data used by the dashboards comes
from the database.

---

## Part 0 — Push the deployment changes first

`backend/src/main/resources/application.yml` now binds to Render's injected
`PORT` variable, and `frontend/vercel.json` adds SPA routing for Vercel. Commit
and push those before creating the Render service so it builds the correct code.

---

## Part 1 — Backend on Render

### 1.1 Create the PostgreSQL database

1. Render Dashboard → **New +** → **Postgres**.
2. Name: `apaar-postgres`. Region: pick the one closest to your users
   (e.g. **Singapore**). Plan: **Free**.
3. Create it and wait for status **Available**.
4. Open the database and copy, from **Connections**:
   - **Hostname** (internal), e.g. `dpg-xxxxxxxx-a`
   - **Port** (`5432`)
   - **Database** (e.g. `apaar`)
   - **Username**
   - **Password**

> Use the **Internal** hostname for a Render web service in the same region.
> The external hostname (`dpg-xxxx.oregon-postgres.render.com`) works too but is
> slower and unnecessary.
>
> Render's free databases are time-limited (the dashboard shows an expiry date).
> Take a `pg_dump` before that date; the data is deleted when it lapses.

### 1.2 Create the web service

1. **New +** → **Web Service** → connect the GitHub repository
   `SHEIKABDULLAHPM/apaar-student-verification-platform`.
2. Fill in:
   - **Name:** `apaar-backend` (this becomes the URL `https://apaar-backend.onrender.com`)
   - **Region:** the **same region** as the database
   - **Branch:** `main`
   - **Root Directory:** **leave empty** — the Dockerfile copies `database/migrations`
     and `database/seed` from the repo root, so the build context must be the root
   - **Runtime:** **Docker**
   - **Dockerfile Path:** `backend/Dockerfile`
   - **Docker Command:** leave empty (the image's `ENTRYPOINT` starts the jar)
   - **Health Check Path:** `/actuator/health`
   - **Plan:** Free

### 1.3 Environment variables

Add these under **Environment** (or when prompted during creation):

| Variable | Value |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `JWT_SECRET` | a random secret, at least 32 bytes: `openssl rand -base64 48` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<internal-hostname>:5432/<database>` |
| `SPRING_DATASOURCE_USERNAME` | database username |
| `SPRING_DATASOURCE_PASSWORD` | database password |
| `CORS_ALLOWED_ORIGINS` | `https://<your-app>.vercel.app` |
| `AUDIT_IP_HASH_SALT` | a random value: `openssl rand -hex 16` |
| `UPLOAD_STORAGE_DIR` | `/tmp/apaar-uploads` (ephemeral; optional, this is the default) |

Notes:

- **`SPRING_PROFILES_ACTIVE=demo` is required for the demo accounts.** It is what
  applies `V100__demo_seed.sql`, which creates the seven roles and the seeded
  users used by the login page and the guest sign-in buttons. Without it the
  database has an empty `users` table and every login returns 401.
- The datasource URL must start with `jdbc:postgresql://` and include `:5432`.
  A raw `postgres://...` connection string from the dashboard will **not** work.
- `JWT_SECRET` has no default. The service fails to start without it.
- Do **not** set `BACKEND_PORT` on Render; Render injects `PORT`, which the app
  now reads first.
- `CORS_ALLOWED_ORIGINS` is a comma-separated list of exact origins, with
  scheme and **no trailing slash**. Vercel preview deployments use different
  hostnames; add each one you want to test, or test on the production domain.

### 1.4 Deploy and verify

Press **Create Web Service**. The first build takes several minutes (Maven
downloads dependencies on Render's builder).

In the logs, look for:

```
Successfully applied 6 migrations to schema "public" ...
Tomcat started on port 10000
Started ApaarPlatformApplication
```

Then verify from your machine:

```bash
# 1. Health check (also used by Render)
curl -s https://apaar-backend.onrender.com/actuator/health
# {"status":"UP"}

# 2. Demo login must return tokens
curl -s -X POST https://apaar-backend.onrender.com/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@apaar.local","password":"Demo@12345"}'
```

A successful login returns `accessToken`, `refreshToken` and `user`. If you get a
404 here, the path is wrong (`/api/v1/auth/login`, not `/auth/login`).

Optional: the API docs are at `https://apaar-backend.onrender.com/swagger-ui/index.html`.

---

## Part 2 — Frontend on Vercel

### 2.1 Set the API URL

Vercel project → **Settings** → **Environment Variables**:

- **Key:** `VITE_API_BASE_URL`
- **Value:** `https://apaar-backend.onrender.com/api/v1`
- **Environments:** Production and Preview (and Development if you use it)

The value **must include `/api/v1`** and have no trailing slash. Vite inlines
environment variables at build time, so an existing deployment keeps the old
value until you redeploy.

### 2.2 Confirm the project layout

Vercel project → **Settings** → **General**:

- **Root Directory:** `frontend`
- Build command and output directory are read from `frontend/vercel.json`
  (`npm run build`, `dist`), so the defaults are fine.

`vercel.json` rewrites every non-file path to `index.html`, so deep links such as
`/dashboard` or `/cases` load the app instead of returning a 404. Static assets
are served from the filesystem before the rewrite applies.

### 2.3 Redeploy

Vercel → **Deployments** → latest → **⋯** → **Redeploy**. Clear the build cache
if the old `VITE_API_BASE_URL` value persists.

### 2.4 Why you saw "The page could not be found"

The frontend defaults to a **relative** API base (`/api/v1`). On Vercel that
request goes to the static site, which has no API, so Vercel returns its own 404
page and the login screen reports a sign-in failure. Pointing
`VITE_API_BASE_URL` at the Render service (including `/api/v1`) and redeploying
fixes it. No rewrite/proxy is needed on Vercel.

---

## Part 3 — End-to-end checklist

1. `GET https://apaar-backend.onrender.com/actuator/health` → `{"status":"UP"}`.
2. `POST .../api/v1/auth/login` with `admin@apaar.local` / `Demo@12345` → tokens.
3. CORS preflight from the Vercel origin succeeds:

   ```bash
   curl -i -X OPTIONS https://apaar-backend.onrender.com/api/v1/auth/login \
     -H 'Origin: https://your-app.vercel.app' \
     -H 'Access-Control-Request-Method: POST' \
     -H 'Access-Control-Request-Headers: content-type'
   ```

   Expect HTTP 200 and `Access-Control-Allow-Origin: https://your-app.vercel.app`.
   If that header is missing, `CORS_ALLOWED_ORIGINS` is wrong and the browser
   will report a network/CORS error on sign-in.
4. Open the Vercel URL, sign in manually or use a **Demo guest access** button,
   and confirm the dashboard loads seeded data (37 students, 2 open cases).

---

## Troubleshooting

| Symptom | Cause and fix |
| :--- | :--- |
| Render deploy fails at `mvn` | Root Directory was set to `backend`; clear it so `database/` is in the build context. |
| Service exits: `Could not resolve placeholder 'JWT_SECRET'` | Set `JWT_SECRET` in Render and redeploy. |
| Startup fails on Hikari/Flyway | Malformed `SPRING_DATASOURCE_URL`; it must be `jdbc:postgresql://host:5432/db`, and the password/username must match the database. |
| Login returns 401 with demo credentials | Demo profile not active, so no seeded users exist. Set `SPRING_PROFILES_ACTIVE=demo`, redeploy, and check Flyway applied `V100`. |
| Browser shows CORS error | Origin mismatch or trailing slash in `CORS_ALLOWED_ORIGINS`; update it and let Render restart the service. |
| Vercel still calls the old URL | Environment variables are baked at build time; redeploy after editing them. |
| First request after idle takes ~1 minute | Render Free instances spin down after inactivity; the first request cold-starts. |
| `The page could not be found` on sign-in | `VITE_API_BASE_URL` missing, wrong, or lacking `/api/v1`; set it and redeploy. |
| Uploads disappear after a restart | `/tmp` is ephemeral on Render; attach a Render disk or use object storage for durable files. |
