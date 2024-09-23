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

public class EditEventActivity extends AppCompatActivity {

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
    private String userId;
    private String eventId;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = getIntent().getStringExtra("USER_ID");
        eventId = getIntent().getStringExtra("EVENT_ID");
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

        loadEventDetails();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeActivity();
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

    private void loadEventDetails() {

        mDatabase.child("users").child(userId).child("events").child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentEvent = dataSnapshot.getValue(Event.class);
                if (currentEvent != null) {
                    eventTitle.setText(currentEvent.getTitle());
                    allDayToggle.setChecked(currentEvent.isAllDay());
                    startDate.setText(currentEvent.getStart().split(" ")[0]);
                    startTime.setText(currentEvent.getStart().split(" ")[1]);
                    endDate.setText(currentEvent.getEnd().split(" ")[0]);
                    endTime.setText(currentEvent.getEnd().split(" ")[1]);
                    locationInput.setText(currentEvent.getLocation());
                    notesInput.setText(currentEvent.getNotes());
                    participantsInput.setText(currentEvent.getParticipants());
                    selectSpinnerItemByValue(repeatSpinner, currentEvent.getRepeat());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Failed to load event details", databaseError.toException());
            }
        });
    }

    private void selectSpinnerItemByValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void closeActivity() {
        Intent intent = new Intent(EditEventActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    private void saveEvent() {
        String title = eventTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAllDay = allDayToggle.isChecked();
        String start = startDate.getText().toString() + " " + startTime.getText().toString();
        String end = endDate.getText().toString() + " " + endTime.getText().toString();
        String repeat = repeatSpinner.getSelectedItem().toString();
        String location = locationInput.getText().toString();
        String notes = notesInput.getText().toString();
        String participants = participantsInput.getText().toString();

        currentEvent.setTitle(title);
        currentEvent.setAllDay(isAllDay);
        currentEvent.setStart(start);
        currentEvent.setEnd(end);
        currentEvent.setRepeat(repeat);
        currentEvent.setLocation(location);
        currentEvent.setNotes(notes);
        currentEvent.setParticipants(participants);

        mDatabase.child("users").child(userId).child("events").child(eventId).setValue(currentEvent)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditEventActivity.this, "Event Updated Successfully", Toast.LENGTH_SHORT).show();
                        closeActivity();
                    } else {
                        Toast.makeText(EditEventActivity.this, "Failed to Update Event", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", "Error updating event", task.getException());
                    }
                });
    }

    private void showDatePickerDialog(final TextView dateTextView) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
            dateTextView.setText(selectedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePickerDialog(final TextView timeTextView) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String selectedTime = String.format("%02d:%02d", hourOfDay, minute1);
            timeTextView.setText(selectedTime);
        }, hour, minute, true);

        timePickerDialog.show();
    }
}
