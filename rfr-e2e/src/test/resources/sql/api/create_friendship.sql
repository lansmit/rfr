insert into friendship (id, requester_id, addressee_id, status, created_at, responded_at)
select UUID_TO_BIN(UUID(), true), r.id, a.id, :status, now(6), if(:status = 'ACCEPTED', now(6), null)
from `user` r
         join `user` a on a.username = :addressee
where r.username = :requester;
