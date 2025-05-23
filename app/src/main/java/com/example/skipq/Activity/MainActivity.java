package com.example.skipq.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.skipq.Fragment.CartFragment;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button LoginButton;
    private EditText Loginpassword;
    private EditText Loginemail;
    private CheckBox CheckBox;
    private TextView signupRedirectText;
    private TextView forgotPassword;
    private FirebaseFirestore db;
    private Button testUserButton;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Loginemail = findViewById(R.id.Loginemail);
        Loginpassword = findViewById(R.id.Loginpassword);
        LoginButton = findViewById(R.id.LoginButton);
        signupRedirectText = findViewById(R.id.SignUpRedirectText);
        CheckBox = findViewById(R.id.checkbox);
        forgotPassword = findViewById(R.id.forgot_password);
        testUserButton = findViewById(R.id.testUserButton);

        testUserButton.setOnClickListener(view -> signInTestUser());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notifications disabled. You may miss order updates.", Toast.LENGTH_LONG).show();
                    }
                });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
        // Check notification permission on start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                checkNotificationPermission();
            }
        }

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
    @Override
    public void onBackPressed() {

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
    private void signInTestUser() {
        String testEmail = "individualproject2025@gmail.com";
        String testPassword = "Samsung2025";

        mAuth.signInWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Test user login successful", Toast.LENGTH_SHORT).show();
                            checkUserRole(user);
                            registerDeviceToken(user.getUid());
                        } else {
                            createTestUser(testEmail, testPassword);
                        }
                    } else {
                        createTestUser(testEmail, testPassword);
                    }
                });
    }

    private void createTestUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Auto-verify email for test user
                            user.updateEmail(email).addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    // Simulate email verification (for testing purposes)
                                    db.collection("users").document(user.getUid())
                                            .set(new HashMap<String, Object>() {{
                                                put("email", email);
                                                put("role", "user"); // Default role; adjust as needed
                                                put("rememberMe", false);
                                            }})
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(MainActivity.this, "Test user created and logged in", Toast.LENGTH_SHORT).show();
                                                checkUserRole(user);
                                                registerDeviceToken(user.getUid());
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(MainActivity.this, "Failed to create test user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to set email: " + verifyTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to create test user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void registerDeviceToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("FCM", "Failed to get token", task.getException());
                return;
            }

            String token = task.getResult();

            // Check if user is a restaurant
            db.collection("FoodPlaces")
                    .whereEqualTo("uid", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Restaurant user
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                document.getReference().update("deviceToken", token)
                                        .addOnSuccessListener(aVoid -> Log.d("FCM", "Restaurant token updated"))
                                        .addOnFailureListener(e -> Log.e("FCM", "Failed to update restaurant token: " + e.getMessage()));
                            }
                        } else {
                            // Regular user
                            db.collection("users").document(userId)
                                    .update("deviceToken", token)
                                    .addOnSuccessListener(aVoid -> Log.d("FCM", "User token updated"))
                                    .addOnFailureListener(e -> Log.e("FCM", "Failed to update user token: " + e.getMessage()));
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
                        String role = documentSnapshot.getString("role");
                        Log.d("MainActivity", "User role: " + role);
                        if ("admin".equals(role)) {
                            Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                            intent.putExtra("userRole", "admin");
                            updateRememberMe(user, intent);
                            startActivity(intent);
                            finish();
                        } else if ("restaurant".equals(role)) {
                            String restaurantId = documentSnapshot.getString("restaurantId");
                            if (restaurantId != null) {
                                db.collection("FoodPlaces").document(restaurantId).get()
                                        .addOnSuccessListener(restaurantSnapshot -> {
                                            if (restaurantSnapshot.exists()) {
                                                Boolean isApproved = restaurantSnapshot.getBoolean("isApproved");
                                                Log.d("MainActivity", "Restaurant " + restaurantId + " isApproved: " + isApproved);
                                                Intent intent;
                                                if (isApproved != null && isApproved) {
                                                    intent = new Intent(MainActivity.this, HomeActivity.class);
                                                    intent.putExtra("userRole", "restaurant");
                                                    intent.putExtra("restaurantId", restaurantId);
                                                    intent.putExtra("FRAGMENT_TO_LOAD", "RESTAURANT_DASHBOARD");
                                                } else {
                                                    intent = new Intent(MainActivity.this, RestaurantPendingActivity.class);
                                                    intent.putExtra("restaurantId", restaurantId);
                                                }
                                                updateRememberMe(user, intent);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Log.e("MainActivity", "Restaurant document not found for ID: " + restaurantId);
                                                Toast.makeText(MainActivity.this, "Restaurant not found", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("MainActivity", "Error checking restaurant: " + e.getMessage(), e);
                                            Toast.makeText(MainActivity.this, "Error checking restaurant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Log.e("MainActivity", "Restaurant ID not found for user: " + user.getUid());
                                Toast.makeText(MainActivity.this, "Restaurant ID not found", Toast.LENGTH_SHORT).show();
                            }
                        } else if ("user".equals(role)) {
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            intent.putExtra("userRole", "user");
                            updateRememberMe(user, intent);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("MainActivity", "Undefined role for user: " + user.getUid());
                            Toast.makeText(MainActivity.this, "Account role not defined", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("MainActivity", "User document not found for UID: " + user.getUid());
                        Toast.makeText(MainActivity.this, "Account not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error checking user role: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Error checking user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRememberMe(FirebaseUser user, Intent intent) {
        if (CheckBox.isChecked()) {
            db.collection("users").document(user.getUid())
                    .update("rememberMe", true, "email", user.getEmail())
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Updated rememberMe for user: " + user.getUid()))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update rememberMe: " + e.getMessage()));
        }
    }


    public void updateCart() {
        CartFragment cartFragment = (CartFragment) getSupportFragmentManager().findFragmentByTag("CartFragment");
        if (cartFragment != null) {
            cartFragment.refreshCart();
        }
    }
}