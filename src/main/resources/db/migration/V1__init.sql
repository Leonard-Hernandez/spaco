-- Multi-tenant SaaS for spas: every spa is a tenant; data is scoped by tenant_id.

-- Overlap constraint on appointments (=, &&) and fuzzy patient search
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE tenant (
    id                   BIGSERIAL    PRIMARY KEY,
    slug                 VARCHAR(60)  NOT NULL UNIQUE,
    name                 VARCHAR(120) NOT NULL,
    timezone             VARCHAR(60)  NOT NULL DEFAULT 'America/Bogota',
    contact_phone        VARCHAR(30),
    address              VARCHAR(255),
    slot_step_min        INT          NOT NULL DEFAULT 30 CHECK (slot_step_min BETWEEN 5 AND 120),
    min_booking_lead_min INT          NOT NULL DEFAULT 120 CHECK (min_booking_lead_min >= 0),
    branding             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Staff allowed into the panel. Invited by email; authco_sub is linked on the first
-- authenticated visit (authco has no notion of tenants).
CREATE TABLE tenant_member (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL REFERENCES tenant (id),
    email      VARCHAR(150) NOT NULL,
    authco_sub VARCHAR(64),
    name       VARCHAR(120),
    role       VARCHAR(12)  NOT NULL CHECK (role IN ('OWNER', 'ESTHETICIAN', 'RECEPTION')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email),
    UNIQUE (tenant_id, authco_sub)
);

-- Catalogue ----------------------------------------------------------------------------

-- ink: label color of the category on the site (honey, lavender, sage, navy, clay)
CREATE TABLE treatment_category (
    id        BIGSERIAL   PRIMARY KEY,
    tenant_id BIGINT      NOT NULL REFERENCES tenant (id),
    name      VARCHAR(80) NOT NULL,
    slug      VARCHAR(60) NOT NULL,
    ink       VARCHAR(12) NOT NULL DEFAULT 'honey',
    position  INT         NOT NULL DEFAULT 0,
    active    BOOLEAN     NOT NULL DEFAULT true,
    UNIQUE (tenant_id, slug)
);

-- price in whole pesos; NULL = not published yet (the site shows "Consultar").
-- benefits: ["..."]; composition: [{label, value}] (the "Fórmula" label table).
-- contraindications: free text the esthetician checks against the clinical record.
CREATE TABLE treatment (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL REFERENCES tenant (id),
    category_id       BIGINT       NOT NULL REFERENCES treatment_category (id),
    formula_no        INT,
    name              VARCHAR(120) NOT NULL,
    slug              VARCHAR(80)  NOT NULL,
    tagline           VARCHAR(200),
    description       TEXT,
    benefits          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    composition       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    contraindications TEXT,
    duration_min      INT          NOT NULL CHECK (duration_min > 0),
    buffer_min        INT          NOT NULL DEFAULT 0 CHECK (buffer_min >= 0),
    price             BIGINT       CHECK (price >= 0),
    image_path        VARCHAR(80),
    position          INT          NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, slug)
);
CREATE INDEX idx_treatment_category ON treatment (tenant_id, category_id, position);

-- Who performs treatments. member_id links a panel account (optional).
CREATE TABLE professional (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL REFERENCES tenant (id),
    display_name VARCHAR(120) NOT NULL,
    title        VARCHAR(120),
    bio          TEXT,
    photo_path   VARCHAR(80),
    member_id    BIGINT       REFERENCES tenant_member (id) ON DELETE SET NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Schedule -----------------------------------------------------------------------------

-- weekday: ISO (1 = Monday ... 7 = Sunday)
CREATE TABLE availability_rule (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT    NOT NULL REFERENCES tenant (id),
    professional_id BIGINT    NOT NULL REFERENCES professional (id) ON DELETE CASCADE,
    weekday         SMALLINT  NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    start_time      TIME      NOT NULL,
    end_time        TIME      NOT NULL,
    active          BOOLEAN   NOT NULL DEFAULT true,
    CHECK (start_time < end_time)
);
CREATE INDEX idx_rule_professional ON availability_rule (tenant_id, professional_id, weekday);

-- professional_id NULL = the whole spa. BLOCK without times = the whole day.
CREATE TABLE availability_exception (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenant (id),
    professional_id BIGINT       REFERENCES professional (id) ON DELETE CASCADE,
    date            DATE         NOT NULL,
    start_time      TIME,
    end_time        TIME,
    type            VARCHAR(5)   NOT NULL CHECK (type IN ('BLOCK', 'EXTRA')),
    note            VARCHAR(255),
    CHECK ((start_time IS NULL AND end_time IS NULL AND type = 'BLOCK') OR start_time < end_time)
);
CREATE INDEX idx_exception_date ON availability_exception (tenant_id, date);

-- Patients and bookings ----------------------------------------------------------------

-- Created by the first booking, without an account. authco_sub links the patient portal on
-- the first Google login whose verified email matches.
CREATE TABLE patient (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL REFERENCES tenant (id),
    full_name  VARCHAR(120) NOT NULL,
    phone_e164 VARCHAR(20)  NOT NULL,
    email      VARCHAR(150),
    birth_date DATE,
    authco_sub VARCHAR(64),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, phone_e164),
    UNIQUE (tenant_id, authco_sub)
);
CREATE INDEX idx_patient_email ON patient (tenant_id, lower(email));
CREATE INDEX idx_patient_name_trgm ON patient USING gin (full_name gin_trgm_ops);

CREATE TABLE appointment (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL REFERENCES tenant (id),
    treatment_id     BIGINT       NOT NULL REFERENCES treatment (id),
    professional_id  BIGINT       NOT NULL REFERENCES professional (id),
    patient_id       BIGINT       NOT NULL REFERENCES patient (id),
    start_at         TIMESTAMPTZ  NOT NULL,
    end_at           TIMESTAMPTZ  NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    -- What the patient wrote when booking
    patient_note     VARCHAR(500),
    management_token UUID         NOT NULL UNIQUE,
    decided_at       TIMESTAMPTZ,
    reject_reason    VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (start_at < end_at)
);
CREATE INDEX idx_appointment_start ON appointment (tenant_id, start_at);
CREATE INDEX idx_appointment_patient ON appointment (tenant_id, patient_id, start_at DESC);
-- A professional is never double-booked, even with two requests racing for the same slot
ALTER TABLE appointment ADD CONSTRAINT no_overlap_professional EXCLUDE USING gist (
    professional_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&
) WHERE (status IN ('PENDING', 'ACCEPTED'));

-- Clinical record ----------------------------------------------------------------------

-- Anamnesis: one per patient, kept up to date by the esthetician.
CREATE TABLE clinical_record (
    patient_id   BIGINT       PRIMARY KEY REFERENCES patient (id) ON DELETE CASCADE,
    tenant_id    BIGINT       NOT NULL REFERENCES tenant (id),
    allergies    TEXT,
    medications  TEXT,
    conditions   TEXT,
    -- Pregnancy or breastfeeding (many treatments are contraindicated)
    pregnancy    VARCHAR(120),
    skin_type    VARCHAR(60),
    -- Fitzpatrick I-VI
    phototype    SMALLINT     CHECK (phototype BETWEEN 1 AND 6),
    surgeries    TEXT,
    habits       TEXT,
    observations TEXT,
    updated_by   VARCHAR(120),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- What was done in each visit
CREATE TABLE session_note (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenant (id),
    patient_id      BIGINT       NOT NULL REFERENCES patient (id) ON DELETE CASCADE,
    appointment_id  BIGINT       UNIQUE REFERENCES appointment (id),
    treatment_id    BIGINT       REFERENCES treatment (id),
    professional_id BIGINT       REFERENCES professional (id),
    performed_at    TIMESTAMPTZ  NOT NULL,
    products        TEXT,
    parameters      TEXT,
    observations    TEXT,
    recommendations TEXT,
    created_by      VARCHAR(120),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_session_patient ON session_note (tenant_id, patient_id, performed_at DESC);
CREATE INDEX idx_session_fts ON session_note USING gin (to_tsvector('spanish',
    coalesce(products, '') || ' ' || coalesce(parameters, '') || ' ' ||
    coalesce(observations, '') || ' ' || coalesce(recommendations, '')));

-- Private photos (uploads/private): only staff with clinical access and the patient see them
CREATE TABLE session_photo (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenant (id),
    session_note_id BIGINT       NOT NULL REFERENCES session_note (id) ON DELETE CASCADE,
    kind            VARCHAR(8)   NOT NULL CHECK (kind IN ('BEFORE', 'AFTER', 'PROGRESS')),
    file_name       VARCHAR(80)  NOT NULL,
    caption         VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_photo_session ON session_photo (tenant_id, session_note_id);

-- Consents -----------------------------------------------------------------------------

-- Text the patient accepts. A new version is a new row; old consents keep their version.
CREATE TABLE consent_template (
    id        BIGSERIAL    PRIMARY KEY,
    tenant_id BIGINT       NOT NULL REFERENCES tenant (id),
    type      VARCHAR(20)  NOT NULL CHECK (type IN ('DATA_PROCESSING', 'TREATMENT', 'PHOTO_TESTIMONIAL')),
    version   INT          NOT NULL,
    title     VARCHAR(160) NOT NULL,
    body      TEXT         NOT NULL,
    active    BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, type, version)
);

-- revoked_at set = no longer valid. channel: WEB (booking form), PORTAL (patient), IN_PERSON (staff)
CREATE TABLE consent (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL REFERENCES tenant (id),
    patient_id       BIGINT       NOT NULL REFERENCES patient (id) ON DELETE CASCADE,
    type             VARCHAR(20)  NOT NULL CHECK (type IN ('DATA_PROCESSING', 'TREATMENT', 'PHOTO_TESTIMONIAL')),
    template_version INT          NOT NULL,
    treatment_id     BIGINT       REFERENCES treatment (id),
    accepted_at      TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    channel          VARCHAR(10)  NOT NULL CHECK (channel IN ('WEB', 'PORTAL', 'IN_PERSON')),
    recorded_by      VARCHAR(120)
);
CREATE INDEX idx_consent_patient ON consent (tenant_id, patient_id, type);

-- Testimonials -------------------------------------------------------------------------

-- Shown on the public site only while PUBLISHED and the patient's PHOTO_TESTIMONIAL consent
-- is valid. Revoking it hides the testimonial and deletes its public photo copies.
CREATE TABLE testimonial (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL REFERENCES tenant (id),
    patient_id   BIGINT       NOT NULL REFERENCES patient (id) ON DELETE CASCADE,
    -- How the patient is named on the site ("Laura, 34 años"), never the full name by default
    display_name VARCHAR(80)  NOT NULL,
    quote        VARCHAR(600),
    treatment_id BIGINT       REFERENCES treatment (id),
    status       VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN')),
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- public_file: copy in uploads/public, made when publishing (metadata already stripped)
CREATE TABLE testimonial_photo (
    id               BIGSERIAL   PRIMARY KEY,
    tenant_id        BIGINT      NOT NULL REFERENCES tenant (id),
    testimonial_id   BIGINT      NOT NULL REFERENCES testimonial (id) ON DELETE CASCADE,
    session_photo_id BIGINT      NOT NULL REFERENCES session_photo (id) ON DELETE CASCADE,
    kind             VARCHAR(8)  NOT NULL,
    position         INT         NOT NULL DEFAULT 0,
    public_file      VARCHAR(80)
);

-- Audit --------------------------------------------------------------------------------

-- Every read or change of clinical data: panel, patient portal or MCP (assistant)
CREATE TABLE clinical_access_log (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL REFERENCES tenant (id),
    patient_id BIGINT       REFERENCES patient (id) ON DELETE SET NULL,
    actor_sub  VARCHAR(64)  NOT NULL,
    actor_name VARCHAR(150),
    channel    VARCHAR(8)   NOT NULL CHECK (channel IN ('PANEL', 'PORTAL', 'MCP')),
    action     VARCHAR(60)  NOT NULL,
    at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_access_patient ON clinical_access_log (tenant_id, patient_id, at DESC);
