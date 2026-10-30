delete a
from `authority` a
         join `user` u on u.id = a.user_id
where u.username = :username;
