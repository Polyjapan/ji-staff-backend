# --- !Ups

alter table schedule_projects
    add max_same_shift_daily int default 2 not null;


# --- !Downs

alter table schedule_projects drop column `max_same_shift_daily`;