-- V008: Child table for the public_key user-identity backend
-- (org.eulerframework.uc.service.identity.PublicKeyUserIdentityService).
--
-- The parent row on t_user_identity carries identity_type='public_key' and
-- subject=<RFC 7638 JWK Thumbprint of the key>, which is the cross-account unique
-- key and the value a jwt-bearer assertion names as its `kid`. This table stores
-- only the public key itself, as a JWK (RFC 7517) in JSON.
--
-- The key is public, so unlike t_user_identity_phone and t_user_identity_email it
-- is stored in clear. One row per identity: there is no key rotation, and a caller
-- who loses the private half opens a new account rather than replacing this key.
--
-- jwk is TEXT, matching app_attest_issued_key.jwk (V009), for the same reasons and with
-- one more: the two hold the same key at different points in its life, so a bound this
-- table could not hold but that one could would let a key register successfully and then
-- fail at the first login that tried to bind it to an account.
--
-- Column style, column order and audit-column names match
-- t_user_identity_phone (see V002__rename_factor_to_identity.sql).

create table t_user_identity_public_key
(
    identity_id   varchar(36)  not null comment 'FK to t_user_identity.identity_id',
    jwk           text         not null comment 'The registered public key as a JWK (RFC 7517), JSON, stored verbatim; its kid equals the parent subject',
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time',
    primary key (identity_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'User identity - public key details';
