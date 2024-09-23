package hr.tvz.calendarandschedulingapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import hr.tvz.calendarandschedulingapp.entities.Event;
import hr.tvz.calendarandschedulingapp.services.NotificationManagerService;
import hr.tvz.calendarandschedulingapp.services.ReminderBroadcast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private LinearLayout eventContainer;
    private boolean isCalendarExpanded = false;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        createNotificationChannel();

        String userId = getIntent().getStringExtra("USER_ID");
        NotificationManagerService notificationManager = new NotificationManagerService(this, userId);
        notificationManager.scheduleAllNotifications();
        ImageView logoutImageView = findViewById(R.id.logoutImageView);
        logoutImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout(userId);
            }
        });


        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        TextView monthTextView = findViewById(R.id.monthTextView);
        LinearLayout daysContainer = findViewById(R.id.daysContainer);
        LinearLayout datesContainer = findViewById(R.id.datesContainer);
        ImageView arrowImageView = findViewById(R.id.arrowImageView);
        LinearLayout expandedCalendarView = findViewById(R.id.expandedCalendarView);
        GridLayout expandedCalendarGrid = findViewById(R.id.expandedCalendarGrid);
        ImageView addImageView = findViewById(R.id.addImageView);

        eventContainer = findViewById(R.id.eventContainer);

        addImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AddEventActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            }
        });


        String currentMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        monthTextView.setText(currentMonth);


        String[] daysOfWeek = {"M", "T", "W", "T", "F", "S", "S"};

        Calendar calendar = Calendar.getInstance();
        int todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);


        for (int i = 0; i < daysOfWeek.length; i++) {
            TextView dayTextView = new TextView(this);
            dayTextView.setText(daysOfWeek[i]);
            dayTextView.setTextColor(Color.WHITE);
            dayTextView.setGravity(Gravity.CENTER);
            daysContainer.setPadding(0, 0, 0, 10);
            dayTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            daysContainer.addView(dayTextView);
        }


        int startOfWeek = currentDayOfMonth - todayIndex;
        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_MONTH, startOfWeek + i);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            TextView dateTextView = new TextView(this);
            dateTextView.setText(String.valueOf(dayOfMonth));
            dateTextView.setTextColor(Color.WHITE);
            dateTextView.setGravity(Gravity.CENTER);
            dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            datesContainer.setPadding(0, 0, 0, 20);
            datesContainer.addView(dateTextView);

            final int selectedDay = dayOfMonth;
            dateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                    highlightSelectedDate(datesContainer, dateTextView);
                    fetchEventsFromFirebase(userId, selectedDate.getTime());
                }
            });

            if (dayOfMonth == currentDayOfMonth) {
                dateTextView.setBackgroundResource(R.drawable.circle_background);
                fetchEventsFromFirebase(userId, selectedDate.getTime());
            }
        }


        arrowImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCalendarExpanded) {
                    expandedCalendarView.setVisibility(View.GONE);
                    datesContainer.setVisibility(View.VISIBLE);
                    arrowImageView.setImageResource(android.R.drawable.arrow_down_float);

                    updateMainDatesContainer(datesContainer, selectedDate, userId);

                } else {
                    populateExpandedCalendarGrid(expandedCalendarGrid, userId);
                    datesContainer.setVisibility(View.GONE);
                    expandedCalendarView.setVisibility(View.VISIBLE);
                    arrowImageView.setImageResource(android.R.drawable.arrow_up_float);
                }
                isCalendarExpanded = !isCalendarExpanded;
            }
        });

    }

    private void fetchEventsFromFirebase(String userId, Date selectedDate) {
        DatabaseReference userEventsRef = databaseReference.child(userId).child("events");

        SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String selectedDateString = dateFormat.format(selectedDate);

        userEventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Event> events = new ArrayList<>();
                List<String> eventIds = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Event event = snapshot.getValue(Event.class);
                    if (event != null) {
                        String eventDate = event.getStart().split(" ")[0];
                        if (selectedDateString.equals(eventDate)) {
                            events.add(event);
                            eventIds.add(snapshot.getKey());
                        }
                    }
                }

                populateEvents(events, eventIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }




    private void populateEvents(List<Event> events, List<String> eventIds) {
        eventContainer.removeAllViews();

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            String eventId = eventIds.get(i);

            View eventView = getLayoutInflater().inflate(R.layout.event_item, null);
            TextView eventTitle = eventView.findViewById(R.id.eventTitle);
            TextView eventTimeLabel = eventView.findViewById(R.id.eventTimeLabel);
            TextView eventTime = eventView.findViewById(R.id.eventTime);

            eventTitle.setText(event.getTitle());

            String startTimeText = formatTime(event.getStart());
            eventTimeLabel.setText(startTimeText);

            String eventTimeText = event.isAllDay() ? "All Day" : startTimeText + " - " + formatTime(event.getEnd());
            eventTime.setText(eventTimeText);

            eventView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, EditEventActivity.class);
                    intent.putExtra("EVENT_ID", eventId);
                    intent.putExtra("USER_ID", getIntent().getStringExtra("USER_ID"));
                    startActivity(intent);
                }
            });


            eventContainer.addView(eventView);
        }
    }


    private String formatTime(String dateTime) {

        SimpleDateFormat inputFormat = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        try {
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateTime;
        }
    }

    private void populateExpandedCalendarGrid(GridLayout gridLayout, String userId) {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(7);

        int cellSize = 150;
        float textSize = 15;
        int textColor = Color.WHITE;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfMonth = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = daysInMonth + firstDayOfMonth;
        int numberOfRows = (int) Math.ceil(totalCells / 7.0);

        gridLayout.setRowCount(numberOfRows + 1);

        int marginSize = 1;

        String[] daysOfWeek = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < daysOfWeek.length; i++) {
            TextView dayHeader = new TextView(this);
            dayHeader.setText(daysOfWeek[i]);
            dayHeader.setTextColor(textColor);
            dayHeader.setGravity(Gravity.CENTER);
            dayHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.rowSpec = GridLayout.spec(0);
            params.columnSpec = GridLayout.spec(i, 1.0f);
            params.setMargins(marginSize, marginSize, marginSize, -100);
            dayHeader.setLayoutParams(params);
            gridLayout.addView(dayHeader);
        }

        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);


        for (int day = 1; day <= daysInMonth; day++) {
            TextView dateTextView = new TextView(this);
            dateTextView.setText(String.valueOf(day));
            dateTextView.setTextColor(textColor);
            dateTextView.setGravity(Gravity.CENTER);
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            int cellIndex = firstDayOfMonth + day - 1;
            params.rowSpec = GridLayout.spec((cellIndex) / 7 + 1);
            params.columnSpec = GridLayout.spec(cellIndex % 7);
            if (day == today) {
                dateTextView.setBackgroundResource(R.drawable.circle_background);
            }
            params.setMargins(marginSize, marginSize, marginSize, marginSize);
            dateTextView.setLayoutParams(params);
            gridLayout.addView(dateTextView);

            final int selectedDay = day;
            dateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                    highlightSelectedDate(gridLayout, dateTextView);
                    fetchEventsFromFirebase(userId, selectedDate.getTime());
                }
            });
        }
    }

    private void highlightSelectedDate(ViewGroup container, TextView selectedDateView) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                child.setBackgroundResource(0);
            }
        }
        selectedDateView.setBackgroundResource(R.drawable.circle_background);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "EventReminderChannel";
            String description = "Channel for Event Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("eventReminderChannel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void performLogout(String userId) {
        NotificationManagerService notificationManager = new NotificationManagerService(this, userId);
        notificationManager.cancelAllNotifications();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateMainDatesContainer(LinearLayout datesContainer, Calendar selectedDate, String userId) {
        datesContainer.removeAllViews();

        Calendar calendar = (Calendar) selectedDate.clone();
        int todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int startOfWeek = currentDayOfMonth - todayIndex;

        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_MONTH, startOfWeek + i);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            TextView dateTextView = new TextView(this);
            dateTextView.setText(String.valueOf(dayOfMonth));
            dateTextView.setTextColor(Color.WHITE);
            dateTextView.setGravity(Gravity.CENTER);
            dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            datesContainer.setPadding(0, 0, 0, 20);
            datesContainer.addView(dateTextView);

            final int selectedDay = dayOfMonth;
            dateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                    highlightSelectedDate(datesContainer, dateTextView);
                    fetchEventsFromFirebase(userId, selectedDate.getTime());
                }
            });

            if (dayOfMonth == selectedDate.get(Calendar.DAY_OF_MONTH)) {
                dateTextView.setBackgroundResource(R.drawable.circle_background);
            }
        }
    }


}