delete pl
from photo_like pl
         join `like` l on l.id = pl.like_id
         join `user` u on u.id = l.user_id
where u.username = :username;
