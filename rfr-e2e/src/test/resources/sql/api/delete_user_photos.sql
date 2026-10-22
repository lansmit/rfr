delete p
from photo p
         join `user` u on u.id = p.user_id
where u.username = :username;
