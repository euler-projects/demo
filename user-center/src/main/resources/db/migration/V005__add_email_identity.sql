-- V005: Child table for the email user-identity backend
-- (org.eulerframework.uc.service.identity.EmailUserIdentityService).
--
-- The parent row on t_user_identity carries identity_type='email' and
-- subject=<SHA-256 hex of the normalised (trimmed, lower-cased) email>.
-- This table stores only the AES-256-GCM-encrypted original address used
-- for email-OTP delivery and masked-display projection.
--
-- Column style, column order and audit-column names match
-- t_user_identity_phone (see V002__rename_factor_to_identity.sql).

create table t_user_identity_email
(
    identity_id   varchar(36)   not null comment 'FK to t_user_identity.identity_id',
    email         varchar(1024) not null comment 'Encrypted envelope of the original email address',
    created_date  datetime(3)   not null comment 'Created time',
    modified_date datetime(3)   not null comment 'Last modified time',
    primary key (identity_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'User identity - email details';
