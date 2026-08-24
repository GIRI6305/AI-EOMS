# AI-EOMS — Setup Guide

Read `CHANGELOG.md` first — it lists every change made in this pass and,
just as importantly, what wasn't changed and why.

## 1. Open in VS Code

Unzip this folder and open it in VS Code. You'll see `backend/` (Spring
Boot) and `frontend/` (React + Vite), plus this README, `CHANGELOG.md`,
`.env.example`, and `docker-compose.yml` at the root.

## 2. Set up your local environment file

```
cp .env.example .env
```

Fill in `.env` with your real TiDB credentials, JWT secret, and Groq API
key. **Never commit this file** — `.gitignore` already excludes it.

If you were using the old `docker-compose.yml` with the hardcoded TiDB
password, rotate that password in TiDB Cloud now — treat it as
compromised regardless of what happens next.

## 3. Build and verify the backend

```
cd backend
mvn clean package
```

This must show `BUILD SUCCESS`. I hand-reviewed every changed file and
checked brace/parenthesis balance programmatically, but I don't have
network access to Maven Central in my environment, so this is the one
step I could not run myself — it's your final gate before pushing.

If it fails, copy the exact error output back to me and I'll fix it.

## 4. Build and verify the frontend

```
cd frontend
npm install
npm run build
```

I already ran this exact sequence and it built cleanly (0 errors, 0
warnings) — you're just confirming it still does on your machine.

## 5. Push to GitHub

```
git init
git add .
git commit -m "Initial commit with all fixes applied"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```

Because `.gitignore` is now in place, `node_modules`, `target`, and your
real `.env` will NOT be committed — that's correct, they shouldn't be.

## 6. Set environment variables on Render

In the Render dashboard, set the same variables from `.env.example` as
actual environment variables for your backend service: `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`,
`OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`, `KAFKA_ENABLED=false`
(unless you're actually running Kafka), `PORT`.

## 7. Deploy and verify

Render will build and deploy automatically once connected to your GitHub
repo. Expect the cold start to still take roughly the same ~160 seconds
on the free tier — that part is a platform limit, not something in this
codebase (see `CHANGELOG.md`). Optionally add `.github/workflows/keep-alive.yml`
from the earlier message to reduce how often you hit that cold start.

## 8. Confirm your account's role in TiDB

The account you're testing with needs `ROLE_OPERATOR` or `ROLE_ADMIN` to
assign incidents — `ROLE_USER` alone will correctly get a 403. If you
create a new test account, promote it the same way we did for `giri`:
grant it via `user_roles` in TiDB directly (there's currently no admin UI
for this in the app).

## If anything fails

Send me the exact error, log line, or HTTP response — not a summary of
it. Exact text lets me find the real cause instead of guessing.
