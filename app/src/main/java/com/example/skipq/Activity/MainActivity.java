package com.example.skipq.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skipq.Fragment.CartFragment;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button LoginButton;
    private EditText Loginpassword;
    private EditText Loginemail;
    private CheckBox CheckBox;
    private TextView signupRedirectText;
    private TextView forgotPassword;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        mAuth = FirebaseAuth.getInstance();

        Loginemail = findViewById(R.id.Loginemail);
        Loginpassword = findViewById(R.id.Loginpassword);
        LoginButton = findViewById(R.id.LoginButton);
        signupRedirectText = findViewById(R.id.SignUpRedirectText);
        CheckBox = findViewById(R.id.checkbox);
        forgotPassword = findViewById(R.id.forgot_password);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && user.isEmailVerified()) {
                    checkUserRole(user);
                }
            });
        }

        CheckBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (mAuth.getCurrentUser() != null) {
                DocumentReference userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
                userRef.update("rememberMe", isChecked)
                        .addOnFailureListener(e ->
                                Toast.makeText(MainActivity.this, "Failed to update preference", Toast.LENGTH_SHORT).show()
                        );
            }
        });

        forgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        LoginButton.setOnClickListener(view -> {
            if (!validateEmail() || !validatePassword()) {
                Toast.makeText(MainActivity.this, "Invalid information", Toast.LENGTH_SHORT).show();
            } else {
                signInUser();
            }
        });

        signupRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });
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

    public void signInUser() {
        String email = Loginemail.getText().toString().trim();
        String password = Loginpassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Email or password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            checkUserRole(user);
                        } else {
                            Toast.makeText(MainActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
        // Check if user is a regular user
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        intent.putExtra("userRole", "user");
                        if (CheckBox.isChecked()) {
                            DocumentReference userRef = db.collection("users").document(user.getUid());
                            userRef.update("rememberMe", true, "email", user.getEmail());
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        // Check if user is a restaurant
                        db.collection("FoodPlaces").whereEqualTo("uid", user.getUid()).get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        String restaurantId = querySnapshot.getDocuments().get(0).getId();
                                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                        intent.putExtra("userRole", "restaurant");
                                        intent.putExtra("restaurantId", restaurantId);
                                        intent.putExtra("FRAGMENT_TO_LOAD", "RESTAURANT_DASHBOARD");
                                        if (CheckBox.isChecked()) {
                                            DocumentReference userRef = db.collection("FoodPlaces").document(restaurantId);
                                            userRef.update("rememberMe", true, "email", user.getEmail());
                                        }
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Account not found in users or restaurants", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error checking restaurant role: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error checking user role: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void updateCart() {
        CartFragment cartFragment = (CartFragment) getSupportFragmentManager().findFragmentByTag("CartFragment");
        if (cartFragment != null) {
            cartFragment.refreshCart();
        }
    }
}