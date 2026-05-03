create database if not exists :schema_name
    default character set utf8mb4
    default collate utf8mb4_bin;

use :schema_name;

create table t_user
(
    id                         varchar(36)  not null,
    username                   varchar(255) not null,
    password                   varchar(255) null,
    email                      varchar(255) null,
    phone                      varchar(16)  null,
    is_account_non_expired     bit          not null,
    is_account_non_locked      bit          not null,
    is_credentials_non_expired bit          not null,
    is_enabled                 bit          not null,
    created_date               datetime(3)  not null comment 'Created time',
    modified_date              datetime(3)  not null comment 'Last modified time',
    primary key (id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Users';

create table t_user_authority
(
    id            bigint auto_increment,
    user_id       varchar(36)  null,
    authority     varchar(255) null,
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time',
    primary key (id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'User authorities';

create table t_authority
(
    authority     varchar(255) not null,
    name          varchar(255) not null,
    description   varchar(255) not null,
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time',
    primary key (authority)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Authorities';

INSERT INTO t_user (id, username, password, email, phone, is_account_non_expired, is_account_non_locked,
                    is_credentials_non_expired, is_enabled, created_date, modified_date)
VALUES ('00000000-0000-0000-0000-000000000000', 'root', null, null, null, true, false, false, false, now(), now());
INSERT INTO t_user (id, username, password, email, phone, is_account_non_expired, is_account_non_locked,
                    is_credentials_non_expired, is_enabled, created_date, modified_date)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin', null, null, null, true, false, false, false, now(), now());
INSERT INTO t_user (id, username, password, email, phone, is_account_non_expired, is_account_non_locked,
                    is_credentials_non_expired, is_enabled, created_date, modified_date)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'user', null, null, null, true, false, false, false, now(), now());

INSERT INTO t_authority (authority, name, description, created_date, modified_date)
VALUES ('root', 'ROOT', 'Full system access', now(), now());
INSERT INTO t_authority (authority, name, description, created_date, modified_date)
VALUES ('admin', 'Admin', 'Administrative access', now(), now());
INSERT INTO t_authority (authority, name, description, created_date, modified_date)
VALUES ('user', 'User', 'Standard user access', now(), now());

INSERT INTO t_user_authority (user_id, authority, created_date, modified_date)
VALUES ('00000000-0000-0000-0000-000000000000', 'root', now(), now());
INSERT INTO t_user_authority (user_id, authority, created_date, modified_date)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin', now(), now());
INSERT INTO t_user_authority (user_id, authority, created_date, modified_date)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'user', now(), now());

create table t_wechat_user_mapping
(
    open_id       varchar(36)   not null,
    user_id       varchar(36)   not null,
    union_id      varchar(36)   null,
    nick_name     varchar(255)  null,
    gender        int unsigned  null,
    city          varchar(1024) null,
    province      varchar(128)  null,
    country       varchar(128)  null,
    avatar_url    varchar(2048) null,
    created_date  datetime(3)   not null comment 'Created time',
    modified_date datetime(3)   not null comment 'Last modified time',
    primary key (open_id),
    unique uk_wechat_user_mapping_user_id (union_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'WeChat user mappings';

create table app_attest_app_registration
(
    id                 varchar(64)  not null,
    app_id             varchar(288) not null comment 'teamId.bundleId',
    app_id_hash        varchar(64)  not null comment 'hex-encoded SHA-256 of app_id',
    team_id            varchar(32)  not null comment 'Apple Developer Team ID',
    bundle_id          varchar(255) not null comment 'Apple App Bundle ID',
    oauth2_enabled     bit          not null,
    oauth2_client_type varchar(16)  null,
    created_date       datetime(3)  not null comment 'Created time',
    modified_date      datetime(3)  not null comment 'Last modified time',
    primary key (id),
    unique uk_app_app_id (app_id),
    unique uk_app_app_id_hash (app_id_hash)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'Registered Apps for App Attest';

CREATE TABLE app_attest_attestation_registration
(
    key_id                        VARCHAR(255) NOT NULL PRIMARY KEY,
    team_id                       VARCHAR(255) NOT NULL,
    bundle_id                     VARCHAR(255) NOT NULL,
    client_id                     VARCHAR(255) NULL,
    aaguid                        BLOB         NOT NULL,
    credential_id                 BLOB         NOT NULL,
    attestation_certificate_chain BLOB         NOT NULL,
    receipt                       BLOB         NOT NULL,
    public_key                    BLOB         NOT NULL,
    jwks                          TEXT         NOT NULL,
    sign_count                    BIGINT       NOT NULL,
    created_date                  DATETIME(3)  NOT NULL COMMENT 'Created time',
    modified_date                 DATETIME(3)  NOT NULL COMMENT 'Last modified time'
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'App Attest Attestation registrations';

create table app_attest_attestation_user_mapping
(
    key_id        varchar(255) not null comment 'App Attest Key ID',
    user_id       varchar(36)  not null,
    team_id       varchar(20)  not null comment 'Apple Developer Team ID',
    bundle_id     varchar(255) not null comment 'Apple App Bundle ID',
    created_date  datetime(3)  not null comment 'Created time',
    modified_date datetime(3)  not null comment 'Last modified time',
    primary key (key_id),
    unique uk_app_attest_attestation_user_mapping_user_id (user_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'App Attest Attestation user mappings';

create table oauth2_client
(
    id                                         varchar(36)  not null,
    client_id                                  varchar(255) not null,
    client_id_issued_at                        datetime(3)  null,
    client_secret                              varchar(255) null,
    client_secret_expires_at                   datetime(3)  null,
    client_name                                varchar(255) null,
    token_endpoint_auth_method                 varchar(255) null,
    grant_types                                varchar(255) null,
    response_types                             varchar(255) null,
    redirect_uris                              varchar(255) null,
    post_logout_redirect_uris                  varchar(255) null,
    scopes                                     varchar(255) null,
    jwk_set_url                                varchar(255) null,
    jwks                                       json         null,
    token_endpoint_auth_signing_alg            varchar(255) null,
    id_token_signed_response_alg               varchar(255) null,
    tls_client_auth_subject_dn                 varchar(255) null,
    tls_client_certificate_bound_access_tokens bit          null,
    client_settings                            json         null,
    token_settings                             json         null,
    created_date                               datetime(3)  not null comment 'Created time',
    modified_date                              datetime(3)  not null comment 'Last modified time',
    primary key (id),
    unique uk_oauth2_client_client_id (client_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment 'OAuth2 Clients';
