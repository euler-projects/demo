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
-- jwk_kid is chosen by the registering instance and is opaque: a handle on this row and
-- nothing more. It is deliberately not the key's RFC 7638 thumbprint, so that it stays a
-- plain identifier and makes no second statement about the key that a reader could mistake
-- for the one that matters. What binds an account to a key is that thumbprint, which the
-- account's own identity row derives and stores; what verifies a signature is the key
-- material. The identifier takes part in neither.
--
-- It is unique across the whole registry rather than within one instance, so one identifier
-- means one key everywhere. A collision is refused rather than settled by overwriting: the
-- row already there may belong to another instance, and replacing it could discard the key
-- an account is bound to, leaving that account unreachable with nothing recording why. Rows
-- are therefore immutable once written - which is also why there is no modified_date, and
-- why nothing in a row is rewritable by whoever can authenticate as its instance.
--
-- The account side keeps no copy of the key. t_user_identity holds identity_type =
-- 'app_attest_instance_key' and subject = the RFC 7638 thumbprint of the key, and that is
-- the whole of the binding: a login names its key by kid, resolves the material from here,
-- and compares that material's thumbprint with the account's subject. Two consequences
-- worth stating where they will be read - a row here must not be removed while an account
-- is bound to the key it holds, and an instance whose App Attest KEY is replaced has to
-- register its keys again before the accounts those keys opened can log in.
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
-- jwk_kid is varchar(128), matching oauth2_jwk.kid (V001), which is likewise a caller-chosen
-- identifier rather than a derived one. The registration endpoint enforces the same bound and
-- also refuses control characters, since the value is echoed into a JSON response and written
-- into log lines.
--
-- Table naming follows app_attest_attestation_registration (V001): both are
-- `app_attest_<registered thing>_registration` tables, and both leave the App instance out of
-- the name - there it is what the row is, here it is what the row hangs off, and in both a
-- column supplies it.

create table app_attest_instance_key_registration
(
    jwk_kid        varchar(128) not null comment 'kid of the registered public key, chosen by the registering instance; opaque, globally unique, never reassigned',
    app_attest_kid varchar(255) not null comment 'app_attest_attestation_registration.key_id of the App Attest KEY that proved the registering instance',
    jwk            text         not null comment 'The public key as a JWK (RFC 7517), JSON, stored verbatim; its kid member equals jwk_kid',
    created_date   datetime(3)  not null comment 'Created time',
    primary key (jwk_kid),
    key idx_app_attest_instance_key_app_attest_kid (app_attest_kid)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Public keys registered by App Attest authenticated App instances';
