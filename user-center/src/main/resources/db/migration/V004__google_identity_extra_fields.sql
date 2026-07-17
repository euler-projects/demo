-- V004: Add given_name, family_name, email_verified to the google
-- identity snapshot table. These fields are refreshed on every
-- successful login so the local profile stays eventually consistent
-- with the upstream Google account.

alter table t_user_identity_google
    add column email_verified tinyint(1) null comment 'Whether Google has verified the email' after email,
    add column given_name     varchar(255) null comment 'Google-issued given (first) name' after name,
    add column family_name    varchar(255) null comment 'Google-issued family (last) name' after given_name;
