# Weather Alert UI

Modern React + TypeScript dashboard for the Weather Alert backend.

## What It Covers

- Login via `POST /api/auth/token`
- Register + email verification flow
- Forgot username + forgot password recovery flow
- Auth routes split for cleaner UX:
  - `/auth/login`
  - `/auth/register`
  - `/auth/verify-email`
  - `/auth/forgot-password`
  - `/auth/forgot-username`
- Dashboard with:
  - current weather snapshot
  - create alert criteria form
  - active criteria list + delete
  - triggered alerts timeline + acknowledge
  - account profile update
  - password change
  - admin pending-approval panel
  - admin account actions (suspend/reactivate/force reset)

## Run Locally

```bash
cd ui
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5174`.

## Backend Routing

The UI uses Vite proxy so API calls stay same-origin in dev:

- `/api/*` -> backend
- `/actuator/*` -> backend
- `/swagger-ui/*` + `/v3/*` -> backend

Default backend target is `http://localhost:8088` (Docker `weather-app`).
Override with:

```bash
VITE_API_TARGET=http://localhost:8092 npm run dev
```

If you changed backend code and run with Docker, rebuild so new endpoints are included:

```bash
docker compose up -d --build weather-app
```

## Build

```bash
npm run build
npm run preview
```
