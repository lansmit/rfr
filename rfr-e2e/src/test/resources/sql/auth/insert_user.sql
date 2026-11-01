insert into `user` (id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
values (:id, :username, :password, true, true, true, true);
