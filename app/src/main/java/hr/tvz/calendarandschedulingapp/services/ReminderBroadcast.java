package hr.tvz.calendarandschedulingapp.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hr.tvz.calendarandschedulingapp.R;
import hr.tvz.calendarandschedulingapp.services.NotificationActionReceiver;

public class ReminderBroadcast extends BroadcastReceiver {

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String eventTitle = intent.getStringExtra("eventTitle");
        String eventKey = intent.getStringExtra("eventKey");
        String location = intent.getStringExtra("location");
        String startTime = intent.getStringExtra("startTime");

        String contentText = String.format("Hey, your event \"%s\" at %s is starting at %s.", eventTitle, location, startTime);

        Intent dismissIntent = new Intent(context, NotificationActionReceiver.class);
        dismissIntent.putExtra("action", "dismiss");
        dismissIntent.putExtra("eventKey", eventKey);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, eventKey.hashCode(), dismissIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, NotificationActionReceiver.class);
        stopIntent.putExtra("action", "stop");
        stopIntent.putExtra("eventKey", eventKey);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, ("stop_" + eventKey).hashCode(), stopIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "eventReminderChannel")
                .setSmallIcon(R.drawable.ic_event_reminder)
                .setContentTitle("Event Reminder")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_dismiss, context.getString(R.string.dismiss), dismissPendingIntent)
                .addAction(R.drawable.ic_stop, context.getString(R.string.stop), stopPendingIntent);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(eventKey.hashCode(), builder.build());
    }
}