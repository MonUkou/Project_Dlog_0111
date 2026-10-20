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
import com.example.myapplication.util.GoogleSignInHelper;
import com.example.myapplication.util.PasswordVisibilityToggle;

/** F02, F04, F05: đăng nhập và khôi phục phiên Firebase. */
public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private Button btnLoginSubmit;
    private ProgressBar progressBarLogin;
    private TextView tvGoToRegister;
    private TextView tvForgotPassword;
    private Button btnGoogleLogin;

    private AuthService authService;
    private boolean requestInProgress = false;
    private boolean resettingPassword = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = AuthService.getInstance();
        authService.init(getApplicationContext());

        initViews();
        setupListeners();

        // FirebaseAuth tự khôi phục phiên đã đăng nhập sau khi mở lại ứng dụng.
        if (authService.isLoggedIn() && authService.getCurrentUserId() != null) {
            navigateHome();
        }
    }

    private void initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        progressBarLogin = findViewById(R.id.progressBarLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        PasswordVisibilityToggle.bind(etLoginPassword);
    }

    private void setupListeners() {
        btnLoginSubmit.setOnClickListener(v -> handleLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        btnGoogleLogin.setOnClickListener(v -> GoogleSignInHelper.launch(this,
                new GoogleSignInHelper.Callback() {
                    @Override public void onToken(String idToken) {
                        setLoading(true);
                        authService.signInWithGoogle(idToken, new IAuthService.AuthCallback() {
                            @Override public void onSuccess(User user) {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                navigateHome();
                            }
                            @Override public void onError(Exception error) {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this,
                                        error == null || error.getMessage() == null
                                                ? getString(R.string.login_failed) : error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    @Override public void onError(Exception error) {
                        Toast.makeText(LoginActivity.this, "Không thể đăng nhập Google: "
                                + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
    }

    private void handleLogin() {
        if (requestInProgress) return;

        String email = etLoginEmail.getText().toString().trim();
        // Không trim mật khẩu vì dấu cách có thể là một phần của mật khẩu.
        String password = etLoginPassword.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError(getString(R.string.email_invalid));
            etLoginEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etLoginPassword.setError(getString(R.string.password_required));
            etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);
        authService.login(email, password, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                navigateHome();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String message = (e == null || e.getMessage() == null)
                        ? getString(R.string.login_failed)
                        : e.getMessage();
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handlePasswordReset() {
        if (requestInProgress) return;

        String email = etLoginEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError(getString(R.string.forgot_password_email_first));
            etLoginEmail.requestFocus();
            return;
        }

        resettingPassword = true;
        setLoading(true);
        authService.sendPasswordResetEmail(email, new IAuthService.PasswordResetCallback() {
            @Override
            public void onSuccess() {
                resettingPassword = false;
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.reset_email_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Exception e) {
                resettingPassword = false;
                setLoading(false);
                String message = (e == null || e.getMessage() == null)
                        ? getString(R.string.login_failed)
                        : e.getMessage();
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        requestInProgress = loading;
        progressBarLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLoginSubmit.setEnabled(!loading);
        btnLoginSubmit.setText(loading
                ? (resettingPassword ? R.string.reset_email_sending : R.string.login_loading)
                : R.string.login_action);
        tvGoToRegister.setEnabled(!loading);
        tvForgotPassword.setEnabled(!loading);
        btnGoogleLogin.setEnabled(!loading);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
