package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.util.UiChrome;
import com.example.myapplication.util.UiMotion;
import com.example.myapplication.service.impl.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.lang.ref.WeakReference;

public class DLogApplication extends Application {
    public static final String EXTRA_ACCOUNT_LOCKED_MESSAGE = "account_locked_message";
    private static final String ACCOUNT_LOCKED_MESSAGE =
            "Tài khoản đã bị khóa do vi phạm tiêu chuẩn cộng đồng DLog.";

    private int startedActivities;
    private ListenerRegistration accountLockRegistration;
    private String observedAccountId;
    private boolean handlingLockedAccount;
    private WeakReference<Activity> foregroundActivity = new WeakReference<>(null);
    private final Handler presenceHandler = new Handler(Looper.getMainLooper());
    private final Runnable markOffline = new Runnable() {
        @Override public void run() {
            if (startedActivities == 0) updatePresence(false);
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = getSharedPreferences("dlog_settings", MODE_PRIVATE).getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(dark
                ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
                UiMotion.forward(activity);
            }
            @Override public void onActivityStarted(@NonNull Activity activity) {
                presenceHandler.removeCallbacks(markOffline);
                if (startedActivities++ == 0) updatePresence(true);
                observeAccountLock(activity);
            }
            @Override public void onActivityResumed(@NonNull Activity activity) {
                foregroundActivity = new WeakReference<>(activity);
                UiChrome.hideNavigationBar(activity);
                observeAccountLock(activity);
            }
            @Override public void onActivityPaused(@NonNull Activity activity) {
                if (activity.isFinishing()) UiMotion.backward(activity);
            }
            @Override public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0) presenceHandler.postDelayed(markOffline, 1500L);
            }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) {
                if (foregroundActivity.get() == activity) {
                    foregroundActivity.clear();
                }
            }
        });
    }

    private void updatePresence(boolean online) {
        com.google.firebase.auth.FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) return;
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        values.put("online", online);
        values.put("lastSeenAt", System.currentTimeMillis());
        values.put("presenceUpdatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(account.getUid())
                .update(values);
    }

    private void observeAccountLock(Activity activity) {
        if (activity instanceof LoginActivity || activity instanceof RegisterActivity) {
            stopAccountLockObserver();
            return;
        }
        com.google.firebase.auth.FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) {
            stopAccountLockObserver();
            return;
        }
        if (accountLockRegistration != null && account.getUid().equals(observedAccountId)) return;

        stopAccountLockObserver();
        handlingLockedAccount = false;
        observedAccountId = account.getUid();
        accountLockRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(observedAccountId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot document,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null || document == null || !document.exists()) return;
                        if (Boolean.TRUE.equals(document.getBoolean("locked"))) {
                            forceLogoutLockedAccount();
                        }
                    }
                });
    }

    private void forceLogoutLockedAccount() {
        if (handlingLockedAccount) return;
        handlingLockedAccount = true;

        Activity activity = foregroundActivity.get();
        AuthService.getInstance().init(getApplicationContext());
        AuthService.getInstance().logout();
        stopAccountLockObserver();

        Toast.makeText(getApplicationContext(), ACCOUNT_LOCKED_MESSAGE, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_LOCKED_MESSAGE, ACCOUNT_LOCKED_MESSAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.startActivity(intent);
            activity.finishAffinity();
        } else {
            startActivity(intent);
        }
    }

    private void stopAccountLockObserver() {
        if (accountLockRegistration != null) {
            accountLockRegistration.remove();
            accountLockRegistration = null;
        }
        observedAccountId = null;
    }
}
