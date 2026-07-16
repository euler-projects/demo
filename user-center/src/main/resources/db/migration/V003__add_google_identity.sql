-- V003: Child table for the google user-identity backend
-- (org.eulerframework.uc.service.identity.GoogleUserIdentityService).
--
-- The parent row on t_user_identity carries identity_type='google' and
-- subject=<raw google sub>. This table stores only the additional
-- profile snapshot returned by Google's UserInfo endpoint, so the
-- projection can be surfaced without re-hitting Google on every read.
--
-- Column style, column order and audit-column names match
-- t_user_identity_phone (see V002__rename_factor_to_identity.sql).

create table t_user_identity_google
(
    identity_id   varchar(36)  not null comment 'FK to t_user_identity.identity_id',
    email         varchar(255) null     comment 'Google-issued email (may be missing when the "email" scope is not granted)',
    name          varchar(255) null     comment 'Google-issued display name',
    picture       varchar(1024) null    comment 'Google-issued avatar URL',
    locale        varchar(32)  null     comment 'Google-issued BCP47 locale tag',
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time',
    primary key (identity_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'User identity - google details';
