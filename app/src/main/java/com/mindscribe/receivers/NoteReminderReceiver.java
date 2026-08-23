package com.mindscribe.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mindscribe.R;
import com.mindscribe.activities.MainActivity;

/**
 * BroadcastReceiver to handle note reminders.
 * Professional implementation with Notification Channels.
 */
public class NoteReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_note_title";
    public static final String EXTRA_TEXT  = "extra_note_text";
    private static final String CHANNEL_ID = "mindscribe_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String text  = intent.getStringExtra(EXTRA_TEXT);

        showNotification(context, title, text);
    }

    private void showNotification(Context context, String title, String text) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 1. Create Notification Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Note Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for your scheduled notes");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 2. Intent to open app when notification is clicked
        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
                .setContentTitle("Reminder: " + (title != null ? title : "Note"))
                .setContentText(text != null ? text : "You have a scheduled note to check.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 4. Show notification
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
