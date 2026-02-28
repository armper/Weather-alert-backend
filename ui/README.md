# Weather Alert UI

Modern React + TypeScript dashboard for the Weather Alert backend.

## What It Covers

- Login via `POST /api/auth/token`
- Register + email verification flow
- Dashboard with:
  - current weather snapshot
  - create alert criteria form
  - active criteria list + delete
  - triggered alerts timeline + acknowledge
  - account profile update
  - admin pending-approval panel

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

Default backend target is `http://localhost:8092`.
Override with:

```bash
VITE_API_TARGET=http://localhost:8080 npm run dev
```

## Build

```bash
npm run build
npm run preview
```
