delete pl
from photo_like pl
         join photo p on p.id = pl.photo_id
         join `user` u on u.id = p.user_id
where u.username = :username;
