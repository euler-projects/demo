create database if not exists euler_uc
    default character set utf8mb4
    default collate utf8mb4_bin;

use euler_uc;

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
    created_date               datetime(3)  not null comment '资源创建时间',
    modified_date              datetime(3)  not null comment '资源最后修改时间',
    primary key (id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment '用户表';

create table t_user_authority
(
    id            bigint auto_increment,
    user_id       varchar(36)  null,
    authority     varchar(255) null,
    created_date  datetime(3)  not null comment '资源创建时间',
    modified_date datetime(3)  not null comment '资源最后修改时间',
    primary key (id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment '用户权限表';

create table t_authority
(
    authority     varchar(255) not null,
    name          varchar(255) not null,
    description   varchar(255) not null,
    created_date  datetime(3)  not null comment '资源创建时间',
    modified_date datetime(3)  not null comment '资源最后修改时间',
    primary key (authority)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment '权限表';

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
VALUES ('root', 'ROOT', '超级权限', now(), now());
INSERT INTO t_authority (authority, name, description, created_date, modified_date)
VALUES ('admin', '管理员', '管理员权限', now(), now());
INSERT INTO t_authority (authority, name, description, created_date, modified_date)
VALUES ('user', '普通用户', '普通用户权限', now(), now());

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
    created_date  datetime(3)   not null comment '资源创建时间',
    modified_date datetime(3)   not null comment '资源最后修改时间',
    primary key (open_id),
    unique uk_wechat_user_mapping_user_id (union_id)
) engine = innodb
  default character set utf8mb4
  default collate utf8mb4_bin
    comment '微信用户到本地用户映射表';
