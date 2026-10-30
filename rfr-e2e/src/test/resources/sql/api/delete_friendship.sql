delete f
from friendship f
         join `user` r on r.id = f.requester_id
         join `user` a on a.id = f.addressee_id
where r.username = :requester
  and a.username = :addressee;
