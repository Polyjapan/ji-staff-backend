# --- !Ups

create table task_types
(
    task_type_id int auto_increment,
    task_type_name varchar(150) null,
    constraint task_types_pk
        primary key (task_type_id)
);

create unique index task_types_task_type_name_uindex
    on task_types (task_type_name);

alter table schedule_tasks
    add task_type_id int null;

alter table schedule_tasks
    add constraint schedule_tasks_task_types_task_type_id_fk
        foreign key (task_type_id) references task_types (task_type_id)
            on update cascade on delete set null;


# --- !Downs

alter table schedule_tasks
    drop constraint schedule_tasks_task_types_task_type_id_fk;

alter table schedule_tasks
    drop column task_type_id;

drop table task_types;




