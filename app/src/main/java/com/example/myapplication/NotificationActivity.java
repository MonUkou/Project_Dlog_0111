package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.Notification;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.NotificationService;
import com.example.myapplication.util.BottomNavHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** F32-F34: tải thông báo của UID hiện tại, đếm chưa đọc và đánh dấu khi mở. */
public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBarNotifications;
    private TextView tvEmptyNotifications;
    private TextView tvUnreadCount;
    private Button btnRefreshNotifications;

    private NotificationAdapter notificationAdapter;
    private NotificationService notificationService;
    private AuthService authService;
    private final List<Notification> notifications = new ArrayList<>();
    private final Set<String> pendingReadIds = new HashSet<>();
    private String currentUserId;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        authService = AuthService.getInstance();
        notificationService = NotificationService.getInstance();
        authService.init(getApplicationContext());

        initViews();
        setupRecyclerView();
        setupListeners();

        currentUserId = authService.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            goToLogin();
        }
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBarNotifications = findViewById(R.id.progressBarNotifications);
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        btnRefreshNotifications = findViewById(R.id.btnRefreshNotifications);
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(this);
        rvNotifications.setAdapter(notificationAdapter);

        // Giữ nguyên click listener trong adapter; listener này chỉ đánh dấu đã đọc.
        GestureDetector detector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent event) {
                        View child = rvNotifications.findChildViewUnder(event.getX(), event.getY());
                        if (child != null) {
                            int position = rvNotifications.getChildAdapterPosition(child);
                            if (position != RecyclerView.NO_POSITION && position < notifications.size()) {
                                markNotificationAsRead(notifications.get(position));
                            }
                        }
                        return false;
                    }
                });

        rvNotifications.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
                detector.onTouchEvent(event);
                return false; // Không chặn thao tác mở bài viết của NotificationAdapter.
            }
        });
    }

    private void setupListeners() {
        btnRefreshNotifications.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { loadNotifications(); }
        });
        tvEmptyNotifications.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (!isLoading && currentUserId != null) loadNotifications();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.NOTIFICATION);
        if (currentUserId != null) loadNotifications();
    }

    private void loadNotifications() {
        if (currentUserId == null || isLoading) return;

        isLoading = true;
        progressBarNotifications.setVisibility(View.VISIBLE);
        btnRefreshNotifications.setEnabled(false);
        tvEmptyNotifications.setVisibility(View.VISIBLE);
        tvEmptyNotifications.setText("Đang tải thông báo...");

        notificationService.getNotificationsAsync(currentUserId,
                new NotificationService.Callback<List<Notification>>() {
                    @Override
                    public void onSuccess(List<Notification> result) {
                        isLoading = false;
                        progressBarNotifications.setVisibility(View.GONE);
                        btnRefreshNotifications.setEnabled(true);

                        notifications.clear();
                        if (result != null) notifications.addAll(result);
                        notificationAdapter.setNotifications(notifications);
                        renderEmptyState();
                        loadUnreadCount();
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading = false;
                        progressBarNotifications.setVisibility(View.GONE);
                        btnRefreshNotifications.setEnabled(true);
                        rvNotifications.setVisibility(View.GONE);
                        tvEmptyNotifications.setVisibility(View.VISIBLE);
                        tvEmptyNotifications.setText("Không thể tải thông báo. Chạm để thử lại.");
                        String message = e == null || e.getMessage() == null
                                ? "Vui lòng kiểm tra kết nối mạng."
                                : e.getMessage();
                        Toast.makeText(NotificationActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void renderEmptyState() {
        if (notifications.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            tvEmptyNotifications.setVisibility(View.VISIBLE);
            tvEmptyNotifications.setText("Bạn chưa có thông báo nào.");
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            tvEmptyNotifications.setVisibility(View.GONE);
        }
    }

    private void loadUnreadCount() {
        if (currentUserId == null) return;
        notificationService.getUnreadCountAsync(currentUserId,
                new NotificationService.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        tvUnreadCount.setText("Chưa đọc: " + (count == null ? 0 : count));
                    }

                    @Override
                    public void onError(Exception e) {
                        tvUnreadCount.setText("Chưa đọc: —");
                    }
                });
    }

    private void markNotificationAsRead(Notification notification) {
        if (notification == null || notification.isRead()) return;
        String notificationId = notification.getId();
        if (notificationId == null || notificationId.trim().isEmpty()
                || pendingReadIds.contains(notificationId)) return;

        pendingReadIds.add(notificationId);
        notificationService.markAsRead(notificationId);
        notification.setRead(true);
        pendingReadIds.remove(notificationId);
        notificationAdapter.setNotifications(notifications);
        loadUnreadCount();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
