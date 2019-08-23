# --- !Ups

create table `authorized_apps`
(
    `app_name` VARCHAR(40) NOT NULL PRIMARY KEY,
    `app_key`  VARCHAR(70) NOT NULL UNIQUE KEY
);

create table `users`
(
    `user_id`    INTEGER      NOT NULL PRIMARY KEY,
    `email`      VARCHAR(200) NOT NULL,
    `first_name` VARCHAR(50)  NOT NULL,
    `last_name`  VARCHAR(50)  NOT NULL,
    `birth_date` DATE         NOT NULL,
    `phone`      VARCHAR(20)  NOT NULL,
    `address`    VARCHAR(250) NOT NULL
);

create table `events`
(
    `event_id`    INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `close_date`  DATETIME     NOT NULL,
    `event_begin` DATE         NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `main_form`   INT          NULL
);


create table `forms`
(
    `form_id`           INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `event`             INTEGER      NOT NULL,
    `internal_name`     VARCHAR(100) NOT NULL,
    `name`              VARCHAR(100) NOT NULL,
    `short_description` TEXT         NOT NULL,
    `description`       TEXT         NOT NULL,
    `max_age`           INT          NOT NULL DEFAULT 200,
    `min_age`           INT          NOT NULL DEFAULT -1,
    -- `required_group`   VARCHAR(100) NULL,
    `requires_staff`    BOOLEAN      NOT NULL DEFAULT FALSE,
    `hidden`            BOOLEAN,

    FOREIGN KEY (`event`) REFERENCES `events` (`event_id`),
    UNIQUE KEY (`event`, `internal_name`)
);

create table `form_pages`
(
    `form_page_id` INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `form`         INTEGER      NOT NULL,
    `name`         VARCHAR(100) NOT NULL,
    `description`  TEXT         NOT NULL,
    `max_age`      INT          NOT NULL DEFAULT 200,
    `min_age`      INT          NOT NULL DEFAULT -1,
    `ordering`     INT          NOT NULL DEFAULT 0,

    FOREIGN KEY (`form`) REFERENCES `forms` (`form_id`)
);

create table `fields`
(
    `field_id`     INTEGER                                                                                  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `form_page_id` INTEGER                                                                                  NOT NULL,
    `name`         VARCHAR(100)                                                                             NOT NULL,
    `label`        VARCHAR(150)                                                                             NOT NULL,
    `help_text`    TEXT                                                                                     NULL,
    `required`     BOOLEAN,
    `type`         SET ('text', 'long_text', 'email', 'date', 'checkbox', 'select', 'file', 'image', 'url') NOT NULL,
    `ordering`     INT                                                                                      NULL,

    FOREIGN KEY (`form_page_id`) REFERENCES `form_pages` (`form_page_id`)
);

create table `fields_additional`
(
    `field_id` INTEGER      NOT NULL,
    `key`      VARCHAR(30) NOT NULL,
    `value`    VARCHAR(200) NOT NULL,

    PRIMARY KEY (`field_id`, `key`),
    FOREIGN KEY (`field_id`) REFERENCES `fields` (`field_id`)
);
create table `applications`
(
    `application_id` INTEGER                                      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`        INTEGER                                      NULL,
    `form_id`        INTEGER                                      NOT NULL,
    `state`          SET ('draft', 'sent', 'accepted', 'refused') NOT NULL DEFAULT 'draft',

    FOREIGN KEY (`form_id`) REFERENCES `forms` (`form_id`)
);

create table `applications_contents`
(
    `application_id` INTEGER NOT NULL,
    `field_id`       INTEGER NOT NULL,
    `value`          TEXT    NOT NULL,

    PRIMARY KEY (`application_id`, `field_id`),
    FOREIGN KEY (`application_id`) REFERENCES `applications` (`application_id`),
    FOREIGN KEY (`field_id`) REFERENCES `fields` (`field_id`)
);

create table `applications_comments`
(
    `application_comment_id` INTEGER PRIMARY KEY AUTO_INCREMENT,
    `application_id`         INTEGER   NOT NULL,
    `user_id`                INTEGER   NOT NULL,
    `timestamp`              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `value`                  TEXT      NOT NULL,
    FOREIGN KEY (`application_id`) REFERENCES `applications` (`application_id`),
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

drop table authorized_apps;
drop table staffs;

drop table applications_comments;
drop table applications_contents;
drop table applications;

drop table form_pages;
drop table forms;
drop table fields_additional;
drop table fields;

drop table events;

drop table users;