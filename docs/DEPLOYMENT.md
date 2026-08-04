# UniWiki-AI deployment

## Recommended production layout

```text
Vercel (frontend)
    -> HTTPS Spring Boot service
        -> managed MySQL
        -> HTTPS AI service with a persistent volume for ChromaDB

Local crawler worker
    -> Spring Boot crawler API / managed MySQL
```

The Selenium crawler needs Chrome, an interactive Everytime login, and a persistent browser profile.
Keep it on an authorized local worker instead of a serverless deployment. Never place Everytime credentials
in Vercel, Docker images, or repository secrets.

## 1. Backend and AI containers

`backend/Dockerfile` and `ai/Dockerfile` can be deployed to a container platform such as Railway,
Render, Fly.io, or a VM. Provision MySQL separately and attach a persistent volume to `/data/chroma`
for the AI service.

Backend environment variables:

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_URL` | yes | Managed MySQL JDBC URL |
| `DB_USERNAME` | yes | Application DB user |
| `DB_PASSWORD` | yes | Application DB password |
| `JWT_SECRET` | yes | Random value of at least 32 characters |
| `CORS_ALLOWED_ORIGINS` | yes | Vercel URL; comma-separated values are supported |
| `AI_SERVICE_BASE_URL` | yes | Private or public AI service URL |
| `PORT` | platform | HTTP port assigned by the platform |

AI environment variables:

| Variable | Required | Purpose |
| --- | --- | --- |
| `OPENAI_API_KEY` | for generated answers | LLM provider key |
| `OPENAI_MODEL` | no | LLM model name |
| `CHROMA_PERSIST_DIR` | yes | Use `/data/chroma` on the mounted volume |
| `PORT` | platform | HTTP port assigned by the platform |

Apply `database/schema.sql` and the required migration files to the managed MySQL instance before
serving traffic. Seed files are optional and should be reviewed before production use.

## 2. Vercel frontend

Create a Vercel project with `frontend` as its Root Directory. The included `vercel.json` builds Vite
and falls back to `index.html` for React routes.

Set this Production and Preview environment variable:

```text
VITE_API_BASE_URL=https://your-backend.example.com
```

Set the backend's `CORS_ALLOWED_ORIGINS` to the production Vercel URL. Add preview domains explicitly,
or use an intentional pattern such as `https://uniwiki-*.vercel.app`.

## 3. Docker Compose smoke test

Copy the example without committing the result:

```powershell
Copy-Item infra/.env.example infra/.env
docker compose --env-file infra/.env -f infra/docker-compose.yml up --build
```

Then verify:

- frontend: `http://localhost:5173`
- backend Swagger: `http://localhost:8080/swagger-ui/index.html`
- AI health: `http://localhost:8000/health` from inside the AI container/network

The compose file persists MySQL and ChromaDB in named volumes. Back up both volumes before upgrades.

## Release checklist

1. Run backend tests, frontend build, and AI tests.
2. Apply reviewed DB migrations.
3. Use platform secrets for all credentials.
4. Confirm CORS with the exact Vercel domains.
5. Confirm the AI volume survives a redeploy.
6. Synchronize approved wiki posts and check vector-store stats.
7. Keep crawler endpoints restricted to administrators or a private network.
