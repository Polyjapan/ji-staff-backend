# --- !Ups

alter table fields_additional drop foreign key fields_additional_ibfk_1;

alter table fields_additional
	add constraint fields_additional_ibfk_1
		foreign key (field_id) references fields (field_id)
			on delete cascade;


alter table fields drop foreign key fields_ibfk_1;

alter table fields
	add constraint fields_ibfk_1
		foreign key (form_page_id) references form_pages (form_page_id)
			on delete cascade;

alter table form_pages drop foreign key form_pages_ibfk_1;

alter table form_pages
	add constraint form_pages_ibfk_1
		foreign key (form) references forms (form_id)
			on delete cascade;

alter table applications_contents drop foreign key applications_contents_ibfk_2;

alter table applications_contents
	add constraint applications_contents_ibfk_2
		foreign key (field_id) references fields (field_id)
			on delete cascade;

create index fields_additional_field_id_index
	on fields_additional (field_id);


# --- !Downs

alter table fields_additional drop foreign key fields_additional_ibfk_1;

alter table fields_additional
	add constraint fields_additional_ibfk_1
		foreign key (field_id) references fields (field_id);

alter table fields drop foreign key fields_ibfk_1;

alter table fields
	add constraint fields_ibfk_1
		foreign key (form_page_id) references form_pages (form_page_id);

alter table form_pages drop foreign key form_pages_ibfk_1;

alter table form_pages
	add constraint form_pages_ibfk_1
		foreign key (form) references forms (form_id);

alter table applications_contents drop foreign key applications_contents_ibfk_2;

alter table applications_contents
	add constraint applications_contents_ibfk_2
		foreign key (field_id) references fields (field_id);

drop index fields_additional_field_id_index on fields_additional;


