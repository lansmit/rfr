select exists(select 1
              from `user`
              where id = :id);
