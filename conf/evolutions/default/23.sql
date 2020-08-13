# --- !Ups

alter table forms add is_main boolean default false not null after event;

update forms inner join (select main_form, event_id from events) as t on t.event_id = event_id
    set forms.is_main = (main_form = forms.form_id)
    where 1;

alter table forms drop foreign key forms_ibfk_1;

alter table schedule_projects drop foreign key schedule_projects_ibfk_1;

alter table meals drop foreign key meals_events_event_id_fk;

alter table staff_arrivals_logs drop foreign key staff_arrivals_logs_events_event_id_fk;

alter table staff_food_particularities drop foreign key staff_food_particularities_events_event_id_fk;

alter table staffs_capabilities drop foreign key staffs_capabilities_events_event_id_fk;

drop table events;

# --- !Downs

create table events
(
    event_id int auto_increment
        primary key,
    event_begin date not null,
    name varchar(100) not null,
    main_form int null,
    is_active tinyint(1) default 0 null
)
    charset=utf8mb4;

alter table forms drop column is_main;

alter table schedule_projects
    add constraint schedule_projects_ibfk_1
        foreign key (event_id) references events (event_id);

alter table forms
    add constraint forms_ibfk_1
        foreign key (event) references events (event_id);

alter table meals
    add constraint meals_events_event_id_fk
        foreign key (event_id) references events (event_id);

alter table staff_arrivals_logs
    add constraint staff_arrivals_logs_events_event_id_fk
        foreign key (event_id) references events (event_id);

alter table staff_food_particularities
    add constraint staff_food_particularities_events_event_id_fk
        foreign key (event_id) references events (event_id);

alter table staffs_capabilities
    add constraint staffs_capabilities_events_event_id_fk
        foreign key (event_id) references events (event_id);





