package com.example.skipq.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skipq.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class RestaurantPendingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listenerRegistration;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_pending);

        TextView statusText = findViewById(R.id.pending_status_text);
        statusText.setText("Your restaurant is pending admin approval. You will be notified once reviewed.");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        restaurantId = getIntent().getStringExtra("restaurantId");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || restaurantId == null) {
            Toast.makeText(this, "Error: Invalid user or restaurant ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listenForApprovalStatus(user.getUid());
    }

    private void listenForApprovalStatus(String uid) {
        DocumentReference restaurantRef = db.collection("FoodPlaces").document(restaurantId);
        listenerRegistration = restaurantRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e("RestaurantPending", "Error checking status: " + error.getMessage(), error);
                Toast.makeText(this, "Error checking status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Boolean isApproved = snapshot.getBoolean("isApproved");
                String declineReason = snapshot.getString("declineReason");
                String restaurantName = snapshot.getString("name");
                String email = snapshot.getString("email");

                if (isApproved != null && isApproved) {


                    final ListenerRegistration[] menuListener = new ListenerRegistration[1];

                    menuListener[0] = db.collection("FoodPlaces").document(restaurantId)
                            .collection("Menu").document("DefaultMenu").collection("Items")
                            .limit(1)
                            .addSnapshotListener((menuSnapshot, menuError) -> {
                                if (menuError != null) {
                                    Log.e("RestaurantPending", "Error checking menu: " + menuError.getMessage(), menuError);

                                    Toast.makeText(this, "Error fetching menu", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (menuSnapshot != null && !menuSnapshot.isEmpty()) {

                                    showAcceptanceDialog(restaurantName != null ? restaurantName : "Unknown Restaurant",
                                            email != null ? email : "Unknown Email");
                                    // Remove menu listener to prevent multiple triggers
                                    menuListener[0].remove();
                                }
                            });

                } else if (declineReason != null) {
                    showDeclineReasonDialog(declineReason);
                }
            } else {
                Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                finish();
            }
        });
    }

    // Modified showAcceptanceDialog to navigate to dashboard on OK button press
    private void showAcceptanceDialog(String restaurantName, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restaurant Approved");
        builder.setMessage("The restaurant '" + restaurantName + "' has been approved.\n\n" +
                "You can now log in using your registered email: " + email + " and your password.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            navigateToDashboard();
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("userRole", "restaurant");
        intent.putExtra("restaurantId", restaurantId);
        intent.putExtra("FRAGMENT_TO_LOAD", "RESTAURANT_DASHBOARD");
        startActivity(intent);
        finish();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void showDeclineReasonDialog(String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restaurant Declined");
        builder.setMessage("Your restaurant was declined for the following reason:\n\n" + reason);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            // Delete Firestore data and Storage files
            List<Task<Void>> deletionTasks = new ArrayList<>();

            // Delete Addresses subcollection
            db.collection("FoodPlaces").document(restaurantId).collection("Addresses")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            deletionTasks.add(doc.getReference().delete());
                        }

                        // Delete Menu subcollection and nested Items
                        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                                .get()
                                .addOnSuccessListener(menuSnapshot -> {
                                    for (QueryDocumentSnapshot menuDoc : menuSnapshot) {
                                        deletionTasks.add(menuDoc.getReference().collection("Items")
                                                .get()
                                                .continueWithTask(itemTask -> {
                                                    List<Task<Void>> itemDeletions = new ArrayList<>();
                                                    for (QueryDocumentSnapshot item : itemTask.getResult()) {
                                                        itemDeletions.add(item.getReference().delete());
                                                    }
                                                    return Tasks.whenAll(itemDeletions);
                                                })
                                                .continueWithTask(task -> menuDoc.getReference().delete()));
                                    }

                                    // Delete restaurant document
                                    deletionTasks.add(db.collection("FoodPlaces").document(restaurantId).delete());

                                    // Delete user document
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        deletionTasks.add(db.collection("users").document(user.getUid()).delete());
                                    }

                                    // Delete Storage files (logo and menu images)
                                    String sanitizedName = restaurantId.replaceAll("[^a-zA-Z0-9]", "_");
                                    StorageReference logoRef = FirebaseStorage.getInstance().getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
                                    deletionTasks.add(logoRef.delete().continueWithTask(task -> {
                                        StorageReference menuImagesRef = FirebaseStorage.getInstance().getReference().child("menu_images/" + restaurantId);
                                        return menuImagesRef.listAll().continueWithTask(listTask -> {
                                            List<Task<Void>> imageDeletions = new ArrayList<>();
                                            for (StorageReference item : listTask.getResult().getItems()) {
                                                imageDeletions.add(item.delete());
                                            }
                                            return Tasks.whenAll(imageDeletions);
                                        });
                                    }));

                                    // Execute all deletion tasks
                                    Tasks.whenAllComplete(deletionTasks)
                                            .addOnSuccessListener(tasks -> {
                                                Log.d("RestaurantPending", "Successfully deleted all restaurant data");
                                                mAuth.signOut();
                                                Toast.makeText(RestaurantPendingActivity.this, "Restaurant data deleted.", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(RestaurantPendingActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("RestaurantPending", "Error deleting restaurant data: " + e.getMessage(), e);
                                                Toast.makeText(RestaurantPendingActivity.this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                                Intent intent = new Intent(RestaurantPendingActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("RestaurantPending", "Error fetching menu subcollection: " + e.getMessage(), e);
                                    mAuth.signOut();
                                    Intent intent = new Intent(RestaurantPendingActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RestaurantPending", "Error fetching addresses subcollection: " + e.getMessage(), e);
                        mAuth.signOut();
                        Intent intent = new Intent(RestaurantPendingActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
        });
        builder.setCancelable(false);
        builder.create().show();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}