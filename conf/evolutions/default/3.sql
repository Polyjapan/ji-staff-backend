# --- !Ups

alter table form_pages modify ordering int null default null;

# --- !Downs

alter table form_pages modify ordering int default 0 not null;
