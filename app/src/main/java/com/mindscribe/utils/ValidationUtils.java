package com.mindscribe.utils;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Utility class for input validation.
 * Demonstrates: Static methods, Exception Handling concepts
 */
public class ValidationUtils {

    private ValidationUtils() {} // Prevent instantiation

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public static boolean isEmpty(String text) {
        return TextUtils.isEmpty(text) || text.trim().isEmpty();
    }

    public static boolean passwordsMatch(String p1, String p2) {
        return p1 != null && p1.equals(p2);
    }

    public static String capitalize(String text) {
        if (isEmpty(text)) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
