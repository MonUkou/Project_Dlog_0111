package com.example.myapplication.service.impl;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.model.User;
import com.example.myapplication.service.IAuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthService implements IAuthService {

    private static AuthService instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static final String PREF_NAME = "dlog_auth_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private String fallbackUserId;
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

    /** Signs into Firebase with a Google ID token and creates the DLog profile on first sign-in. */
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        if (idToken == null || idToken.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Google ID token is empty."));
            return;
        }
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        if (callback != null) callback.onError(new IllegalStateException("Không lấy được tài khoản Google."));
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    setCurrentUserId(uid);
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists() && snapshot.getData() != null) {
                                    if (callback != null) callback.onSuccess(User.fromMap(snapshot.getData()));
                                    return;
                                }
                                String displayName = firebaseUser.getDisplayName();
                                String email = firebaseUser.getEmail();
                                String username = displayName;
                                if (username == null || username.trim().isEmpty()) {
                                    username = email == null ? "dlog_user" : email.split("@")[0];
                                }
                                User user = new User(uid, username, username, email,
                                        firebaseUser.getPhotoUrl() == null ? "" : firebaseUser.getPhotoUrl().toString(),
                                        "");
                                db.collection("users").document(uid).set(user.toMap())
                                        .addOnSuccessListener(unused -> {
                                            if (callback != null) callback.onSuccess(user);
                                        })
                                        .addOnFailureListener(error -> {
                                            if (callback != null) callback.onError(error);
                                        });
                            })
                            .addOnFailureListener(error -> {
                                if (callback != null) callback.onError(error);
                            });
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onError(error);
                });
    }

    @Override
    public void logout() {
        auth.signOut();
        fallbackUserId = null;
        if (context != null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_CURRENT_USER_ID)
                    .apply();
        }
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public void sendPasswordResetEmail(String email, PasswordResetCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(error -> {
                    if (callback != null) callback.onError(error);
                });
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
