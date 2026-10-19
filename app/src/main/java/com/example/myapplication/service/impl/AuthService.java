package com.example.myapplication.service.impl;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.model.User;
import com.example.myapplication.service.IAuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthService implements IAuthService {

    private static AuthService instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static final String PREF_NAME = "dlog_auth_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private String fallbackUserId = "user_minh_anh";
    private Context context;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        SharedPreferences prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedId = prefs.getString(KEY_CURRENT_USER_ID, null);
        if (savedId != null) {
            fallbackUserId = savedId;
        }
    }

    public void setCurrentUserId(String userId) {
        this.fallbackUserId = userId;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply();
        }
    }

    @Override
    public User register(String email, String password, String username) {
        // Sync registration stub
        User user = new User(username, username, username, email, "", "Giới thiệu ngắn về bản thân.");
        user.setId(username);
        return user;
    }

    @Override
    public User login(String email, String password) {
        // Sync login stub
        String username = email.contains("@") ? email.split("@")[0] : email;
        User user = new User(username, username, username, email, "", "Giới thiệu ngắn về bản thân.");
        user.setId(username);
        setCurrentUserId(username);
        return user;
    }

    @Override
    public void register(String email, String password, String username, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        User user = new User(uid, username, username, email, "", "Giới thiệu ngắn về bản thân.");
                        db.collection("users").document(uid).set(user.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    setCurrentUserId(uid);
                                    if (callback != null) callback.onSuccess(user);
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onError(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        setCurrentUserId(uid);
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                                        User user = User.fromMap(documentSnapshot.getData());
                                        if (callback != null) callback.onSuccess(user);
                                    } else {
                                        String username = email.contains("@") ? email.split("@")[0] : email;
                                        User user = new User(uid, username, username, email, "", "");
                                        if (callback != null) callback.onSuccess(user);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    String username = email.contains("@") ? email.split("@")[0] : email;
                                    User user = new User(uid, username, username, email, "", "");
                                    if (callback != null) callback.onSuccess(user);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void logout() {
        auth.signOut();
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null || fallbackUserId != null;
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return fallbackUserId;
    }

    public User getCurrentUser() {
        String uid = getCurrentUserId();
        User user = new User();
        user.setId(uid);
        user.setUsername(uid);
        user.setFullName(uid);
        user.setEmail(uid + "@example.com");
        return user;
    }
}
