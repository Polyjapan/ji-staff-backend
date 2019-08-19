# --- !Ups


create table `users`
(
    `user_id`    INTEGER      NOT NULL PRIMARY KEY,
    `first_name` VARCHAR(50)  NOT NULL,
    `last_name`  VARCHAR(50)  NOT NULL,
    `birth_date` DATE         NOT NULL,
    `phone`      VARCHAR(20)  NOT NULL,
    'address'    VARCHAR(250) NOT NULL
);

create table `events`
(
    `id`        INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `end_date`  DATETIME     NOT NULL,
    `name`      VARCHAR(100) NOT NULL,
    `main_form` INT          NULL
);

create table `fields`
(
    `id`        INTEGER                                                                                  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name`      VARCHAR(100)                                                                             NOT NULL,
    `label`     VARCHAR(150)                                                                             NOT NULL,
    `help_text` TEXT                                                                                     NULL,
    `required`  BOOLEAN,
    `type`      SET ('text', 'long_text', 'email', 'date', 'checkbox', 'select', 'file', 'image', 'url') NOT NULL
);

create table `fields_additional`
(
    `field` INTEGER      NOT NULL,
    `key`   VARCHAR(200) NOT NULL,
    `value` VARCHAR(200) NOT NULL,

    PRIMARY KEY (`field`, `key`),
    FOREIGN KEY (`field`) REFERENCES `fields` (`id`)
);

create table `forms`
(
    `id`               INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `event`            INTEGER      NOT NULL,
    `internal_name`    VARCHAR(100) NOT NULL,
    `name`             VARCHAR(100) NOT NULL,
    `shortDescription` TEXT         NOT NULL,
    `description`      TEXT         NOT NULL,
    `max_age`          INT          NOT NULL DEFAULT 200,
    `min_age`          INT          NOT NULL DEFAULT -1,
    -- `required_group`   VARCHAR(100) NULL,
    `requires_staff`   BOOLEAN      NOT NULL DEFAULT FALSE,
    `hidden`           BOOLEAN,

    FOREIGN KEY (`event`) REFERENCES `events` (`id`),
    UNIQUE KEY (`event`, `internal_name`)
);


create table `forms_fields`
(
    `field_id` INTEGER NOT NULL PRIMARY KEY,
    `form_id`  INTEGER NOT NULL,
    `hidden`   BOOLEAN,


    FOREIGN KEY (`field_id`) REFERENCES `fields` (`id`),
    FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`)
);


create table `applications`
(
    `id`      INTEGER                                                           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INTEGER                                                           NULL,
    `form_id` INTEGER                                                           NOT NULL,
    `state`   SET ('draft', 'sent', 'requested_changes', 'accepted', 'refused') NOT NULL DEFAULT 'draft',

    FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`)
);

create table `applications_contents`
(
    `application_id` INTEGER NOT NULL,
    `field_id`       INTEGER NOT NULL,
    `value`          TEXT    NOT NULL,

    PRIMARY KEY (`application_id`, `field_id`),
    FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`),
    FOREIGN KEY (`field_id`) REFERENCES `fields` (`id`)
);

create table `applications_comments`
(
    `id`             INTEGER PRIMARY KEY AUTO_INCREMENT,
    `application_id` INTEGER   NOT NULL,
    `user_id`        INTEGER   NOT NULL,
    `timestamp`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `value`          TEXT      NOT NULL,
    FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);

create table `staffs`
(
    `event_id`     INTEGER NOT NULL,
    `staff_number` INTEGER NOT NULL,
    `user_id`      INTEGER NOT NULL,

    PRIMARY KEY (`event_id`, `staff_number`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
);


# --- !Downs

drop table staffs;

drop table applications_comments;
drop table applications_contents;
drop table applications;

drop table forms_fields;
drop table forms;

drop table fields_additional;
drop table fields;

drop table events;

drop table users;