-- V009: Registry of the public keys an App Attest App instance issues
-- (org.eulerframework.security.authentication.appattest.JdbcAppAttestIssuedKeyService).
--
-- Written by POST /app_attest/keys, which authenticates its caller with an App Attest
-- assertion and registers the submitted public key under the client_id bound to that
-- App Attest KEY. That client_id is the JWT `iss` a jwt-bearer assertion signed by this
-- key has to name, and this table is what the authorization server resolves the
-- assertion's signature against.
--
-- Deliberately separate from t_user_identity_public_key (V008): this row says "this
-- issuer vouches for this key" and is consulted only when a first login has to find an
-- account from the key alone, while the identity row says "this account owns this key"
-- and is what every later login verifies against. Keeping the two apart is what lets an
-- account survive its issuer's App Attest KEY being revoked and re-registered under a
-- new client_id.
--
-- The composite primary key is what makes registration idempotent: key_id is the RFC
-- 7638 thumbprint of jwk, derived by the server rather than supplied by the client, so
-- re-registering the same key lands on the same row instead of adding a second one.
--
-- jwk is TEXT rather than VARCHAR or JSON:
--   - VARCHAR would have to name a bound, and any bound narrow enough to be worth naming
--     can be exceeded by a key that the registration endpoint already accepted (it admits
--     up to 4 KiB). A rejected write is a 500 the client cannot act on, and a truncated
--     one is a row no later login can parse.
--   - JSON would validate the document but not store it verbatim: MySQL re-serialises on
--     read, sorting object keys and normalising whitespace, so what comes back is not what
--     went in. The service reads the row back after writing and compares, precisely so that
--     a store which cannot round-trip the value says so at registration time; a JSON column
--     would fail that comparison on every call. Well-formedness is already guaranteed on the
--     way in - the value written is nimbus' own JWK serialisation of a parsed key.
--   - TEXT stores verbatim, is bounded far above anything reachable here (64 KiB against a
--     4 KiB request limit), and is not a MySQL-specific type.
--
-- Column style and audit-column names match app_attest_app_registration (V001).

create table app_attest_issued_key
(
    issuer        varchar(255) not null comment 'JWT iss the key is registered under: the client_id bound to the App Attest KEY',
    key_id        varchar(255) not null comment 'JWT kid: RFC 7638 JWK Thumbprint of jwk, server-derived',
    jwk           text         not null comment 'The public key as a JWK (RFC 7517), JSON, stored verbatim; carries the same kid',
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time; differs from created_date once a key has been re-registered',
    primary key (issuer, key_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Public keys issued by App Attest App instances, for jwt-bearer assertions';
