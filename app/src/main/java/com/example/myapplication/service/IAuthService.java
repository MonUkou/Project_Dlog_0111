package com.example.myapplication.service;

import com.example.myapplication.model.User;

public interface IAuthService {

    interface AuthCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    interface PasswordResetCallback {
        void onSuccess();
        void onError(Exception e);
    }

    User register(String email, String password, String username);

    User login(String email, String password);

    void register(String email, String password, String username, AuthCallback callback);

    void login(String email, String password, AuthCallback callback);

    void sendPasswordResetEmail(String email, PasswordResetCallback callback);

    void logout();

    boolean isLoggedIn();

    String getCurrentUserId();

    User getCurrentUser();
}
