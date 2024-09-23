package hr.tvz.calendarandschedulingapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import hr.tvz.calendarandschedulingapp.entities.Event;

public class AddEventActivity extends AppCompatActivity {

    private EditText eventTitle;
    private Switch allDayToggle;
    private TextView startDate;
    private TextView startTime;
    private TextView endDate;
    private TextView endTime;
    private Spinner repeatSpinner;
    private EditText locationInput;
    private EditText notesInput;
    private EditText participantsInput;
    private Button closeButton;
    private Button saveButton;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        String userId = getIntent().getStringExtra("USER_ID");

        eventTitle = findViewById(R.id.event_title);
        allDayToggle = findViewById(R.id.all_day_toggle);
        startDate = findViewById(R.id.start_date);
        startTime = findViewById(R.id.start_time);
        endDate = findViewById(R.id.end_date);
        endTime = findViewById(R.id.end_time);
        repeatSpinner = findViewById(R.id.repeat_spinner);
        locationInput = findViewById(R.id.add_location);
        notesInput = findViewById(R.id.add_notes);
        participantsInput = findViewById(R.id.add_participants);
        closeButton = findViewById(R.id.close_button);
        saveButton = findViewById(R.id.save_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.repeat_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeActivity(userId);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEvent();
            }
        });

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(startDate);
            }
        });

        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(endDate);
            }
        });

        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(startTime);
            }
        });

        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(endTime);
            }
        });
    }


    private void closeActivity(String userId) {
        Intent intent = new Intent(AddEventActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void saveEvent() {
        String userId = getIntent().getStringExtra("USER_ID");
        String title = eventTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAllDay = allDayToggle.isChecked();
        String start = startDate.getText().toString() + " " + startTime.getText().toString();
        String end = endDate.getText().toString() + " " + endTime.getText().toString();

        Object selectedItem = repeatSpinner.getSelectedItem();
        String repeat = selectedItem != null ? selectedItem.toString() : "";

        String location = locationInput.getText().toString();
        String notes = notesInput.getText().toString();
        String participants = participantsInput.getText().toString();

        Event event = new Event(title, isAllDay, start, end, repeat, location, notes, participants);

        saveEventToDatabase(userId, event);

        if (!participants.isEmpty()) {
            findAndSaveEventForParticipant(participants, event, userId);
        }
        closeActivity(userId);
    }

    private void saveEventToDatabase(String userId, Event event) {
        mDatabase.child("users").child(userId).child("events").push().setValue(event)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Event Saved Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to Save Event", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", "Error saving event", task.getException());
                    }
                });
    }

    private void findAndSaveEventForParticipant(String participantIdentifier, Event event, String creatorUserId) {
        mDatabase.child("users").orderByChild("email").equalTo(participantIdentifier).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    saveEventForParticipant(dataSnapshot, event, creatorUserId);
                } else {
                    mDatabase.child("users").orderByChild("username").equalTo(participantIdentifier).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                saveEventForParticipant(dataSnapshot, event, creatorUserId);
                            } else {
                                Toast.makeText(AddEventActivity.this, "Participant not found.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("FirebaseError", "Error finding participant by username", databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Error finding participant by email", databaseError.toException());
            }
        });
    }

    private void saveEventForParticipant(DataSnapshot dataSnapshot, Event event, String creatorUserId) {
        mDatabase.child("users").child(creatorUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot creatorSnapshot) {
                String creatorUsername = creatorSnapshot.getValue(String.class);
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String participantId = userSnapshot.getKey();
                    String participantUsername = userSnapshot.child("username").getValue(String.class);

                     Event participantEvent = new Event(
                            event.getTitle(),
                            event.isAllDay(),
                            event.getStart(),
                            event.getEnd(),
                            event.getRepeat(),
                            event.getLocation(),
                            event.getNotes(),
                            creatorUsername
                    );

                    saveEventToDatabase(participantId, participantEvent);

                    event.setParticipants(participantUsername);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching creator's username", databaseError.toException());
            }
        });
    }



    private void showDatePickerDialog(final TextView dateTextView) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                dateTextView.setText(selectedDate);
            }
        }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePickerDialog(final TextView timeTextView) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                timeTextView.setText(selectedTime);
            }
        }, hour, minute, true);

        timePickerDialog.show();
    }
}
