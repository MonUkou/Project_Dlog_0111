package com.example.myapplication;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.service.IAuthService;
import com.example.myapplication.service.impl.AuthService;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText emailInput;
    private TextView submit;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailInput = findViewById(R.id.etResetEmail);
        submit = findViewById(R.id.btnSendResetEmail);
        progress = findViewById(R.id.progressResetEmail);
        findViewById(R.id.btnBackReset).setOnClickListener(v -> finish());
        submit.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = emailInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Nhập địa chỉ email hợp lệ");
            emailInput.requestFocus();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        submit.setEnabled(false);
        AuthService.getInstance().sendPasswordResetEmail(email, new IAuthService.PasswordResetCallback() {
            @Override public void onSuccess() {
                progress.setVisibility(View.GONE);
                submit.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Đã gửi liên kết đặt lại mật khẩu đến email của bạn.", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                submit.setEnabled(true);
                String message = e == null || e.getMessage() == null
                        ? "Không thể gửi email đặt lại mật khẩu." : e.getMessage();
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
