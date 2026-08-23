package com.mindscribe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mindscribe.R;
import com.mindscribe.utils.AppLockManager;
import com.mindscribe.utils.FirebaseHelper;

/**
 * Splash screen — first impression. Elegant gold fade + scale animation.
 * Expert Refactor: Added safety checks to prevent navigation after activity destruction.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide system UI for full-screen splash
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);
        View     logoContainer = findViewById(R.id.logo_container);

        // Logo scale + fade animation
        AnimationSet logoAnim = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(
                0.6f, 1f, 0.6f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(700);
        AlphaAnimation logoFade = new AlphaAnimation(0f, 1f);
        logoFade.setDuration(700);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(logoFade);
        logoAnim.setFillAfter(true);
        logoContainer.startAnimation(logoAnim);

        // App name slide up after 300ms
        handler.postDelayed(() -> {
            if (isFinishing()) return;
            TranslateAnimation slideUp = new TranslateAnimation(0, 0, 40f, 0);
            slideUp.setDuration(500);
            AlphaAnimation nameAlpha = new AlphaAnimation(0f, 1f);
            nameAlpha.setDuration(500);
            AnimationSet nameAnim = new AnimationSet(true);
            nameAnim.addAnimation(slideUp);
            nameAnim.addAnimation(nameAlpha);
            nameAnim.setFillAfter(true);
            tvAppName.setVisibility(View.VISIBLE);
            tvAppName.startAnimation(nameAnim);
        }, 300);

        // Tagline fade after 600ms
        handler.postDelayed(() -> {
            if (isFinishing()) return;
            AlphaAnimation tagFade = new AlphaAnimation(0f, 1f);
            tagFade.setDuration(600);
            tagFade.setFillAfter(true);
            tvTagline.setVisibility(View.VISIBLE);
            tvTagline.startAnimation(tagFade);
        }, 700);

        // Navigate after splash duration
        navigateRunnable = this::navigateNext;
        handler.postDelayed(navigateRunnable, SPLASH_DURATION);
    }

    private void navigateNext() {
        if (isFinishing()) return;

        Intent intent;
        if (FirebaseHelper.getInstance().isLoggedIn()) {
            if (new AppLockManager(this).isLockEnabled()) {
                intent = new Intent(this, AppLockActivity.class);
                intent.putExtra(AppLockActivity.EXTRA_MODE, AppLockActivity.MODE_UNLOCK);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks and unwanted navigation
        handler.removeCallbacksAndMessages(null);
    }
}
