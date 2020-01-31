# --- !Ups

create table staff_arrivals_logs
(
    staff_id int not null,
    event_id int not null,
    time TIMESTAMP default CURRENT_TIMESTAMP not null,
    action SET('arrived', 'left') null,
    constraint staff_arrivals_logs_events_event_id_fk
        foreign key (event_id) references events (event_id)
);

# --- !Downs

drop table staff_arrivals_logs;