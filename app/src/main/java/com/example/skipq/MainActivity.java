package com.example.skipq;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button LoginButton;
    EditText Loginpassword;
    EditText Loginemail;
    CheckBox CheckBox;
    TextView signupRedirectText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Loginemail = findViewById(R.id.Loginemail);
        Loginpassword = findViewById(R.id.Loginpassword);
        LoginButton = findViewById(R.id.LoginButton);
        signupRedirectText = findViewById(R.id.SignUpRedirectText);
        CheckBox = findViewById(R.id.checkbox);

        loadRememberMeState();

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateEmail() || !validatePassword()) {
                    Toast.makeText(MainActivity.this, "Invalid information", Toast.LENGTH_SHORT).show();
                } else {
                    checkUser();
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void saveRememberMeState(boolean isChecked) {
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("rememberMe", isChecked);
        editor.apply();
    }

    private void loadRememberMeState() {
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe", false);
        CheckBox.setChecked(rememberMe);
    }

    public boolean validateEmail() {
        String val = Loginemail.getText().toString();
        if (val.isEmpty()) {
            Loginemail.setError("Email can't be empty");
            return false;
        } else {
            Loginemail.setError(null);
            return true;
        }
    }

    public boolean validatePassword() {
        String val = Loginpassword.getText().toString();
        if (val.isEmpty()) {
            Loginpassword.setError("Password can't be empty");
            return false;
        } else {
            Loginpassword.setError(null);
            return true;
        }
    }

    public void checkUser() {
        String userEmail = Loginemail.getText().toString().trim();
        String userPassword = Loginpassword.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("Email").equalTo(userEmail);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Loginemail.setError(null);

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordfromDB = userSnapshot.child("password").getValue(String.class);
                        String emailFromDB = userSnapshot.child("Email").getValue(String.class);

                        if (Objects.equals(emailFromDB, userEmail) && Objects.equals(passwordfromDB, userPassword)) {
                            // If email and password match
                            if (CheckBox.isChecked()) {
                                saveRememberMeState(true);
                            }
                            Toast.makeText(MainActivity.this, "Going home", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Loginpassword.setError("Invalid Password!");
                            Loginpassword.requestFocus();
                        }
                    }
                } else {
                    Loginemail.setError("User does not exist!");
                    Loginemail.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
