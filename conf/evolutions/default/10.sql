# --- !Ups

alter table schedule_projects change max_hours_per_staff max_daily_hours int not null;

alter table schedule_projects
    add min_break_minutes int default 15 not null;


# --- !Downs

alter table schedule_projects change max_daily_hours max_hours_per_staff int not null;

alter table schedule_projects drop column `min_break_minutes`;


