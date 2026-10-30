delete l
from `like` l
         join `user` u on u.id = l.user_id
where u.username = :username;
