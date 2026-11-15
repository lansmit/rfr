select exists(select 1
              from friendship f
                       join `user` u on u.id in (f.requester_id, f.addressee_id)
              where u.username = :username);
