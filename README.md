# Spaco

Multi-tenant platform for spas, sibling of barberco and restauco (same stack and conventions).
Demo tenant: **Miel Spa & Bienestar** (Cali), with the "Botica de Miel" design of
`../miel-spa-design` (every treatment is an apothecary label).

- **Public site** (`/`, `/tratamientos/{slug}`): the catalogue with descriptions, benefits,
  "formula" table and contraindications, plus a before/after **testimonial carousel** fed only by
  patients who authorized it.
- **Booking without an account** (`/agendar`): treatment → date and time (real free slots per
  professional) → contact data + personal-data consent → done. The request stays PENDING
  (holding the slot) until the spa confirms it; `/cita/{token}` lets her see or cancel it.
- **Patient portal** (`/mi-historia`, Google login): her anamnesis, sessions with photos,
  appointments, consents (she grants or revokes the photo/testimonial one) and **who accessed
  her record**. The account links to the patient registered with the same verified email.
- **Staff panel** (`/panel`): requests, agenda, patients and their **clinical record**
  (anamnesis, session notes, private before/after photos, consents signed at the spa,
  contraindication check, access log), testimonials, and for the owner: treatments, schedule,
  team and legal texts.
- **MCP server** (`/spaco-api/mcp/{spa}`): read-only tools so an esthetician asks Claude in
  natural language about her patients (see below).

Roles live in spaco's `tenant_member` (invited by email, linked on first login):
OWNER (everything), ESTHETICIAN (clinical data, testimonials, MCP), RECEPTION (requests,
agenda, contact data; **no** clinical data).

## Platform (shared Postgres and authco)

Spaco does not run its own Postgres or authco: it uses the ones in `../platform` over the
external `platform` Docker network. Already done for this machine (2026-10-21):

- role and database `spaco` in the shared Postgres (also in `platform/deploy/postgres/init-databases.sh`);
- `SPACO_URL` in `platform/.env`, appended to `AUTHCO_ISSUER`, `CORS_ALLOWED_ORIGINS` and
  `AUTHCO_RESOURCES` in `platform/docker-compose.yml`;
- authco client `spaco-app` (copied from `restauco-app`) with the localhost and tunnel callbacks.

Still manual: add `<PUBLIC_URL>/authco/login/oauth2/code/google` to the Google OAuth client.

## Run

```sh
(cd ../platform && docker compose up -d)
cp .env.example .env   # fill it in (PASSWORD_DB = platform's SPACO_DB_PASSWORD)
docker compose up -d --build   # -> http://localhost:8093 (or PUBLIC_URL)
```

`BOOTSTRAP_OWNER_EMAIL` becomes the first OWNER of `miel-spa`.

### Public demo (Cloudflare quick tunnel)

```sh
docker compose --profile demo up -d --no-deps tunnel
docker compose logs --since 2m tunnel | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com'
```

The URL changes every time the tunnel container is recreated (so only recreate it on purpose,
and start the rest with `--no-deps`). On a new URL update, in this order:
1. `PUBLIC_URL` in `.env` and `SPACO_URL` in `../platform/.env`;
2. `(cd ../platform && docker compose up -d --no-deps authco)`;
3. the authco client callbacks:
   ```sh
   docker exec platform-postgres-1 psql -U restauco -d authco -c "update oauth2_registered_client \
     set redirect_uris='http://localhost:8093/auth/callback,<URL>/auth/callback', \
         post_logout_redirect_uris='http://localhost:8093/,<URL>/' where client_id='spaco-app'"
   ```
4. Google: `<URL>/authco/login/oauth2/code/google`;
5. `docker compose up -d --no-deps spaco-api spaco-app`.

Tests: `docker run --rm -v "$PWD":/w -w /w amazoncorretto:25-alpine ./gradlew test`

## Layout

- `src/` Spring Boot 4 API (`/spaco-api`), clean architecture like restauco:
  `entity/` domain, `usecase/`, `infrastructure/` (controllers, JPA, MCP).
  Tenant = `X-Tenant` header (or the MCP path), Hibernate `@TenantId`.
- `spaco-app/` Angular 19 SPA, runtime `config.json`, tenant theme in `src/themes/`.
- `deploy/gateway` nginx: `/spaco-api`, `/authco` (shared), `/`, and the OAuth discovery
  documents MCP clients look for at the origin root.

## Clinical data and privacy

- Booking requires accepting the personal/health data policy (Ley 1581 de 2012); every consent
  stores the version of the text accepted. Texts are versioned in `/panel/autorizaciones`.
  **The seeded texts are drafts: have them reviewed by a lawyer before going live.**
- Clinical photos are stored under `uploads/private/{spa}` and only served through authorized
  endpoints (staff with clinical access, or the patient herself). JPEG location/EXIF data is
  stripped on upload.
- Publishing a testimonial needs the patient's valid PHOTO_TESTIMONIAL consent and copies the
  chosen photos to `uploads/public`; revoking (from her portal or at the spa) hides it and
  deletes the public copies at once. The public list re-checks the consent on every request.
- Every read or change of clinical data (panel, portal, MCP) is written to `clinical_access_log`;
  the patient sees it in her portal.
- Not yet: column encryption of clinical fields, retention policy, and notifications by
  WhatsApp (contact goes through `wa.me` links for now).

## MCP server (ask about patients in natural language)

`<PUBLIC_URL>/spaco-api/mcp/<spa>`, e.g. `https://…/spaco-api/mcp/miel-spa`. Streamable HTTP,
stateless JSON-RPC, read-only tools:

| Tool | What |
|---|---|
| `search_patients` | by name (accents/typos), phone or email |
| `get_patient_summary` | anamnesis, alerts, last sessions, upcoming appointments, consents |
| `get_clinical_record` | the anamnesis |
| `list_sessions` / `get_session` | session notes (photos only described) |
| `search_session_notes` | Spanish full-text search over every note |
| `check_contraindications` | treatment contraindications vs the patient's history |
| `get_patient_appointments` / `list_appointments` | appointments |
| `list_treatments` / `list_professionals` | catalogue and team |

Auth: authco OAuth. A 401 points to the RFC 9728 metadata (served by Spring Security), which
names authco; MCP clients register themselves (authco DCR allows the claude.ai/claude.com
callbacks) and the user logs in with Google. Only OWNER and ESTHETICIAN members of that spa get
through; every call is audited with channel MCP.

In Claude: Settings → Connectors → Add custom connector → paste the URL → log in with the staff
Google account. The panel page `/panel/asistente` shows the URL and the steps.

Local check: `npx @modelcontextprotocol/inspector` against the URL above.

## New spa

1. Insert the tenant (Flyway migration or SQL) with its `branding` JSON (`wordmark`, `subline`,
   `tagline`, `intro`, `city`, `hours`, `address`, `whatsapp`, `email`, `instagram`,
   `heroTreatment`, optional `theme` color overrides), its categories, treatments, professionals,
   schedule and consent texts (see `V2__seed_miel_spa.sql`).
2. Add `spaco-app/src/themes/<slug>.css` with the same tokens, import it in `styles.css`, and
   deploy the SPA with `TENANT=<slug>`.
3. Set `BOOTSTRAP_OWNER_EMAIL`/`BOOTSTRAP_TENANT`, or insert the owner in `tenant_member`.
