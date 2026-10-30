insert into `user` (username, firstname, last_name, country_id)
select :username, :firstname, :lastName, c.id
from country c
where c.code = :countryCode;
