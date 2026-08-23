package com.mindscribe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mindscribe.R;
import com.mindscribe.utils.AppLockManager;
import com.mindscribe.utils.ValidationUtils;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_UNLOCK = "unlock";
    public static final String MODE_SETUP = "setup";

    private TextView tvTitle, tvSubtitle;
    private TextInputLayout tilPin, tilConfirmPin;
    private TextInputEditText etPin, etConfirmPin;
    private MaterialButton btnPrimary, btnBiometric;

    private AppLockManager appLockManager;
    private boolean setupMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        appLockManager = new AppLockManager(this);
        setupMode = MODE_SETUP.equals(getIntent().getStringExtra(EXTRA_MODE));

        initViews();
        configureMode();
        setListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_lock_title);
        tvSubtitle = findViewById(R.id.tv_lock_subtitle);
        tilPin = findViewById(R.id.til_pin);
        tilConfirmPin = findViewById(R.id.til_confirm_pin);
        etPin = findViewById(R.id.et_pin);
        etConfirmPin = findViewById(R.id.et_confirm_pin);
        btnPrimary = findViewById(R.id.btn_primary);
        btnBiometric = findViewById(R.id.btn_biometric);
    }

    private void configureMode() {
        if (setupMode) {
            tvTitle.setText("Create App Lock");
            tvSubtitle.setText("Choose a 4 to 6 digit PIN for your private diary.");
            tilConfirmPin.setVisibility(View.VISIBLE);
            btnPrimary.setText("Enable App Lock");
            btnBiometric.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Unlock MindScribe");
            tvSubtitle.setText("Enter your private PIN to continue.");
            tilConfirmPin.setVisibility(View.GONE);
            btnPrimary.setText("Unlock");
            configureBiometric();
        }
    }

    private void configureBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        btnBiometric.setVisibility(result == BiometricManager.BIOMETRIC_SUCCESS
                ? View.VISIBLE : View.GONE);
    }

    private void setListeners() {
        btnPrimary.setOnClickListener(v -> {
            if (setupMode) {
                setupLock();
            } else {
                unlockWithPin();
            }
        });

        btnBiometric.setOnClickListener(v -> showBiometricPrompt());

        etPin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPin.setError(null);
        });
        etConfirmPin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilConfirmPin.setError(null);
        });
    }

    private void setupLock() {
        String pin = etPin.getText() != null ? etPin.getText().toString().trim() : "";
        String confirmPin = etConfirmPin.getText() != null
                ? etConfirmPin.getText().toString().trim() : "";

        if (!isValidPin(pin)) {
            tilPin.setError("PIN must be 4 to 6 digits");
            return;
        }

        if (!pin.equals(confirmPin)) {
            tilConfirmPin.setError("PINs do not match");
            return;
        }

        appLockManager.enableLock(pin);
        Toast.makeText(this, "App lock enabled", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void unlockWithPin() {
        String pin = etPin.getText() != null ? etPin.getText().toString().trim() : "";

        if (!isValidPin(pin)) {
            tilPin.setError("Enter your 4 to 6 digit PIN");
            return;
        }

        if (appLockManager.verifyPin(pin)) {
            openMain();
        } else {
            tilPin.setError("Incorrect PIN");
        }
    }

    private boolean isValidPin(String pin) {
        return !ValidationUtils.isEmpty(pin) && pin.matches("\\d{4,6}");
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        openMain();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(AppLockActivity.this,
                                errString, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock MindScribe")
                .setSubtitle("Use your fingerprint or face unlock")
                .setNegativeButtonText("Use PIN")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
