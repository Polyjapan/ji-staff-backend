# --- !Ups

alter table staffs
    add staff_level int default 0 not null;

create table staffs_capabilities
(
    event_id int null,
    staff_number int null,
    capability_id int null,
    constraint staffs_capabilities_pk
        primary key (event_id, staff_number, capability_id),
    constraint staffs_capabilities_events_event_id_fk
        foreign key (event_id) references events (event_id),
    constraint staffs_capabilities_schedule_capabilities_capability_id_fk
        foreign key (capability_id) references schedule_capabilities (capability_id)
            on update cascade on delete cascade,
    constraint staffs_capabilities_staffs_event_id_staff_number_fk
        foreign key (event_id, staff_number) references staffs (event_id, staff_number)
            on update cascade on delete cascade
);



# --- !Downs

drop table staffs_capabilities;

alter table staffs drop column staff_level;





