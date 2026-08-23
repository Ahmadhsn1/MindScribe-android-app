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
import com.mindscribe.models.User;
import com.mindscribe.utils.FirebaseHelper;
import com.mindscribe.utils.ValidationUtils;

/**
 * Registration screen — Firebase Auth + save user to Firestore.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout   tilName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton    btnSignUp;
    private CircularProgressIndicator progressBar;
    private TextView          tvLogin;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firebaseHelper = FirebaseHelper.getInstance();
        initViews();
        setListeners();

        findViewById(R.id.container_form).startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up));
    }

    private void initViews() {
        tilName            = findViewById(R.id.til_name);
        tilEmail           = findViewById(R.id.til_email);
        tilPassword        = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etName             = findViewById(R.id.et_name);
        etEmail            = findViewById(R.id.et_email);
        etPassword         = findViewById(R.id.et_password);
        etConfirmPassword  = findViewById(R.id.et_confirm_password);
        btnSignUp          = findViewById(R.id.btn_signup);
        progressBar        = findViewById(R.id.progress_bar);
        tvLogin            = findViewById(R.id.tv_login);
    }

    private void setListeners() {
        btnSignUp.setOnClickListener(v -> attemptSignUp());

        tvLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_left);
        });

        // Clear errors on focus
        etName.setOnFocusChangeListener((v, f)    -> { if (f) tilName.setError(null); });
        etEmail.setOnFocusChangeListener((v, f)   -> { if (f) tilEmail.setError(null); });
        etPassword.setOnFocusChangeListener((v, f)-> { if (f) tilPassword.setError(null); });
        etConfirmPassword.setOnFocusChangeListener((v, f) -> {
            if (f) tilConfirmPassword.setError(null);
        });
    }

    private void attemptSignUp() {
        String name    = etName.getText()            != null ? etName.getText().toString().trim() : "";
        String email   = etEmail.getText()           != null ? etEmail.getText().toString().trim() : "";
        String pass    = etPassword.getText()        != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (!validateInputs(name, email, pass, confirm)) return;

        setLoading(true);

        try {
            firebaseHelper.getAuth()
                    .createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        saveUserToFirestore(uid, name, email);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Sign up failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        User user = new User(uid, name, email);

        firebaseHelper.saveUser(user, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                // Account created but profile save failed — still navigate
                Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });
    }

    private boolean validateInputs(String name, String email, String pass, String confirm) {
        boolean valid = true;

        if (ValidationUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.err_empty_name));
            valid = false;
        } else {
            tilName.setError(null);
        }

        if (ValidationUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.err_empty_email));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.err_invalid_email));
            valid = false;
        } else {
            tilEmail.setError(null);
        }

        if (ValidationUtils.isEmpty(pass)) {
            tilPassword.setError(getString(R.string.err_empty_password));
            valid = false;
        } else if (!ValidationUtils.isValidPassword(pass)) {
            tilPassword.setError(getString(R.string.err_short_password));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!ValidationUtils.passwordsMatch(pass, confirm)) {
            tilConfirmPassword.setError(getString(R.string.err_password_mismatch));
            valid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!loading);
        btnSignUp.setText(loading ? "" : getString(R.string.sign_up));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
    }
}
