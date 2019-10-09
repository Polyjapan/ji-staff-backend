# !--- Ups

alter table applications_comments drop foreign key applications_comments_ibfk_2;

# !--- Downs

alter table applications_comments
	add constraint applications_comments_ibfk_2
		foreign key (user_id) references users (user_id);
