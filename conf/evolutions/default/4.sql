# --- !Ups

alter table fields change label placeholder varchar(150) not null;

# --- !Downs

alter table fields change placeholder label varchar(150) not null;
