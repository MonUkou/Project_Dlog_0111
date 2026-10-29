package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class MenuActivity extends AppCompatActivity {
    private ImageView avatar;
    private TextView name;
    private TextView username;
    private AuthService authService;
    private DrawerLayout drawer;
    private ListenerRegistration userRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        drawer = findViewById(R.id.dlogDrawer);
        authService = AuthService.getInstance();
        authService.init(this);

        avatar = findViewById(R.id.ivMenuAvatar);
        name = findViewById(R.id.tvMenuName);
        username = findViewById(R.id.tvMenuUsername);

        findViewById(R.id.rowMenuProfile).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openProfile(); }
        });
        findViewById(R.id.rowMenuSaved).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openProfile(true); }
        });
        findViewById(R.id.rowMenuCreate).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, CreatePostActivity.class));
            }
        });
        findViewById(R.id.rowMenuNotifications).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, NotificationActivity.class));
            }
        });
        findViewById(R.id.rowMenuChats).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, ChatsActivity.class));
            }
        });
        findViewById(R.id.rowMenuSettings).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, SettingsActivity.class));
            }
        });
        findViewById(R.id.rowMenuAdmin).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, AdminActivity.class));
            }
        });
        findViewById(R.id.rowMenuHelp).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                android.widget.Toast.makeText(MenuActivity.this, "Trung tâm trợ giúp sẽ sớm có mặt.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.rowMenuLogout).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                authService.logout();
                Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.MENU);
        loadUser();
        drawer.post(new Runnable() {
            @Override public void run() { drawer.openDrawer(GravityCompat.START); }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (drawer != null) drawer.post(new Runnable() {
            @Override public void run() { drawer.openDrawer(GravityCompat.START); }
        });
    }

    private void loadUser() {
        String userId = authService.getCurrentUserId();
        if (userId == null || userRegistration != null) return;
        userRegistration = FirebaseFirestore.getInstance().collection("users")
                .document(userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException error) {
                if (error != null || snapshot == null || snapshot.getData() == null) return;
                User user = User.fromMap(snapshot.getData());
                if (user == null) return;
                findViewById(R.id.rowMenuAdmin).setVisibility(
                        "admin".equalsIgnoreCase(user.getRole())
                                ? android.view.View.VISIBLE : android.view.View.GONE);
                name.setText(user.getFullName() == null || user.getFullName().isEmpty()
                        ? "Người dùng DLog" : user.getFullName());
                username.setText("@" + (user.getUsername() == null ? "dlog_user" : user.getUsername()));
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(MenuActivity.this).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUser();
    }

    @Override
    protected void onStop() {
        if (userRegistration != null) {
            userRegistration.remove();
            userRegistration = null;
        }
        super.onStop();
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
