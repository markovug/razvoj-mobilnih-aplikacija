package hr.tvz.calendarandschedulingapp.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        String eventKey = intent.getStringExtra("eventKey");

        if ("dismiss".equals(action)) {
            NotificationManagerCompat.from(context).cancel(eventKey.hashCode());

        } else if ("stop".equals(action)) {
            NotificationManagerService notificationManagerService = new NotificationManagerService(context, getUserIdFromContext(context));
            notificationManagerService.cancelNotificationByKey(eventKey);
            NotificationManagerCompat.from(context).cancel(eventKey.hashCode());
        }
    }

    private String getUserIdFromContext(Context context) {
        return "user_id";
    }
}
