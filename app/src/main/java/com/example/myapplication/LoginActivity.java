package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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

/** F02, F04, F05: đăng nhập và khôi phục phiên Firebase. */
public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private Button btnLoginSubmit;
    private ProgressBar progressBarLogin;
    private TextView tvGoToRegister;

    private AuthService authService;
    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = AuthService.getInstance();
        authService.init(getApplicationContext());

        initViews();
        setupListeners();
    }

    private void initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        progressBarLogin = findViewById(R.id.progressBarLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        btnLoginSubmit.setOnClickListener(v -> handleLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void handleLogin() {
        if (requestInProgress) return;

        String email = etLoginEmail.getText().toString().trim();
        // Không trim mật khẩu vì dấu cách có thể là một phần của mật khẩu.
        String password = etLoginPassword.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError("Vui lòng nhập email hợp lệ");
            etLoginEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etLoginPassword.setError("Vui lòng nhập mật khẩu");
            etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);
        authService.login(email, password, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateHome();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String message = (e == null || e.getMessage() == null)
                        ? "Đăng nhập thất bại. Vui lòng thử lại."
                        : e.getMessage();
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        requestInProgress = loading;
        progressBarLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
        btnLoginSubmit.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
        tvGoToRegister.setEnabled(!loading);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
