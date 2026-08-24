# Everything changed in this pass, in order

## Security-critical (do this first, independent of the code)
- **Rotate your TiDB password.** `docker-compose.yml` had a real TiDB
  username and password hardcoded in plaintext. It's now read from
  environment variables instead, but if that file was ever pushed to any
  git repo, treat the old password as burned and rotate it in TiDB Cloud
  regardless.

## Bug fixes (behavior changes, all verified against your live logs where possible)
1. `JwtAuthenticationFilter.java` — removed a duplicate DB query that ran
   on every single authenticated request (confirmed fixed via your Render
   logs: the RBAC query now appears once per request, not twice).
2. `AiAnalysisService.java` — added a hard 25-second timeout around the
   Groq call. Previously there was no timeout at all, which is why
   analysis could hang for 6+ minutes. Now it fails fast with a clear
   message instead.
3. `SecurityConfig.java` — added a no-op `UserDetailsService` bean so
   Spring Boot stops generating a fake in-memory user and logging
   "Using generated security password" (confirmed gone from your logs).
4. `GlobalExceptionHandler.java` — the catch-all handler used to return
   the raw exception class name, root cause class name, and exception
   message to the client on any unhandled error. That's an information
   disclosure risk. It now logs full details server-side with a
   correlation ID and returns only a generic message + that ID to the
   client.
5. RBAC data fix (not code) — your account only had `ROLE_USER`; granted
   `ROLE_OPERATOR` directly in TiDB. Confirmed working — assign incident
   now returns 200.

## Hardening (new, non-breaking additions)
6. `LoginRateLimiter.java` + `RateLimitExceededException.java` (new
   files) — 5 failed login attempts per username within 5 minutes now
   returns HTTP 429 instead of allowing unlimited password guessing.
   Dependency-free (no new library), in-memory, single-instance only —
   see the class-level comment for the scaling caveat.
7. Replaced `System.out.println` / `System.err.println` with proper
   SLF4J logger calls in `UserService`, `IncidentController`, and
   `IncidentEventConsumer`. No behavior change, just makes Render's log
   viewer/filtering actually useful.
8. Fixed a stale mismatch in `IncidentEventConsumer`'s
   `@KafkaListener(autoStartup = "${KAFKA_ENABLED:true}")` — default
   didn't match the app's actual default of `false` in
   `application.yml`. Aligned to `false`.
9. `docker-compose.yml` — also fixed leftover `OLLAMA_*` environment
   variables that did nothing (the app reads `OPENAI_*` now, since it
   moved to Groq). Replaced with the variables that are actually used.

## Repo hygiene (new files)
10. Root `.gitignore` — didn't exist before. Without it, `node_modules`,
    `target`, and any real `.env` file would get committed by accident.
11. `.env.example` — documents every environment variable the app needs,
    with no real secrets, so you have a template for a real `.env`.

## Tests
12. `AiAnalysisServiceTest.java` — 2 of the 3 existing tests asserted on
    the string `"Ollama AI service unavailable"`, left over from before
    this app was migrated from Ollama to Groq. That string doesn't exist
    anywhere in the current code, so those 2 tests were already broken
    before I touched anything. Fixed the assertions to match current,
    real behavior.

## What I could verify vs. what you need to verify
- **Frontend**: ran a real `npm install` + `npm run build` in a sandbox —
  it succeeded cleanly, 0 errors, 0 warnings. This is a genuine build
  verification, not a guess.
- **Backend**: I do not have network access to Maven Central in this
  environment, so I could not run `mvn clean package` here. Every changed
  file was hand-reviewed line by line, and brace/parenthesis balance was
  checked programmatically across all of them — but you must run
  `mvn clean package` locally before pushing, as the final gate. If it
  fails, send me the exact error and I'll fix it immediately.

## What was deliberately left alone (not bugs, just judgment calls)
- No pagination added to `/api/incidents/all` — would require also
  changing the frontend's expected response shape (it currently expects
  a plain array), which is a bigger, riskier change than what you asked
  for in this pass.
- No caching added to the per-request user/role DB lookups — after the
  RBAC debugging you just went through, adding a TTL cache right now
  would reintroduce "why isn't my role change showing up immediately"
  confusion. Worth doing later, deliberately not in this pass.
- No refresh-token / logout-revocation system — real architectural
  decision (token store, expiry strategy), not a drop-in fix.
- Render's ~160s cold start is unchanged — that's free-tier platform
  behavior, not something in this codebase. The `keep-alive.yml` GitHub
  Action from earlier reduces how often you hit it.
