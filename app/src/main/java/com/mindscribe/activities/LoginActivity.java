package com.mindscribe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mindscribe.R;
import com.mindscribe.utils.FirebaseHelper;
import com.mindscribe.utils.ValidationUtils;

/**
 * Login screen with Firebase Authentication.
 * Exception Handling + Input Validation demonstrated.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout   tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton    btnLogin;
    private CircularProgressIndicator progressBar;
    private TextView          tvSignUp, tvForgotPassword;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setListeners();

        // Enter animation
        findViewById(R.id.container_form).startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up));
    }

    private void initViews() {
        tilEmail         = findViewById(R.id.til_email);
        tilPassword      = findViewById(R.id.til_password);
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        btnLogin         = findViewById(R.id.btn_login);
        progressBar      = findViewById(R.id.progress_bar);
        tvSignUp         = findViewById(R.id.tv_sign_up);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void setListeners() {
        // Lambda expressions for all click listeners
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Clear errors on text change — Lambda
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilEmail.setError(null);
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPassword.setError(null);
        });
    }

    private void attemptLogin() {
        // Get and trim input
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Validate inputs — Exception handling style validation
        if (!validateInputs(email, password)) return;

        setLoading(true);

        // Firebase Authentication — Background thread handled internally by Firebase SDK
        try {
            firebaseHelper.getAuth()
                    .signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        setLoading(false);
                        navigateToMain();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        handleAuthError(e.getMessage());
                    });
        } catch (Exception e) {
            // Explicit exception handling
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        if (ValidationUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.err_empty_email));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.err_invalid_email));
            valid = false;
        } else {
            tilEmail.setError(null);
        }

        if (ValidationUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.err_empty_password));
            valid = false;
        } else if (!ValidationUtils.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.err_short_password));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        return valid;
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }

        firebaseHelper.getAuth()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleAuthError(String message) {
        if (message != null) {
            if (message.contains("no user record")) {
                tilEmail.setError("No account found with this email");
            } else if (message.contains("password is invalid")) {
                tilPassword.setError("Incorrect password");
            } else {
                Toast.makeText(this, "Login failed: " + message, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "" : getString(R.string.login));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
    }
}
