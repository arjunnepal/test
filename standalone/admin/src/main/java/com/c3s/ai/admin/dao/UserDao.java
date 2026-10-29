package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.model.User;

import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);
}
