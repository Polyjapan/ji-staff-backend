# --- !Ups

create table admin_food_particularities
(
    admin_id int not null primary key,
    food_particularities TEXT null
);

create table staff_food_particularities
(
    event_id int not null primary key,
    particularities_field_id int null,
    constraint staff_food_particularities_events_event_id_fk
        foreign key (event_id) references events (event_id),
    constraint staff_food_particularities_fields_field_id_fk
        foreign key (particularities_field_id) references fields (field_id)
);

# --- !Downs

drop table admin_food_particularities;
drop table staff_food_particularities;
