# --- !Ups

alter table fields_additional
    add ordering int default 0 null;

alter table fields_additional drop primary key;

alter table fields_additional drop column `key`;

alter table fields_additional
    add primary key (field_id, value);

create index fields_additional_field_id_ordering_index
    on fields_additional (field_id, ordering);

# --- !Downs

drop index fields_additional_field_id_ordering_index on fields_additional;

alter table fields_additional
    add `key` VARCHAR(30) NOT NULL;

alter table fields_additional drop primary key;

alter table fields_additional drop column `ordering`;

alter table fields_additional
    add primary key (field_id, `key`);

