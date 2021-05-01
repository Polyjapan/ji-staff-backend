# --- !Ups

drop table authorized_apps;

# --- !Downs

create table authorized_apps
(
    app_name varchar(40) not null
        primary key,
    app_key varchar(70) not null,
    constraint app_key
        unique (app_key)
)
    charset=utf8mb4;

