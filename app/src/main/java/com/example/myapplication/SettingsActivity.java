package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.myapplication.service.impl.AuthService;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "dlog_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private SwitchCompat switchDarkMode;
    private boolean isUpdatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupDarkMode();
        setupClickListeners();
    }

    private void initializeViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
    }

    private void setupDarkMode() {
        SharedPreferences preferences = getSharedPreferences(
                PREFS_NAME, MODE_PRIVATE
        );

        boolean isDarkMode = preferences.getBoolean(
                KEY_DARK_MODE, false
        );

        updateThemeDescription(isDarkMode);
        findViewById(R.id.rowDarkMode).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { switchDarkMode.toggle(); }
        });
        isUpdatingSwitch = true;
        switchDarkMode.setChecked(isDarkMode);
        isUpdatingSwitch = false;

        switchDarkMode.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView,
                            boolean isChecked
                    ) {
                        if (isUpdatingSwitch) {
                            return;
                        }

                        updateThemeDescription(isChecked);
                        preferences.edit()
                                .putBoolean(KEY_DARK_MODE, isChecked)
                                .apply();

                        if (isChecked) {
                            AppCompatDelegate.setDefaultNightMode(
                                    AppCompatDelegate.MODE_NIGHT_YES
                            );
                        } else {
                            AppCompatDelegate.setDefaultNightMode(
                                    AppCompatDelegate.MODE_NIGHT_NO
                            );
                        }
                    }
                }
        );
    }

    private void updateThemeDescription(boolean dark) {
        ((TextView)findViewById(R.id.tvThemeDescription)).setText(dark ? "Đang dùng giao diện tối" : "Đang dùng giao diện sáng");
        switchDarkMode.setStateDescription(dark ? "Tối" : "Sáng");
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBackSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });

        findViewById(R.id.rowAccountSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(
                                SettingsActivity.this,
                                EditProfileActivity.class
                        ));
                    }
                });

        findViewById(R.id.rowPrivacySettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showComingSoon();
                    }
                });

        findViewById(R.id.rowNotificationSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(
                                SettingsActivity.this,
                                NotificationActivity.class
                        ));
                    }
                });

        findViewById(R.id.rowBackupSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showComingSoon();
                    }
                });

        findViewById(R.id.rowLanguageSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showComingSoon();
                    }
                });

        findViewById(R.id.rowAboutSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(
                                SettingsActivity.this,
                                "DLog · Phiên bản 1.0.0",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        findViewById(R.id.btnLogoutSettings)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AuthService.getInstance().logout();

                        Intent intent = new Intent(
                                SettingsActivity.this,
                                LoginActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void showComingSoon() {
        Toast.makeText(
                this,
                "Tùy chọn này sẽ sớm có mặt.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
