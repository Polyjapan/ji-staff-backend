# --- !Ups

alter table fixed_task_slot_constraints change task_slot_id task_id int not null;

alter table fixed_task_slot_constraints drop foreign key fixed_task_slot_constraints_ibfk_3;

rename table fixed_task_slot_constraints to fixed_task_constraints;

alter table fixed_task_constraints
    add constraint fixed_task_slot_constraints_ibfk_3
        foreign key (task_id) references schedule_tasks (task_id)
            on delete cascade;


# --- !Downs

rename table fixed_task_constraints to fixed_task_slot_constraints;

alter table fixed_task_slot_constraints drop foreign key fixed_task_slot_constraints_ibfk_3;

alter table fixed_task_slot_constraints change task_partition_id task_slot_id int not null;

alter table fixed_task_slot_constraints
    add constraint fixed_task_slot_constraints_ibfk_3
        foreign key (task_slot_id) references schedule_tasks_slots (task_slot_id)
            on delete cascade;






