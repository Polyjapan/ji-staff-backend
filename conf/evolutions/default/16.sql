# --- !Ups

alter table schedule_task_partitions
    add alternate_offset int default null null;

alter table schedule_task_partitions
    add first_alternated_shift SET('shorter', 'longer', 'removed') default 'shorter' not null;

alter table schedule_task_partitions
    add last_alternated_shift SET('shorter', 'longer', 'removed') default 'longer' not null;

alter table schedule_task_partitions
    add last_normal_shift SET('shorter', 'longer', 'removed') default 'shorter' not null;



# --- !Downs

alter table schedule_task_partitions drop column `alternate_offset`;
alter table schedule_task_partitions drop column `first_alternated_shift`;
alter table schedule_task_partitions drop column `last_alternated_shift`;
alter table schedule_task_partitions drop column `last_normal_shift`;
