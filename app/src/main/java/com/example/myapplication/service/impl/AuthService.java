package com.example.myapplication.service.impl;

import android.os.Looper;
import android.util.Log;

import com.example.myapplication.model.User;
import com.example.myapplication.service.IAuthService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AuthService implements IAuthService {
    private static final String TAG = "AuthService";
    private final FirebaseAuth auth;
    private final CollectionReference usersRef;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
        this.usersRef = FirebaseFirestore.getInstance().collection("users");
    }

    public AuthService(FirebaseAuth auth, CollectionReference usersRef) {
        this.auth = auth;
        this.usersRef = usersRef;
    }

    @Override
    public User register(String email, String password, String username) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty() || username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Email, password, and username must not be empty");
        }

        try {
            AuthResult authResult = awaitTask(auth.createUserWithEmailAndPassword(email, password));
            FirebaseUser firebaseUser = authResult.getUser();
            if (firebaseUser == null) {
                throw new RuntimeException("Registration failed: FirebaseUser is null");
            }

            String uid = firebaseUser.getUid();
            User user = new User(uid, username, email);

            // Save user profile to Firestore (without password)
            awaitTask(usersRef.document(uid).set(user.toMap()));

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Register error: " + e.getMessage(), e);
            if (e instanceof FirebaseAuthUserCollisionException) {
                throw new RuntimeException("Email already exists", e);
            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                throw new RuntimeException("Invalid email format or weak password", e);
            }
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    @Override
    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and password must not be empty");
        }

        try {
            AuthResult authResult = awaitTask(auth.signInWithEmailAndPassword(email, password));
            FirebaseUser firebaseUser = authResult.getUser();
            if (firebaseUser == null) {
                throw new RuntimeException("Login failed: FirebaseUser is null");
            }

            String uid = firebaseUser.getUid();

            try {
                DocumentSnapshot doc = awaitTask(usersRef.document(uid).get());
                if (doc.exists()) {
                    return User.fromMap(doc.getData(), uid);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch user profile from Firestore, creating fallback user object", e);
            }

            return new User(uid, firebaseUser.getEmail(), firebaseUser.getEmail());

        } catch (Exception e) {
            Log.e(TAG, "Login error: " + e.getMessage(), e);
            if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
                throw new RuntimeException("Incorrect email or password", e);
            }
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void logout() {
        auth.signOut();
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private static <T> T awaitTask(Task<T> task) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AtomicReference<T> resultRef = new AtomicReference<>();
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            task.addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    resultRef.set(task1.getResult());
                } else {
                    exceptionRef.set(task1.getException() != null ? task1.getException() : new RuntimeException("Task failed"));
                }
                latch.countDown();
            });

            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Operation timed out");
            }

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return resultRef.get();
        } else {
            return Tasks.await(task, 10, TimeUnit.SECONDS);
        }
    }
}
