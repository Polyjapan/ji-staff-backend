# --- !Ups

create table `schedule_projects`
(
    `project_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `event_id` INTEGER NOT NULL,
    `project_title` VARCHAR(100) NOT NULL,
    `max_hours_per_staff` INTEGER NOT NULL,

    FOREIGN KEY (`event_id`) REFERENCES events (`event_id`)
);

create table `schedule_tasks`
(
    `task_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `min_age` INTEGER NOT NULL,
    `min_experience` INTEGER NOT NULL,
    `name` VARCHAR(100) NOT NULL,

    FOREIGN KEY (`project_id`) REFERENCES schedule_projects (`project_id`) ON DELETE CASCADE
);

create table `schedule_capabilities`
(
    `capability_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL
);


create table `task_capabilities`
(
    `capability_id` INTEGER NOT NULL,
    `task_id` INTEGER NOT NULL,

    PRIMARY KEY (capability_id, task_id)
);


create table `schedule_task_partitions`
(
    `task_partition_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `task_id` INTEGER NOT NULL,
    `split_in` INTEGER NOT NULL,
    `staffs_required` INTEGER NOT NULL,
    `day` DATE NOT NULL,
    `start` TIME NOT NULL,
    `end` TIME NOT NULL,
    `allow_alternate` BOOLEAN,
    `first_starts_later` BOOLEAN,
    `first_ends_earlier` BOOLEAN,
    `last_ends_earlier` BOOLEAN,
    `last_ends_later` BOOLEAN,

    FOREIGN KEY (`task_id`) REFERENCES `schedule_tasks` (`task_id`) ON DELETE CASCADE
);

create table `schedule_tasks_slots`
(
    `task_slot_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `task_id` INTEGER NOT NULL,
    `staffs_required` INTEGER NOT NULL,
    `day` DATE NOT NULL,
    `start` TIME NOT NULL,
    `end` TIME NOT NULL,

        FOREIGN KEY (`task_id`) REFERENCES schedule_tasks (`task_id`) ON DELETE CASCADE

);

create table `schedule_staffs_assignation`
(
    `task_slot_id` INTEGER NOT NULL,
    `user_id` INTEGER NOT NULL,

    PRIMARY KEY (task_slot_id, user_id),
    FOREIGN KEY (task_slot_id) REFERENCES schedule_tasks_slots(task_slot_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

create table `fixed_task_constraints`
(
    `constraint_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `staff_id` INTEGER NOT NULL,
    `task_id` INTEGER NOT NULL,


    FOREIGN KEY (project_id) REFERENCES schedule_projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id),
    FOREIGN KEY (task_id) REFERENCES schedule_tasks(task_id) ON DELETE CASCADE
);

create table `fixed_task_slot_constraints`
(
    `constraint_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `staff_id` INTEGER NOT NULL,
    `task_slot_id` INTEGER NOT NULL,

    FOREIGN KEY (project_id) REFERENCES schedule_projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id),
    FOREIGN KEY (task_slot_id) REFERENCES schedule_tasks_slots(task_slot_id) ON DELETE CASCADE
);

create table `unavailable_constraints`
(
    `constraint_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `staff_id` INTEGER NOT NULL,

    `day` DATE NOT NULL,
    `start` TIME NOT NULL,
    `end` TIME NOT NULL,

    FOREIGN KEY (project_id) REFERENCES schedule_projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id)
);

create table `association_constraints`
(
    `constraint_id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `project_id` INTEGER NOT NULL,
    `staff_1` INTEGER NOT NULL,
    `staff_2` INTEGER NOT NULL,
    `together` BOOLEAN NOT NULL,

    FOREIGN KEY (project_id) REFERENCES schedule_projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_1) REFERENCES users(user_id),
    FOREIGN KEY (staff_2) REFERENCES users(user_id)

);

# --- !Downs

drop table association_constraints;
drop table unavailable_constraints;
drop table fixed_task_constraints;
drop table fixed_task_slot_constraints;
drop table schedule_staffs_assignation;
drop table schedule_tasks_slots;
drop table schedule_task_partitions;
drop table task_capabilities;
drop table schedule_capabilities;
drop table schedule_tasks;
drop table schedule_projects;



