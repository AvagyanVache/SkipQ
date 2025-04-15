package com.example.skipq.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.skipq.Fragment.CartFragment;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button LoginButton;
    private EditText Loginpassword;
    private EditText Loginemail;
    private CheckBox CheckBox;
    private TextView signupRedirectText;
    private TextView forgotPassword;
    private FirebaseFirestore db;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Loginemail = findViewById(R.id.Loginemail);
        Loginpassword = findViewById(R.id.Loginpassword);
        LoginButton = findViewById(R.id.LoginButton);
        signupRedirectText = findViewById(R.id.SignUpRedirectText);
        CheckBox = findViewById(R.id.checkbox);
        forgotPassword = findViewById(R.id.forgot_password);

        // Request notification permission for Android 13+
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notifications disabled. You may miss order updates.", Toast.LENGTH_LONG).show();
                    }
                });

        // Check notification permission on start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                checkNotificationPermission();
            }
        }

        // Check if user is already logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && user.isEmailVerified()) {
                    checkUserRole(user);
                    registerDeviceToken(user.getUid());
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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Notifications")
                    .setMessage("Notifications are required to receive order updates. Please enable them.")
                    .setPositiveButton("Settings", (dialog, which) ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton("Cancel", (dialog, which) ->
                            Toast.makeText(this, "Notifications disabled. You may miss updates.", Toast.LENGTH_LONG).show())
                    .show();
        }
    }

    private void registerDeviceToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("FCM", "Failed to get token", task.getException());
                return;
            }

            String token = task.getResult();

            // Save token for user (if in users collection)
            db.collection("users").document(userId)
                    .update("deviceToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "User token updated"))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to update user token: " + e.getMessage()));

            // Save token for restaurant (if in FoodPlaces collection)
            db.collection("FoodPlaces")
                    .whereEqualTo("uid", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                document.getReference().update("deviceToken", token)
                                        .addOnSuccessListener(aVoid -> Log.d("FCM", "Restaurant token updated"))
                                        .addOnFailureListener(e -> Log.e("FCM", "Failed to update restaurant token: " + e.getMessage()));
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to find restaurant doc: " + e.getMessage()));
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
                            registerDeviceToken(user.getUid());
                        } else {
                            Toast.makeText(MainActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
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