package com.mindscribe.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Manages App Lock security using EncryptedSharedPreferences for professional-grade protection.
 */
public class AppLockManager {

    private static final String PREFS_NAME = "mindscribe_secure_lock";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";

    private SharedPreferences prefs;

    public AppLockManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to standard if encryption fails (highly unlikely on supported SDKs)
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public boolean isLockEnabled() {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false) && hasPin();
    }

    public boolean hasPin() {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT);
    }

    public void enableLock(String pin) {
        String salt = createSalt();
        prefs.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hashPin(pin, salt))
                .putBoolean(KEY_LOCK_ENABLED, true)
                .apply();
    }

    public void disableLock() {
        prefs.edit()
                .remove(KEY_PIN_SALT)
                .remove(KEY_PIN_HASH)
                .putBoolean(KEY_LOCK_ENABLED, false)
                .apply();
    }

    public boolean verifyPin(String pin) {
        String salt = prefs.getString(KEY_PIN_SALT, null);
        String storedHash = prefs.getString(KEY_PIN_HASH, null);
        return salt != null && storedHash != null && storedHash.equals(hashPin(pin, salt));
    }

    private String createSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = (salt + ":" + pin).getBytes(StandardCharsets.UTF_8);
            return Base64.encodeToString(digest.digest(input), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash app lock PIN", e);
        }
    }
}
