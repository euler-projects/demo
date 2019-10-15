create table cdb_user_profile
(
  user_id            varchar(36)  not null
    primary key,
  full_name          varchar(100) not null,
  gender             varchar(6)   not null,
  year_of_enrollment varchar(4)   not null,
  faculty            varchar(100) not null,
  study_class        varchar(100) not null
);