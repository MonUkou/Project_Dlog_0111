package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.Notification;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.NotificationService;
import com.example.myapplication.util.BottomNavHelper;

import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBarNotifications;
    private TextView tvEmptyNotifications;

    private NotificationAdapter notificationAdapter;
    private NotificationService notificationService;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationService = NotificationService.getInstance();
        authService = AuthService.getInstance();

        initViews();
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.NOTIFICATION);

        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBarNotifications = findViewById(R.id.progressBarNotifications);
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications);
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(this);
        rvNotifications.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        String currentUserId = authService.getCurrentUserId();
        progressBarNotifications.setVisibility(View.VISIBLE);
        tvEmptyNotifications.setVisibility(View.GONE);

        notificationService.getNotificationsAsync(currentUserId, new NotificationService.Callback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                progressBarNotifications.setVisibility(View.GONE);
                if (notifications.isEmpty()) {
                    tvEmptyNotifications.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyNotifications.setVisibility(View.GONE);
                }
                notificationAdapter.setNotifications(notifications);
            }

            @Override
            public void onError(Exception e) {
                progressBarNotifications.setVisibility(View.GONE);
                tvEmptyNotifications.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.NOTIFICATION);
        loadNotifications();
    }
}
