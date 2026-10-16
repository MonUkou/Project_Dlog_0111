package com.example.myapplication.service;

import com.example.myapplication.model.User;

public interface IAuthService {

    User register(String email, String password, String username);

    User login(String email, String password);

    void logout();

    boolean isLoggedIn();

    String getCurrentUserId();
}