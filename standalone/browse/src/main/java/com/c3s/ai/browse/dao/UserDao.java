package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.model.User;

import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);
}
