package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());
        findViewById(R.id.rowAccountSettings).setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));
        findViewById(R.id.rowPrivacySettings).setOnClickListener(v -> showComingSoon());
        findViewById(R.id.rowNotificationSettings).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        findViewById(R.id.rowBackupSettings).setOnClickListener(v -> showComingSoon());
        findViewById(R.id.rowLanguageSettings).setOnClickListener(v -> showComingSoon());
        findViewById(R.id.rowAboutSettings).setOnClickListener(v ->
                Toast.makeText(this, "DLog · Phiên bản 1.0.0", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnLogoutSettings).setOnClickListener(v -> {
            com.example.myapplication.service.impl.AuthService.getInstance().logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showComingSoon() {
        Toast.makeText(this, "Tùy chọn này sẽ sớm có mặt.", Toast.LENGTH_SHORT).show();
    }
}
