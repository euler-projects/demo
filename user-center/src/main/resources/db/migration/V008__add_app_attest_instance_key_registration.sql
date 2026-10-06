-- V008: Registry of the public keys an App Attest authenticated App instance registers
-- (org.eulerframework.security.authentication.appattest.JdbcAppAttestInstanceKeyRegistrationService).
--
-- Written by POST /app_attest/keys, which authenticates its caller with an App Attest
-- assertion and files the submitted public key under the App Attest KEY that proved it.
-- The trust a row here carries is exactly what App Attest proves - an unmodified
-- installation of a registered app, on genuine Apple hardware, generated this key and
-- holds its private half in the platform's secure area. What the key may then be used for
-- is left to whoever consumes it. Signing a jwt-bearer assertion is one such use, and the
-- reason this migration exists, but nothing in this table knows about it.
--
-- Which is why there is no client_id column. This is the App Attest domain rather than the
-- OAuth2 one, and the two are joined only by app_attest_attestation_registration.client_id
-- (V001): a consumer that needs to know which OAuth2 client an instance became resolves the
-- App Attest KEY there, and an instance that has not registered a client yet can still have
-- keys here.
--
-- Both key identifiers are qualified rather than being called key_id, because a row names
-- two keys in two different namespaces. app_attest_kid is
-- app_attest_attestation_registration.key_id - the KEY Apple issued, which the instance
-- authenticated with and which identifies the installation, one per instance. jwk_kid is
-- the kid of a public key that instance generated itself, of which it may register
-- several. Reading one for the other is the mistake these names exist to prevent.
--
-- The account side keeps no copy of the key. t_user_identity holds identity_type =
-- 'app_attest_instance_key' and subject = the same RFC 7638 thumbprint jwk_kid is, and that is
-- the whole of the binding: a login names its key by kid, checks it against the account's
-- subject, and reads the material from here. Two consequences worth stating where they will be
-- read - a row here must not be removed while an account is bound to the key it holds, and an
-- instance whose App Attest KEY is replaced has to register its keys again before the accounts
-- those keys opened can log in.
--
-- The composite primary key is what makes registration idempotent: jwk_kid is the RFC
-- 7638 thumbprint of jwk, derived by the server rather than supplied by the client, so
-- re-registering the same key lands on the same row instead of adding a second one.
--
-- jwk is TEXT rather than VARCHAR or JSON:
--   - VARCHAR would have to name a bound, and any bound narrow enough to be worth naming
--     can be exceeded by a key that the registration endpoint already accepted (it admits
--     up to 4 KiB). A rejected write is a 500 the client cannot act on, and a truncated
--     one is a row no later use can parse.
--   - JSON would validate the document but not store it verbatim: MySQL re-serialises on
--     read, sorting object keys and normalising whitespace, so what comes back is not what
--     went in. The service reads the row back after writing and compares, precisely so that
--     a store which cannot round-trip the value says so at registration time; a JSON column
--     would fail that comparison on every call. Well-formedness is already guaranteed on the
--     way in - the value written is nimbus' own JWK serialisation of a parsed key.
--   - TEXT stores verbatim, is bounded far above anything reachable here (64 KiB against a
--     4 KiB request limit), and is not a MySQL-specific type.
--
-- Column style, suffix and audit-column names match app_attest_attestation_registration (V001):
-- both are `app_attest_<registered thing>_registration` tables, and both leave the App instance
-- out of the name - there it is what the row is, here it is what the row hangs off, and in both
-- the primary key supplies it.

create table app_attest_instance_key_registration
(
    app_attest_kid varchar(255) not null comment 'app_attest_attestation_registration.key_id of the App Attest KEY that proved the registering instance',
    jwk_kid        varchar(255) not null comment 'kid of the registered public key: RFC 7638 JWK Thumbprint of jwk, server-derived',
    jwk            text         not null comment 'The public key as a JWK (RFC 7517), JSON, stored verbatim; carries the same kid',
    created_date   datetime(3)  not null comment 'Created time',
    modified_date  datetime(3)  not null comment 'Last modified time; differs from created_date once a key has been re-registered',
    primary key (app_attest_kid, jwk_kid)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Public keys registered by App Attest authenticated App instances';
