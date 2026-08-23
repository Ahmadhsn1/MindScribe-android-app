package com.mindscribe.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mindscribe.R;
import com.mindscribe.fragments.HomeFragment;
import com.mindscribe.fragments.LunaFragment;
import com.mindscribe.fragments.NotesFragment;
import com.mindscribe.fragments.ProfileFragment;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.mindscribe.workers.LunaNotificationWorker;
import java.util.concurrent.TimeUnit;
import com.mindscribe.utils.NotificationHelper;

/**
 * Main container activity hosting 3 fragments via Bottom Navigation.
 * Demonstrates: Fragment management, Navigation patterns
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Notification Channels for Reminders
        NotificationHelper.createNotificationChannels(this);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
        
        scheduleLunaNotifications();

        setupBottomNavigation();
    }

    private void scheduleLunaNotifications() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                LunaNotificationWorker.class, 1, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LunaReminders",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // Lambda-style conditional — clean switch
            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_notes) {
                selectedFragment = new NotesFragment();
            } else if (id == R.id.nav_luna) {
                selectedFragment = new LunaFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Called from fragments to switch tabs programmatically */
    public void switchToNotesTab() {
        bottomNav.setSelectedItemId(R.id.nav_notes);
    }
}
