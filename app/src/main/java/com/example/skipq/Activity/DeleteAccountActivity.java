package com.example.skipq.Activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.skipq.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class DeleteAccountActivity extends AppCompatActivity {
    private static final String TAG = "DeleteAccountActivity";
    private TextInputEditText passwordEditText;
    private Button deleteButton;
    private TextView backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userRole;
    private String restaurantId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        userRole = getIntent().getStringExtra("userRole");
        restaurantId = getIntent().getStringExtra("restaurantId");

        backButton=findViewById(R.id.backButton);
        passwordEditText = findViewById(R.id.deleteAccountPassword);
        deleteButton = findViewById(R.id.deleteAccountButton);
        scaleUIElements(findViewById(android.R.id.content));

        deleteButton.setOnClickListener(v -> verifyAndDeleteAccount());
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
    private void verifyAndDeleteAccount() {
        String password = passwordEditText.getText().toString().trim();

        if (password.isEmpty()) {
            passwordEditText.setError("Please enter your password");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> showDeleteConfirmationDialog(user))
                    .addOnFailureListener(e -> {
                        Toast.makeText(DeleteAccountActivity.this,
                                "Incorrect password", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private void scaleUIElements(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = Math.min(screenWidth / (360 * density), 1.5f); // Reference width: 360dp, cap at 1.5x

        // Scale Top Bar (LinearLayout)
        ConstraintLayout topBar = view.findViewById(R.id.topBar);
        if (topBar != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
            params.topMargin = (int) (24 * density * scaleFactor);
            params.bottomMargin = (int) (24 * density * scaleFactor);
            topBar.setPadding(
                    (int) (16 * density * scaleFactor),
                    (int) (24 * density * scaleFactor),
                    (int) (16 * density * scaleFactor),
                    (int) (24 * density * scaleFactor)
            );
            topBar.setLayoutParams(params);
        }

        TextView backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
            backButton.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (6 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (6 * density * scaleFactor)
            );
            backButton.setLayoutParams(params);
        }

        // Scale Logo Text
        TextView logoText = view.findViewById(R.id.text);
        if (logoText != null) {
            logoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) logoText.getLayoutParams();
            logoText.setLayoutParams(params);
        }

        // Scale CardView
        androidx.cardview.widget.CardView cardView = view.findViewById(R.id.cardView);
        if (cardView != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
            params.width = (int) (335 * density * scaleFactor); // Constrain CardView width
            params.leftMargin = (int) (20 * density * scaleFactor);
            params.rightMargin = (int) (20 * density * scaleFactor);
            params.bottomMargin = (int) (20 * density * scaleFactor);
            cardView.setRadius((int) (20 * density * scaleFactor));
            cardView.setLayoutParams(params);
        }

        // Scale Title Text
        TextView titleText = view.findViewById(R.id.title);
        if (titleText != null) {
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) titleText.getLayoutParams();
            params.bottomMargin = (int) (8 * density * scaleFactor);
            titleText.setLayoutParams(params);
        }

        // Scale Subtitle Text
        TextView subtitleText = view.findViewById(R.id.subtitle);
        if (subtitleText != null) {
            subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) subtitleText.getLayoutParams();
            params.bottomMargin = (int) (16 * density * scaleFactor);
            subtitleText.setLayoutParams(params);
        }

        // Scale TextInputLayout
        com.google.android.material.textfield.TextInputLayout textInputLayout = view.findViewById(R.id.textInputLayout1);
        if (textInputLayout != null) {
            textInputLayout.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor)
            );
            ViewGroup.LayoutParams params = textInputLayout.getLayoutParams();
            params.width = (int) (280 * density * scaleFactor); // Match TextInputEditText minWidth
            textInputLayout.setLayoutParams(params);
        }

        // Scale Email EditText
        EditText emailEditText = view.findViewById(R.id.reset_password);
        if (emailEditText != null) {
            emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            emailEditText.setMinimumWidth((int) (280 * density * scaleFactor));
        }

        // Scale Reset Button
        Button resetButton = view.findViewById(R.id.deleteAccountButton);
        if (resetButton != null) {
            resetButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.LayoutParams params = resetButton.getLayoutParams();
            params.width = (int) (280 * density * scaleFactor); // Match TextInputEditText minWidth
            params.height = (int) (56 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) resetButton.getLayoutParams();
            marginParams.topMargin = (int) (16 * density * scaleFactor);
            resetButton.setLayoutParams(params);
        }
    }
    private void showDeleteConfirmationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount(user))
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show();
    }

    private void deleteAccount(FirebaseUser user) {
        String userId = user.getUid();
        Log.d(TAG, "Starting account deletion for user: " + userId + ", role: " + userRole);

        // First delete all device tokens
        deleteDeviceTokens(userId, () -> {
            // Then delete user document
            db.collection("users").document(userId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User document deleted: " + userId);
                        if ("restaurant".equals(userRole)) {
                            deleteRestaurantData(userId, user);
                        } else {
                            deleteFirebaseUser(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete user document", e);
                        Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                        // Still try to delete the Firebase user
                        deleteFirebaseUser(user);
                    });
        });
    }

    private void deleteDeviceTokens(String userId, Runnable onComplete) {
        db.collection("users").document(userId).collection("deviceTokens")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task<?>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        tasks.add(doc.getReference().delete());
                    }
                    if (tasks.isEmpty()) {
                        onComplete.run();
                        return;
                    }
                    Tasks.whenAllComplete(tasks)
                            .addOnSuccessListener(task -> {
                                Log.d(TAG, "All device tokens deleted");
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete some device tokens", e);
                                onComplete.run(); // Continue anyway
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch device tokens", e);
                    onComplete.run(); // Continue anyway
                });
    }

    private void deleteRestaurantData(String userId, FirebaseUser user) {
        db.collection("FoodPlaces")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.w(TAG, "No restaurant found for user");
                        deleteFirebaseUser(user);
                        return;
                    }

                    List<Task<?>> deletionTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String restaurantId = doc.getId();
                        String restaurantName = doc.getString("name");
                        String sanitizedName = restaurantName != null ?
                                restaurantName.replaceAll("[^a-zA-Z0-9]", "_") : restaurantId;

                        Log.d(TAG, "Deleting restaurant: " + restaurantId);

                        // Add restaurant document deletion to tasks
                        deletionTasks.add(db.collection("FoodPlaces").document(restaurantId).delete());

                        // Add addresses deletion to tasks
                        deletionTasks.add(deleteSubcollection(restaurantId, "Addresses"));

                        // Add menu and menu items deletion to tasks
                        deletionTasks.add(deleteMenuAndItems(restaurantId));

                        // Add logo deletion to tasks
                        if (sanitizedName != null) {
                            deletionTasks.add(storage.getReference()
                                    .child("restaurant_logos/" + sanitizedName + "_logo.jpg")
                                    .delete()
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Logo deleted for: " + restaurantId))
                                    .addOnFailureListener(e ->
                                            Log.w(TAG, "Failed to delete logo for: " + restaurantId, e)));
                        }

                        // Delete any global items associated with this restaurant
                        deletionTasks.add(deleteGlobalItems(restaurantId));
                    }

                    // Execute all deletions
                    Tasks.whenAllComplete(deletionTasks)
                            .addOnSuccessListener(task -> {
                                Log.d(TAG, "All restaurant data deleted successfully");
                                deleteFirebaseUser(user);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Some restaurant data failed to delete", e);
                                deleteFirebaseUser(user); // Continue with user deletion anyway
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query restaurant data", e);
                    deleteFirebaseUser(user); // Continue with user deletion anyway
                });
    }

    private Task<Void> deleteMenuAndItems(String restaurantId) {
        return db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .get()
                .continueWithTask(task -> {
                    List<Task<?>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot menuDoc : task.getResult()) {
                        // Delete all items in each menu
                        tasks.add(menuDoc.getReference().collection("Items")
                                .get()
                                .continueWithTask(itemsTask -> {
                                    List<Task<?>> itemTasks = new ArrayList<>();
                                    for (QueryDocumentSnapshot itemDoc : itemsTask.getResult()) {
                                        itemTasks.add(itemDoc.getReference().delete());
                                    }
                                    return Tasks.whenAll(itemTasks);
                                }));
                        // Delete the menu document itself
                        tasks.add(menuDoc.getReference().delete());
                    }
                    return Tasks.whenAll(tasks);
                });
    }

    private Task<Void> deleteGlobalItems(String restaurantId) {
        return db.collection("Items")
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .continueWithTask(task -> {
                    List<Task<?>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        tasks.add(doc.getReference().delete());
                    }
                    return Tasks.whenAll(tasks);
                });
    }
    private Task<Void> deleteSubcollection(String restaurantId, String subcollection) {
        return db.collection("FoodPlaces").document(restaurantId)
                .collection(subcollection)
                .get()
                .continueWithTask(task -> {
                    List<Task<?>> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        tasks.add(doc.getReference().delete());
                    }
                    return Tasks.whenAll(tasks);
                });
    }

    private void deleteFirebaseUser(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firebase user deleted successfully");
                    completeDeletion();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete Firebase user", e);
                    Toast.makeText(this,
                            "Account deletion incomplete. Please contact support.",
                            Toast.LENGTH_LONG).show();
                    completeDeletion();
                });
    }

    private void completeDeletion() {
        mAuth.signOut();
        Toast.makeText(this, "Account deletion completed", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    }
