# --- !Ups

update staffs set staff_level = 1 where staff_level = 0;
alter table staffs alter column staff_level set default 1;


# --- !Downs

alter table staffs alter column staff_level set default 0;
update staffs set staff_level = 0 where staff_level = 1;





