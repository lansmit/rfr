delete s
from statistic s
         join `user` u on u.id = s.user_id
where u.username = :username;
