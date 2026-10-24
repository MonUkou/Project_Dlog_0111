package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuActivity extends AppCompatActivity {
    private ImageView avatar;
    private TextView name;
    private TextView username;
    private AuthService authService;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        drawer = findViewById(R.id.dlogDrawer);
        authService = AuthService.getInstance();
        authService.init(this);

        avatar = findViewById(R.id.ivMenuAvatar);
        name = findViewById(R.id.tvMenuName);
        username = findViewById(R.id.tvMenuUsername);

        findViewById(R.id.rowMenuProfile).setOnClickListener(v -> openProfile());
        findViewById(R.id.rowMenuSaved).setOnClickListener(v -> openProfile(true));
        findViewById(R.id.rowMenuCreate).setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class)));
        findViewById(R.id.rowMenuNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        findViewById(R.id.rowMenuChats).setOnClickListener(v ->
                startActivity(new Intent(this, ChatsActivity.class)));
        findViewById(R.id.rowMenuSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.rowMenuAdmin).setOnClickListener(v ->
                startActivity(new Intent(this, AdminActivity.class)));
        findViewById(R.id.rowMenuHelp).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Trung tâm trợ giúp sẽ sớm có mặt.", android.widget.Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowMenuLogout).setOnClickListener(v -> {
            authService.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.MENU);
        loadUser();
        drawer.post(() -> drawer.openDrawer(GravityCompat.START));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (drawer != null) drawer.post(() -> drawer.openDrawer(GravityCompat.START));
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    private void loadUser() {
        String userId = authService.getCurrentUserId();
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(document -> {
                        if ("admin".equalsIgnoreCase(document.getString("role"))) {
                            findViewById(R.id.rowMenuAdmin).setVisibility(android.view.View.VISIBLE);
                        }
                    });
        }
        UserService.getInstance().getUserProfile(userId, new UserService.Callback<User>() {
            @Override public void onSuccess(User user) {
                if (user == null) return;
                name.setText(user.getFullName() == null || user.getFullName().isEmpty()
                        ? "Người dùng DLog" : user.getFullName());
                username.setText("@" + (user.getUsername() == null ? "dlog_user" : user.getUsername()));
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(MenuActivity.this).load(user.getAvatarUrl()).circleCrop().into(avatar);
                }
            }
            @Override public void onError(Exception e) {
                name.setText("Người dùng DLog");
                username.setText("@dlog_user");
            }
        });
    }

    private void openProfile() {
        openProfile(false);
    }

    private void openProfile(boolean showSaved) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("showSavedPosts", showSaved);
        startActivity(intent);
    }
}
