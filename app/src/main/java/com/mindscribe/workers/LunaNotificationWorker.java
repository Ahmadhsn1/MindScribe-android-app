package com.mindscribe.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mindscribe.R;
import com.mindscribe.activities.MainActivity;

import java.util.Calendar;

public class LunaNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "luna_channel";
    
    private final String[] LUNA_MESSAGES = {
            "It’s been a while since we talked... I’m listening whenever you’re ready.",
            "You haven't lost your streak yet, let's keep it alive!",
            "Rough day? Writing it down might help.",
            "Take a deep breath, and let's write a little.",
            "A small note today makes a big memory tomorrow."
    };

    public LunaNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        sendLunaNotification();
        return Result.success();
    }

    private void sendLunaNotification() {
        Context context = getApplicationContext();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Luna Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Gentle reminders from Luna");
            nm.createNotificationChannel(channel);
        }

        String message = LUNA_MESSAGES[(int) (Math.random() * LUNA_MESSAGES.length)];

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notes) // Use notes icon as fallback
                .setContentTitle("Luna")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
