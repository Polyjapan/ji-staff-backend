# --- !Ups

create table schedule_versions
(
    version_id int auto_increment,
    project_id int not null,
    generation_time DATETIME default CURRENT_TIMESTAMP not null,
    tag varchar(250) null,
    visible boolean not null default 0,
    constraint schedule_versions_pk
        primary key (version_id),
    constraint schedule_versions_schedule_projects_project_id_fk
        foreign key (project_id) references schedule_projects (project_id)
            on update cascade on delete cascade
);

-- We will need to stop deleting tasks for real to avoid breaking old generated schedules
alter table schedule_tasks
    add deleted bool default 0 not null;

truncate schedule_staffs_assignation; -- forced to truncate the next one
delete from schedule_tasks_slots where 1; -- not a big deal, we can regen them quickly

alter table schedule_tasks_slots
    add version_id int null after task_id;

alter table schedule_tasks_slots
    add constraint schedule_tasks_slots_schedule_versions_version_id_fk
        foreign key (version_id) references schedule_versions (version_id)
            on update cascade on delete cascade;

# --- !Downs
alter table schedule_tasks_slots drop foreign key schedule_tasks_slots_schedule_versions_version_id_fk;

alter table schedule_tasks_slots drop column version_id;

drop table schedule_versions;

alter table schedule_tasks
    drop column deleted;


