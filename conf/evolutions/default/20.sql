# --- !Ups

create table meals
(
    meal_id int auto_increment,
    event_id int not null,
    meal_name varchar(200) not null,
    meal_date DATE default CURRENT_DATE not null,
    constraint meals_pk primary key (meal_id),
    constraint meals_events_event_id_fk foreign key (event_id) references events (event_id)
);

create table meals_taken
(
    meal_id int not null,
    user_id int not null,
    timestamp TIMESTAMP default CURRENT_TIMESTAMP null,
    constraint meals_taken_meals_meal_id_fk
        foreign key (meal_id) references meals (meal_id)
);

# --- !Downs

drop table meals_taken;
drop table meals;
