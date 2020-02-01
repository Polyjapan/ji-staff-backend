# --- !Ups

alter table fixed_task_constraints
    add day DATE default NULL null,
    add start TIME default NULL null,
    add end TIME default NULL null;


# --- !Downs

alter table fixed_task_constraints
    drop column day,
    drop column start,
    drop column end;