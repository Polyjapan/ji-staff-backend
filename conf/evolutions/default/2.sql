# --- !Ups

alter table events drop column close_date;

alter table forms
	add close_date datetime null;

alter table events
	add is_active boolean default false null;

alter table applications modify state set('draft', 'sent', 'accepted', 'refused', 'requested_changes') default 'draft' not null;

alter table applications_comments
	add user_visible boolean default false null;


# --- !Downs

alter table events
	add close_date datetime not null;

alter table events drop column close_date;
alter table events drop column is_active;
alter table applications modify state set('draft', 'sent', 'accepted', 'refused') default 'draft' not null;

alter table applications_comments
	drop column user_visible;



