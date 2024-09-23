package hr.tvz.calendarandschedulingapp.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import hr.tvz.calendarandschedulingapp.entities.Event;
import hr.tvz.calendarandschedulingapp.services.ReminderBroadcast;

public class NotificationManagerService {

    private static final String TAG = "NotificationManagerService";
    private final Context context;
    private final DatabaseReference databaseReference;
    private final AlarmManager alarmManager;

    public NotificationManagerService(Context context, String userId) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("events");
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleAllNotifications() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        String eventKey = snapshot.getKey();
                        scheduleNotification(event, eventKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching events: " + databaseError.getMessage());
            }
        });
    }

    public void scheduleNotification(Event event, String eventKey) {
        Intent intent = new Intent(context, ReminderBroadcast.class);
        intent.putExtra("eventTitle", event.getTitle());
        intent.putExtra("eventKey", eventKey);
        intent.putExtra("location", event.getLocation());
        intent.putExtra("startTime", event.getStart());

        int eventId = eventKey.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_IMMUTABLE);

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

        try {
            long currentTimeInMillis = System.currentTimeMillis();
            String currentTime = sdf.format(new Date(currentTimeInMillis));
            Log.d("NotificationScheduler", "Current time: " + currentTime);

            Date startDate = sdf.parse(event.getStart());
            long startTimeInMillis = startDate.getTime();

            long notificationStartTimeInMillis = startTimeInMillis - 2 * 60 * 60 * 1000;
            String notificationStartTime = sdf.format(new Date(notificationStartTimeInMillis));

            if (notificationStartTimeInMillis < currentTimeInMillis) {
                Log.d("NotificationScheduler", "Notification start time is in the past. Scheduling immediate trigger.");
                notificationStartTimeInMillis = currentTimeInMillis + (2 * 60 * 1000);
                Log.d("NotificationScheduler", "Notification will now trigger at: " + sdf.format(new Date(notificationStartTimeInMillis)));
            } else {
                Log.d("NotificationScheduler", "Notification scheduled to trigger at: " + notificationStartTime);
            }

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationStartTimeInMillis, pendingIntent);

            scheduleStopNotification(eventKey, startTimeInMillis);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public void cancelAllNotifications() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        String eventKey = snapshot.getKey();
                        cancelNotification(event, eventKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching events for cancellation: " + databaseError.getMessage());
            }
        });
    }

    public void cancelNotification(Event event, String eventKey) {
        int eventId = eventKey.hashCode();
        Intent intent = new Intent(context, ReminderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    public void cancelNotificationByKey(String eventKey) {
        int eventId = eventKey.hashCode();
        Intent intent = new Intent(context, ReminderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    private void scheduleStopNotification(String eventKey, long stopTimeInMillis) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.putExtra("action", "stop");
        intent.putExtra("eventKey", eventKey);

        int stopEventId = ("stop_" + eventKey).hashCode();
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, stopEventId, intent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, stopTimeInMillis, stopPendingIntent);
    }

}
