-- Rename the legacy "authentication factor" tables in place to the
-- "user identity" terminology. The domain has converged on a single
-- aggregate (UserIdentity) — the factor concept was abandoned together
-- with its SPI, exception family and per-factor backend services. The
-- pre-existing schema was already structurally identical to what the
-- identity model needs; this migration is therefore a pure rename +
-- minor column adjustments, with zero data movement.
--
-- Column mapping for t_user_authentication_factor → t_user_identity:
--   factor_id        → identity_id
--   user_id          → user_id           (unchanged)
--   factor_type      → identity_type     (values already 'phone' / etc.)
--   identifier       → subject           (the SHA-256 hex written by the
--                                         old factor backend was already
--                                         the cross-account uniqueness
--                                         key — i.e. the new subject)
--   bound_at         → bound_at          (unchanged)
--   last_verified_at → dropped           (factor-specific verifier hook;
--                                         identity has no equivalent)
--
-- Column mapping for t_user_authentication_factor_phone → t_user_identity_phone:
--   factor_id → identity_id
--   phone     → phone (widened from TEXT to VARCHAR(512) to host the
--                      AES-256-GCM envelope of an E.164 number)

rename table t_user_authentication_factor to t_user_identity,
             t_user_authentication_factor_phone to t_user_identity_phone;

-- t_user_identity: rename columns, drop the factor-only verifier hook,
-- shrink the subject column to 128 (SHA-256 hex is 64 chars; IdP-issued
-- openid / sub values fit comfortably), refresh the unique constraint
-- and the user-scoped lookup index.
alter table t_user_identity
    change column factor_id   identity_id   varchar(36)  not null comment 'UUID, framework-generated',
    change column factor_type identity_type varchar(32)  not null comment 'phone / email / wechat / apple / google / ...',
    change column identifier  subject       varchar(128) not null comment 'Deterministic per-type unique key: SHA-256 hex for phone/email; openid/sub for federated identities',
    drop column last_verified_at,
    drop index uk_user_authentication_factor_type_identifier,
    add constraint uk_user_identity_type_subject unique (identity_type, subject),
    drop index idx_user_authentication_factor_user_type,
    add index idx_user_identity_user_type (user_id, identity_type),
    comment = 'User identities (parent table)';

-- t_user_identity_phone: rename PK column to identity_id and widen the
-- encrypted phone column to varchar(512) so it can host the AES-256-GCM
-- envelope produced by EncryptedEnvelopeCodec.
alter table t_user_identity_phone
    change column factor_id identity_id varchar(36)  not null comment 'FK to t_user_identity.identity_id',
    modify column phone                 varchar(512) not null comment 'Encrypted E.164 phone number (AES-256-GCM envelope, see EncryptedEnvelopeCodec)',
    comment = 'User identity - phone details';
