package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

/** F01: đăng ký Firebase Authentication và tạo hồ sơ Firestore qua AuthService. */
public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterName;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;
    private TextView tvGoLogin;
    private TextView btnBackRegister;
    private CheckBox cbAcceptTerms;
    private Button btnGoogleRegister;

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
        btnBackRegister = findViewById(R.id.btnBackRegister);
        cbAcceptTerms = findViewById(R.id.cbAcceptTerms);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
        PasswordVisibilityToggle.bind(etRegisterPassword);
        PasswordVisibilityToggle.bind(etConfirmPassword);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoLogin.setOnClickListener(v -> finish());
        btnBackRegister.setOnClickListener(v -> finish());
        btnGoogleRegister.setOnClickListener(v -> GoogleSignInHelper.launch(this,
                new GoogleSignInHelper.Callback() {
                    @Override public void onToken(String idToken) {
                        setLoading(true);
                        authService.signInWithGoogle(idToken, new IAuthService.AuthCallback() {
                            @Override public void onSuccess(User user) {
                                setLoading(false);
                                Toast.makeText(RegisterActivity.this,
                                        R.string.register_success, Toast.LENGTH_SHORT).show();
                                navigateHome();
                            }
                            @Override public void onError(Exception error) {
                                setLoading(false);
                                Toast.makeText(RegisterActivity.this,
                                        error == null || error.getMessage() == null
                                                ? getString(R.string.register_failed) : error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    @Override public void onError(Exception error) {
                        Toast.makeText(RegisterActivity.this, "Không thể đăng ký Google: "
                                + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
    }

    private void handleRegister() {
        if (requestInProgress) return;

        String displayName = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (displayName.isEmpty()) {
            etRegisterName.setError(getString(R.string.name_required));
            etRegisterName.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError(getString(R.string.email_invalid));
            etRegisterEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError(getString(R.string.password_min_error));
            etRegisterPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.password_match_error));
            etConfirmPassword.requestFocus();
            return;
        }
        if (!cbAcceptTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với điều khoản để tiếp tục.", Toast.LENGTH_SHORT).show();
            cbAcceptTerms.requestFocus();
            return;
        }

        setLoading(true);
        authService.register(email, password, displayName, new IAuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        R.string.register_success, Toast.LENGTH_SHORT).show();
                navigateHome();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                String message = (e == null || e.getMessage() == null)
                        ? getString(R.string.register_failed)
                        : e.getMessage();
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        requestInProgress = loading;
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? R.string.register_loading : R.string.register_action);
        tvGoLogin.setEnabled(!loading);
        btnBackRegister.setEnabled(!loading);
        cbAcceptTerms.setEnabled(!loading);
        btnGoogleRegister.setEnabled(!loading);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
