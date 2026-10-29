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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Source;

public class DLogApplication extends Application {
    private long lastLockCheckAt;
    private boolean checkingLock;
    private int startedActivities;
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
            }
            @Override public void onActivityResumed(@NonNull Activity activity) {
                UiChrome.hideNavigationBar(activity);
                checkAccountLock(activity);
            }
            @Override public void onActivityPaused(@NonNull Activity activity) {
                if (activity.isFinishing()) UiMotion.backward(activity);
            }
            @Override public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0) presenceHandler.postDelayed(markOffline, 1500L);
            }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
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

    private void checkAccountLock(final Activity activity) {
        if (activity instanceof LoginActivity || activity instanceof RegisterActivity) return;
        com.google.firebase.auth.FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null || checkingLock || System.currentTimeMillis() - lastLockCheckAt < 30_000L) return;
        checkingLock = true;
        FirebaseFirestore.getInstance().collection("users").document(account.getUid()).get(Source.SERVER)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override public void onSuccess(com.google.firebase.firestore.DocumentSnapshot document) {
                        checkingLock = false;
                        lastLockCheckAt = System.currentTimeMillis();
                        if (!Boolean.TRUE.equals(document.getBoolean("locked"))
                                || activity.isFinishing() || activity.isDestroyed()) return;
                        AuthService.getInstance().init(activity);
                        AuthService.getInstance().logout();
                        Toast.makeText(activity, "Tài khoản đã bị khóa.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) {
                        checkingLock = false;
                        lastLockCheckAt = System.currentTimeMillis();
                    }
                });
    }
}
