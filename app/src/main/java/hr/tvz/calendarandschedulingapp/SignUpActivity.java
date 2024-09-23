package hr.tvz.calendarandschedulingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import hr.tvz.calendarandschedulingapp.entities.User;

public class SignUpActivity extends AppCompatActivity {

    private EditText username, email, password, confirmPassword;
    private Button signUpButton;
    private TextView signInLink;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        databaseReference = FirebaseDatabase.getInstance("https://calendarandschedulingapp-default-rtdb.firebaseio.com/")
                .getReference("users");

        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirm_password);
        signUpButton = findViewById(R.id.sign_up_button);
        signInLink = findViewById(R.id.sign_in_link);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIfUserExists();
            }
        });

        signInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void checkIfUserExists() {
        String usernameText = username.getText().toString().trim();
        String emailText = email.getText().toString().trim();

        if (TextUtils.isEmpty(usernameText)) {
            showToast("Username is required");
            username.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(emailText) || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            showToast("Valid email is required");
            email.requestFocus();
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    databaseReference.setValue("initialized").addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            checkUserName(usernameText, emailText);
                        } else {
                            showToast("Database initialization failed. Please try again");
                        }
                    });
                } else {
                    checkUserName(usernameText, emailText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showToast("Database error. Please try again");
            }
        });
    }


    private void checkUserName(String usernameText, String emailText) {
        databaseReference.orderByChild("username").equalTo(usernameText)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            showToast("Username already exists");
                            username.requestFocus();
                        } else {
                            checkIfEmailExists(emailText, usernameText);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showToast("Database error. Please try again");
                    }
                });
    }

    private void checkIfEmailExists(String emailText, String usernameText) {
        databaseReference.orderByChild("email").equalTo(emailText)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            showToast("Email already exists");
                            email.requestFocus();
                        } else {
                            signUpUser(usernameText, emailText);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showToast("Database error. Please try again");
                    }
                });
    }

    private void signUpUser(String usernameText, String emailText) {
        String passwordText = password.getText().toString().trim();
        String confirmPasswordText = confirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(passwordText) || passwordText.length() < 6) {
            showToast("Password must be at least 6 characters");
            password.requestFocus();
            return;
        }

        if (!passwordText.equals(confirmPasswordText)) {
            showToast("Passwords do not match");
            confirmPassword.requestFocus();
            return;
        }

        User user = new User(usernameText, emailText, passwordText);

        databaseReference.push().setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("User registered successfully. You can login now.");
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showToast("Registration failed. Please try again");
                    }
                });
    }

    private void showToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_message));

        TextView text = layout.findViewById(R.id.custom_toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
}
