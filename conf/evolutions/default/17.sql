# --- !Ups

create table `banned_task_types_constraints`
(
    `constraint_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `staff_id` INTEGER NOT NULL,
    `task_type_id` INTEGER NOT NULL,


    FOREIGN KEY (project_id) REFERENCES schedule_projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id),
    FOREIGN KEY (task_type_id) REFERENCES task_types(task_type_id) ON DELETE CASCADE
);

# --- !Downs

drop table banned_task_types_constraints;