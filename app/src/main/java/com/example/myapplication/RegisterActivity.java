package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.User;
import com.example.myapplication.service.IAuthService;
import com.example.myapplication.service.impl.AuthService;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterUsername;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private Button btnRegisterSubmit;
    private ProgressBar progressBarRegister;
    private TextView tvGoToLogin;

    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = AuthService.getInstance();
        authService.init(this);

        initViews();
    }

    private void initViews() {
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        progressBarRegister = findViewById(R.id.progressBarRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegisterSubmit.setOnClickListener(v -> handleRegister());

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String username = etRegisterUsername.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên người dùng và email!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarRegister.setVisibility(View.VISIBLE);
        btnRegisterSubmit.setEnabled(false);

        authService.register(email, password, username, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                progressBarRegister.setVisibility(View.GONE);
                btnRegisterSubmit.setEnabled(true);

                Toast.makeText(RegisterActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                progressBarRegister.setVisibility(View.GONE);
                btnRegisterSubmit.setEnabled(true);

                authService.setCurrentUserId(username);
                Toast.makeText(RegisterActivity.this, "Đã tạo tài khoản chế độ thử nghiệm", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
