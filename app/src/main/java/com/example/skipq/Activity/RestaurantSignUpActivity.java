package com.example.skipq.Activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Initialize views
        backButton = findViewById(R.id.backButton);
        restaurantSignupEmail = findViewById(R.id.restaurant_signup_email);
        restaurantSignupPassword = findViewById(R.id.restaurant_signup_password);
        restaurantSignupConfirmPassword = findViewById(R.id.restaurant_signup_confirm_password);
        continueButton = findViewById(R.id.ContinueButton);

        mAuth = FirebaseAuth.getInstance();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous screen
            }
        });

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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }
    @Override
    public void onBackPressed() {

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
                                            Toast.makeText(this, "Verification email sent! Please verify your email.", Toast.LENGTH_LONG).show();
                                            // Periodically check email verification status
                                            checkEmailVerification(user, email, password);
                                        } else {
                                            Toast.makeText(this, "Failed to send verification email: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Sign-up failed: User not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser user, String email, String password) {
        // Disable continue button to prevent multiple clicks
        continueButton.setEnabled(false);
        continueButton.setText("Verify Email...");

        // Create a handler to periodically check verification
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable checkVerification = new Runnable() {
            @Override
            public void run() {
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (user.isEmailVerified()) {
                            // Email verified, proceed to next activity
                            Intent intent = new Intent(RestaurantSignUpActivity.this, RestaurantSignUpActivity2.class);
                            intent.putExtra("email", email);
                            intent.putExtra("password", password);
                            intent.putExtra("uid", user.getUid());
                            startActivity(intent);
                            finish();
                        } else {
                            // Email not verified, check again after 3 seconds
                            handler.postDelayed(this, 3000);
                        }
                    } else {
                        Toast.makeText(RestaurantSignUpActivity.this, "Error checking verification: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        continueButton.setEnabled(true);
                        continueButton.setText("Continue");
                    }
                });
            }
        };
        handler.post(checkVerification);
    }
}