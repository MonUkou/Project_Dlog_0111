package com.example.myapplication.service.impl;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.myapplication.model.User;
import com.example.myapplication.service.IAuthService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
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

    /** Listener lỗi dùng chung: chuyển lỗi về AuthCallback. */
    private OnFailureListener errorListener(final AuthCallback callback) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (callback != null) callback.onError(e);
            }
        };
    }

    /** Tạo User tạm khi chưa đọc được hồ sơ Firestore. */
    private User buildFallbackUser(String uid, String email) {
        String username = (email != null && email.contains("@")) ? email.split("@")[0] : email;
        return new User(uid, username, username, email, "", "");
    }

    @Override
    public void register(final String email, String password, final String username,
                         final AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser == null) {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("Không tạo được tài khoản."));
                            }
                            return;
                        }
                        final String uid = firebaseUser.getUid();
                        final User user = new User(uid, username, username, email, "",
                                "Giới thiệu ngắn về bản thân.");
                        db.collection("users").document(uid).set(user.toMap())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        setCurrentUserId(uid);
                                        if (callback != null) callback.onSuccess(user);
                                    }
                                })
                                .addOnFailureListener(errorListener(callback));
                    }
                })
                .addOnFailureListener(errorListener(callback));
    }

    @Override
    public void login(final String email, String password, final AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser == null) {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("Không đăng nhập được."));
                            }
                            return;
                        }
                        final String uid = firebaseUser.getUid();
                        setCurrentUserId(uid);

                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot snapshot) {
                                        if (Boolean.TRUE.equals(snapshot.getBoolean("locked"))) {
                                            auth.signOut();
                                            setCurrentUserId(null);
                                            if (callback != null) callback.onError(new IllegalStateException("Tài khoản đã bị khóa. Liên hệ quản trị viên."));
                                            return;
                                        }
                                        if (snapshot.exists() && snapshot.getData() != null) {
                                            User user = User.fromMap(snapshot.getData());
                                            if (callback != null) callback.onSuccess(user);
                                        } else {
                                            if (callback != null) {
                                                callback.onSuccess(buildFallbackUser(uid, email));
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        if (callback != null) {
                                            callback.onSuccess(buildFallbackUser(uid, email));
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(errorListener(callback));
    }

    /** Signs into Firebase with a Google ID token and creates the DLog profile on first sign-in. */
    public void signInWithGoogle(String idToken, final AuthCallback callback) {
        if (idToken == null || idToken.trim().isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Google ID token is empty."));
            }
            return;
        }

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        final FirebaseUser firebaseUser = result.getUser();
                        if (firebaseUser == null) {
                            if (callback != null) {
                                callback.onError(new IllegalStateException("Không lấy được tài khoản Google."));
                            }
                            return;
                        }
                        final String uid = firebaseUser.getUid();
                        setCurrentUserId(uid);

                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot snapshot) {
                                        if (Boolean.TRUE.equals(snapshot.getBoolean("locked"))) {
                                            auth.signOut();
                                            setCurrentUserId(null);
                                            if (callback != null) callback.onError(new IllegalStateException("Tài khoản đã bị khóa. Liên hệ quản trị viên."));
                                            return;
                                        }
                                        // Đã có hồ sơ, trả về luôn.
                                        if (snapshot.exists() && snapshot.getData() != null) {
                                            if (callback != null) {
                                                callback.onSuccess(User.fromMap(snapshot.getData()));
                                            }
                                            return;
                                        }

                                        // Lần đầu đăng nhập Google, tạo hồ sơ mới.
                                        String displayName = firebaseUser.getDisplayName();
                                        String email = firebaseUser.getEmail();
                                        String username = displayName;
                                        if (username == null || username.trim().isEmpty()) {
                                            username = (email == null) ? "dlog_user" : email.split("@")[0];
                                        }
                                        String photoUrl = firebaseUser.getPhotoUrl() == null
                                                ? "" : firebaseUser.getPhotoUrl().toString();

                                        final User user = new User(uid, username, username, email,
                                                photoUrl, "");
                                        db.collection("users").document(uid).set(user.toMap())
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        if (callback != null) callback.onSuccess(user);
                                                    }
                                                })
                                                .addOnFailureListener(errorListener(callback));
                                    }
                                })
                                .addOnFailureListener(errorListener(callback));
                    }
                })
                .addOnFailureListener(errorListener(callback));
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
    public void sendPasswordResetEmail(String email, final PasswordResetCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (callback != null) callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception error) {
                        if (callback != null) callback.onError(error);
                    }
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
