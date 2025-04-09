package com.example.skipq.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RestaurantSignUpActivity extends AppCompatActivity {

    private EditText restaurantSignupEmail, restaurantSignupPassword, restaurantSignupConfirmPassword;
    private Button continueButton;
    private TextView backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_signup); // Adjust to your first XML filename

        // Initialize views
        backButton = findViewById(R.id.backButton);
        restaurantSignupEmail = findViewById(R.id.restaurant_signup_email);
        restaurantSignupPassword = findViewById(R.id.restaurant_signup_password);
        restaurantSignupConfirmPassword = findViewById(R.id.restaurant_signup_confirm_password);
        continueButton = findViewById(R.id.ContinueButton);

        mAuth = FirebaseAuth.getInstance();

        // Back button click listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous screen
            }
        });

        // Continue button click listener
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = restaurantSignupEmail.getText().toString().trim();
                String password = restaurantSignupPassword.getText().toString();
                String confirmPassword = restaurantSignupConfirmPassword.getText().toString();

                if (validateInput(email, password, confirmPassword)) {
                    signUpUser(email, password);
                }
            }
        });
    }

    private boolean validateInput(String email, String password, String confirmPassword) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void signUpUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                                            // Pass email and password to the next activity
                                            Intent intent = new Intent(RestaurantSignUpActivity.this, RestaurantSignUpActivity2.class);
                                            intent.putExtra("email", email);
                                            intent.putExtra("password", password);
                                            intent.putExtra("uid", user.getUid()); // Pass UID for Firestore
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}