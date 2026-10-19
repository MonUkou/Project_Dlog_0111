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

/** F01: đăng ký Firebase Authentication và tạo hồ sơ Firestore qua AuthService. */
public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterName;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;
    private TextView tvGoLogin;

    private AuthService authService;
    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = AuthService.getInstance();
        authService.init(getApplicationContext());

        initViews();
        setupListeners();
    }

    private void initViews() {
        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressRegister = findViewById(R.id.progressRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        if (requestInProgress) return;

        String displayName = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (displayName.isEmpty()) {
            etRegisterName.setError("Vui lòng nhập tên hiển thị");
            etRegisterName.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Vui lòng nhập email hợp lệ");
            etRegisterEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
            etRegisterPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);
        authService.register(displayName, email, password, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                navigateHome();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String message = (e == null || e.getMessage() == null)
                        ? "Không thể tạo tài khoản. Vui lòng thử lại."
                        : e.getMessage();
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        requestInProgress = loading;
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Đang tạo tài khoản..." : "Tạo tài khoản");
        tvGoLogin.setEnabled(!loading);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
