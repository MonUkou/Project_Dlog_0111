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

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private Button btnLoginSubmit;
    private ProgressBar progressBarLogin;
    private TextView tvGoToRegister;

    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = AuthService.getInstance();
        authService.init(this);

        initViews();
    }

    private void initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        progressBarLogin = findViewById(R.id.progressBarLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLoginSubmit.setOnClickListener(v -> handleLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email / Tên đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarLogin.setVisibility(View.VISIBLE);
        btnLoginSubmit.setEnabled(false);

        authService.login(email, password, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                progressBarLogin.setVisibility(View.GONE);
                btnLoginSubmit.setEnabled(true);

                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                progressBarLogin.setVisibility(View.GONE);
                btnLoginSubmit.setEnabled(true);

                // Fallback login so user can test without online auth
                authService.login(email, password);
                Toast.makeText(LoginActivity.this, "Đăng nhập chế độ thử nghiệm", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
