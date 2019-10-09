# --- !Ups

alter table users
    drop column email;

alter table users
    drop column first_name;

alter table users
    drop column last_name;

alter table users
    drop column phone;

alter table users
    drop column address;

# --- !Downs

alter table users
    add `email` VARCHAR(200) NOT NULL;

alter table users
    add `first_name` VARCHAR(50) NOT NULL;

alter table users
    add `last_name` VARCHAR(50) NOT NULL;

alter table users
    add `phone` VARCHAR(20) NOT NULL;

alter table users
    add `address` VARCHAR(250) NOT NULL;
