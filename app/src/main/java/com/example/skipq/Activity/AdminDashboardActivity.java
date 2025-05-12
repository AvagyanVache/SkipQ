package com.example.skipq.Activity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.skipq.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;
    private List<Restaurant> restaurantList;
    private void showDeclineReasonDialog(Context context, String restaurantId, Consumer<String> onDeclineConfirmed) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Decline Restaurant");

        String[] declineReasons = {
                "Incomplete information",
                "Invalid documentation",
                "Policy violation",
                "Other"
        };

        builder.setItems(declineReasons, (dialog, which) -> {
            String selectedReason = declineReasons[which];
            if ("Other".equals(selectedReason)) {
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_decline_reason, null);
                com.google.android.material.textfield.TextInputLayout textInputLayout = dialogView.findViewById(R.id.textInputDeclineReason);
                com.google.android.material.textfield.TextInputEditText reasonInput = dialogView.findViewById(R.id.decline_reason_input);
                reasonInput.setHint("Enter reason for decline");

                AlertDialog inputDialog = new AlertDialog.Builder(context)
                        .setTitle("Specify Decline Reason")
                        .setView(dialogView)
                        .setPositiveButton("Submit", (d, w) -> {
                            String customReason = reasonInput.getText().toString().trim();
                            if (customReason.isEmpty()) {
                                Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show();
                            } else {
                                declineRestaurant(restaurantId, customReason, onDeclineConfirmed);
                            }
                        })
                        .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                        .create();
                inputDialog.show();
            } else {
                declineRestaurant(restaurantId, selectedReason, onDeclineConfirmed);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void declineRestaurant(String restaurantId, String declineReason, Consumer<String> onDeclineConfirmed) {
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        db.collection("FoodPlaces").document(restaurantId)
                .update("declineReason", declineReason, "isDeclined", true)
                .addOnSuccessListener(aVoid -> {
                    db.collection("FoodPlaces").document(restaurantId).get()
                            .addOnSuccessListener(docSnapshot -> {
                                String uid = docSnapshot.getString("uid");
                                String restaurantName = docSnapshot.getString("name");

                                List<Task<Void>> deletionTasks = new ArrayList<>();

                                // Delete subcollections
                                deletionTasks.add(deleteSubcollection("FoodPlaces", restaurantId, "Menu"));
                                deletionTasks.add(deleteSubcollection("FoodPlaces", restaurantId, "Items"));

                                // Delete global Items
                                db.collection("Items")
                                        .whereEqualTo("restaurantId", restaurantId)
                                        .get()
                                        .addOnSuccessListener(itemsSnapshot -> {
                                            for (QueryDocumentSnapshot item : itemsSnapshot) {
                                                deletionTasks.add(item.getReference().delete());
                                            }

                                            // Delete user if exists
                                            if (uid != null) {
                                                deletionTasks.add(db.collection("users").document(uid).delete());
                                            }

                                            // Delete main restaurant doc
                                            deletionTasks.add(db.collection("FoodPlaces").document(restaurantId).delete());

                                            // Delete Storage
                                            String sanitizedName = restaurantId.replaceAll("[^a-zA-Z0-9]", "_");
                                            StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
                                            Task<Void> logoDeleteTask = logoRef.delete().continueWithTask(task -> {
                                                StorageReference menuImagesRef = storage.getReference().child("menu_images/" + restaurantId);
                                                return menuImagesRef.listAll().continueWithTask(listTask -> {
                                                    List<Task<Void>> imageDeletions = new ArrayList<>();
                                                    for (StorageReference imageRef : listTask.getResult().getItems()) {
                                                        imageDeletions.add(imageRef.delete());
                                                    }
                                                    return Tasks.whenAll(imageDeletions);
                                                });
                                            });
                                            deletionTasks.add(logoDeleteTask);

                                            // When All Done
                                            Tasks.whenAllComplete(deletionTasks)
                                                    .addOnSuccessListener(tasks -> {
                                                        verifyDeletion(restaurantId, onDeclineConfirmed);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(AdminDashboardActivity.this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        verifyDeletion(restaurantId, onDeclineConfirmed);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(AdminDashboardActivity.this, "Error deleting global items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminDashboardActivity.this, "Error fetching restaurant data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this, "Error updating decline reason: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private Task<Void> deleteSubcollection(String parentCollection, String docId, String subcollection) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference subRef = db.collection(parentCollection).document(docId).collection(subcollection);
        return subRef.get().continueWithTask(task -> {
            List<Task<Void>> deletions = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult()) {
                deletions.add(doc.getReference().delete());
            }
            return Tasks.whenAll(deletions);
        });
    }


    // Helper method to delete a collection and all its subcollections recursively
    private void deleteCollectionRecursively(DocumentReference document, List<Task<Void>> deletionTasks) {
        // List of known subcollections under FoodPlaces/{restaurantId}
        String[] subcollections = {"Addresses", "Menu"};

        for (String subcollection : subcollections) {
            document.collection(subcollection).get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            // If this is a Menu document, check for nested Items subcollection
                            if (subcollection.equals("Menu")) {
                                doc.getReference().collection("Items").get()
                                        .addOnSuccessListener(itemSnapshot -> {
                                            for (QueryDocumentSnapshot itemDoc : itemSnapshot) {
                                                Log.d("AdminDashboard", "Deleting item: " + itemDoc.getId() + " in Menu/" + doc.getId() + "/Items");
                                                deletionTasks.add(itemDoc.getReference().delete());
                                            }
                                            // Delete the Menu document after its Items
                                            Log.d("AdminDashboard", "Deleting Menu document: " + doc.getId());
                                            deletionTasks.add(doc.getReference().delete());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AdminDashboard", "Error fetching Items for Menu " + doc.getId() + ": " + e.getMessage(), e);
                                        });
                            } else {
                                // Delete other subcollection documents (e.g., Addresses)
                                Log.d("AdminDashboard", "Deleting document: " + doc.getId() + " in collection: " + subcollection);
                                deletionTasks.add(doc.getReference().delete());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminDashboard", "Error fetching subcollection " + subcollection + ": " + e.getMessage(), e);
                    });
        }
    }

    // Verify that all data is deleted
    private void verifyDeletion(String restaurantId, Consumer<String> onDeclineConfirmed) {
        // Check for any remaining Menu subcollections
        db.collection("FoodPlaces").document(restaurantId).collection("Menu").get()
                .addOnSuccessListener(menuSnapshot -> {
                    if (!menuSnapshot.isEmpty()) {
                        Log.e("AdminDashboard", "Menus still exist after deletion for restaurant: " + restaurantId);
                        // Attempt to delete again
                        List<Task<Void>> retryTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot menuDoc : menuSnapshot) {
                            deleteCollectionRecursively(menuDoc.getReference(), retryTasks);
                        }
                        Tasks.whenAll(retryTasks).addOnCompleteListener(task -> {
                            checkGlobalItems(restaurantId, onDeclineConfirmed);
                        });
                    } else {
                        checkGlobalItems(restaurantId, onDeclineConfirmed);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error verifying menus: " + e.getMessage());
                    checkGlobalItems(restaurantId, onDeclineConfirmed);
                });
    }

    private void checkGlobalItems(String restaurantId, Consumer<String> onDeclineConfirmed) {
        db.collection("Items").whereEqualTo("restaurantId", restaurantId).get()
                .addOnSuccessListener(itemsSnapshot -> {
                    if (!itemsSnapshot.isEmpty()) {
                        Log.e("AdminDashboard", "Global items still exist after deletion for restaurant: " + restaurantId);
                        // Delete remaining items
                        List<Task<Void>> itemTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot item : itemsSnapshot) {
                            Log.d("AdminDashboard", "Deleting remaining global item: " + item.getId());
                            itemTasks.add(item.getReference().delete());
                        }
                        Tasks.whenAll(itemTasks).addOnCompleteListener(task -> {
                            finalizeDeletion(restaurantId, onDeclineConfirmed);
                        });
                    } else {
                        finalizeDeletion(restaurantId, onDeclineConfirmed);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error verifying global Items: " + e.getMessage());
                    finalizeDeletion(restaurantId, onDeclineConfirmed);
                });
    }

    // Finalize deletion and notify
    private void finalizeDeletion(String restaurantId, Consumer<String> onDeclineConfirmed) {
        // Final check for restaurant document
        db.collection("FoodPlaces").document(restaurantId).get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        Log.e("AdminDashboard", "Restaurant document still exists: " + restaurantId);
                        // Delete it
                        docSnapshot.getReference().delete().addOnCompleteListener(task -> {
                            onDeclineConfirmed.accept(restaurantId);
                            Toast.makeText(AdminDashboardActivity.this, "Restaurant declined and all data deleted", Toast.LENGTH_SHORT).show();
                            loadPendingRestaurants();
                        });
                    } else {
                        onDeclineConfirmed.accept(restaurantId);
                        Toast.makeText(AdminDashboardActivity.this, "Restaurant declined and all data deleted", Toast.LENGTH_SHORT).show();
                        loadPendingRestaurants();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error in final restaurant document check: " + e.getMessage());
                    onDeclineConfirmed.accept(restaurantId);
                    loadPendingRestaurants();
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists() || !"admin".equals(documentSnapshot.getString("role"))) {
                        Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        setContentView(R.layout.activity_admin_dashboard);
                        recyclerView = findViewById(R.id.recyclerViewRestaurants);
                        recyclerView.setLayoutManager(new LinearLayoutManager(this));
                        restaurantList = new ArrayList<>();
                        adapter = new RestaurantAdapter(restaurantList,
                                restaurantId -> {
                                    db.collection("FoodPlaces").document(restaurantId).get()
                                            .addOnSuccessListener(docSnapshot -> {
                                                if (docSnapshot.exists()) {
                                                    String restaurantName = docSnapshot.getString("name");
                                                    String email = docSnapshot.getString("email");
                                                    // Update approval status
                                                    db.collection("FoodPlaces").document(restaurantId)
                                                            .update("isApproved", true)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(this, "Restaurant approved", Toast.LENGTH_SHORT).show();
                                                                // Show acceptance dialog
                                                                loadPendingRestaurants();
                                                            })
                                                            .addOnFailureListener(e -> Toast.makeText(this, "Error approving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                } else {
                                                    Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Error fetching restaurant: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                },
                                restaurantId -> showDeclineReasonDialog(this, restaurantId, id -> {
                                    // No need to call delete here; declineRestaurant handles it
                                    Log.d("AdminDashboard", "Decline initiated for restaurant: " + id);
                                }));
                        recyclerView.setAdapter(adapter);
                        loadPendingRestaurants();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verifying access", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadPendingRestaurants() {
        db.collection("FoodPlaces")
                .whereEqualTo("isApproved", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    restaurantList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Restaurant restaurant = document.toObject(Restaurant.class);
                        restaurant.setId(document.getId());
                        restaurantList.add(restaurant);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading restaurants: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

        private List<Restaurant> restaurants;
        private Consumer<String> onApprove;
        private Consumer<String> onDecline;

        public RestaurantAdapter(List<Restaurant> restaurants, Consumer<String> onApprove, Consumer<String> onDecline) {
            this.restaurants = restaurants;
            this.onApprove = onApprove;
            this.onDecline = onDecline;
        }

        @NonNull
        @Override
        public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant_approval, parent, false);
            return new RestaurantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
            Restaurant restaurant = restaurants.get(position);
            String restaurantName = restaurant.getName();

            holder.name.setText(restaurantName != null ? restaurantName : "Unknown Restaurant");

            if (restaurantName != null && !restaurantName.isEmpty()) {
                String imagePath = "restaurant_logos/" + restaurantName.replaceAll("[^a-zA-Z0-9]", "_") + "_logo.jpg";
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);
                storageReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Glide.with(holder.itemView.getContext())
                                    .load(uri.toString())
                                    .placeholder(R.drawable.white)
                                    .error(R.drawable.white)
                                    .centerCrop()
                                    .into(holder.image);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RestaurantAdapter", "Failed to load restaurant logo: " + e.getMessage());
                            holder.image.setImageResource(R.drawable.white);
                        });
            } else {
                holder.image.setImageResource(R.drawable.white);
            }
            holder.image.setClipToOutline(true);
            holder.image.setBackgroundResource(R.drawable.rounded_corners);

            holder.approveButton.setOnClickListener(v -> onApprove.accept(restaurant.getId()));
            holder.declineButton.setOnClickListener(v -> onDecline.accept(restaurant.getId()));
            holder.itemView.setOnClickListener(v -> showRestaurantDetailsDialog(holder.itemView.getContext(), restaurant));
        }

        private void showRestaurantDetailsDialog(Context context, Restaurant restaurant) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_restaurant_details, null);

            ImageView restaurantImage = dialogView.findViewById(R.id.restaurant_image);
            TextView restaurantName = dialogView.findViewById(R.id.restaurant_name);
            TextView restaurantAddress = dialogView.findViewById(R.id.restaurant_address);
            TextView restaurantEmail = dialogView.findViewById(R.id.restaurant_email);
            TextView restaurantPhone = dialogView.findViewById(R.id.restaurant_phone);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String restaurantId = restaurant.getName(); // Assuming getName() returns the document ID

            db.collection("FoodPlaces").document(restaurantId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String contactPhone = documentSnapshot.getString("contactPhone");
                            String category = documentSnapshot.getString("category");

                            // Fetch addresses from subcollection
                            db.collection("FoodPlaces").document(restaurantId).collection("Addresses")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        StringBuilder addressesText = new StringBuilder();
                                        for (DocumentSnapshot doc : querySnapshot) {
                                            String address = doc.getString("address");
                                            Boolean isAvailable = doc.getBoolean("isAvailable");
                                            if (address != null && (isAvailable == null || isAvailable)) {
                                                addressesText.append(address).append("\n");
                                            }
                                        }

                                        restaurantName.setText("Name: " + (name != null ? name : "Unknown Restaurant"));
                                        restaurantEmail.setText("Email: " + (email != null ? email : "No email"));
                                        restaurantPhone.setText("Phone: " + (contactPhone != null ? contactPhone : "No phone"));
                                        restaurantAddress.setText("Address(es):\n" + (addressesText.length() > 0 ? addressesText.toString() : "No address"));

                                        // Load image (unchanged)
                                        if (name != null && !name.isEmpty()) {
                                            String imagePath = "restaurant_logos/" + name.replaceAll("[^a-zA-Z0-9]", "_") + "_logo.jpg";
                                            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);
                                            storageReference.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {
                                                        Glide.with(context)
                                                                .load(uri.toString())
                                                                .placeholder(R.drawable.white)
                                                                .centerCrop()
                                                                .error(R.drawable.white)
                                                                .into(restaurantImage);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("RestaurantAdapter", "Failed to load restaurant logo: " + e.getMessage());
                                                        restaurantImage.setImageResource(R.drawable.white);
                                                    });
                                        } else {
                                            restaurantImage.setImageResource(R.drawable.white);
                                        }
                                        restaurantImage.setClipToOutline(true);
                                        restaurantImage.setBackgroundResource(R.drawable.rounded_corners);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RestaurantAdapter", "Failed to fetch addresses: " + e.getMessage());
                                        restaurantAddress.setText("Address(es): Error loading addresses");
                                    });

                            AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setTitle("Restaurant Details")
                                    .setView(dialogView)
                                    .setPositiveButton("OK", (d, which) -> d.dismiss())
                                    .create();
                            dialog.show();
                        } else {
                            Log.e("RestaurantAdapter", "Restaurant document not found: " + restaurantId);
                            restaurantName.setText("Name: Unknown Restaurant");
                            restaurantAddress.setText("Address: No address");
                            restaurantEmail.setText("Email: No email");
                            restaurantPhone.setText("Phone: No phone");
                            restaurantImage.setImageResource(R.drawable.white);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RestaurantAdapter", "Error fetching restaurant data: " + e.getMessage());
                        restaurantName.setText("Name: Error loading");
                        restaurantAddress.setText("Address: Error loading");
                        restaurantEmail.setText("Email: Error loading");
                        restaurantPhone.setText("Phone: Error loading");
                        restaurantImage.setImageResource(R.drawable.white);
                    });
        }


        @Override
        public int getItemCount() {
            return restaurants.size();
        }

        public class RestaurantViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image, approveButton, declineButton;

            public RestaurantViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.RestaurantTitle);
                image = itemView.findViewById(R.id.RestaurantPhoto);
                approveButton = itemView.findViewById(R.id.AcceptRestaurant);
                declineButton = itemView.findViewById(R.id.DeclineRestaurant);
            }
        }
    }

    public static class Restaurant {
        private String id;
        private String name;
        private String address;
        private String email;
        private String phone;
        private String imgUrl;
        private boolean isApproved;

        // No-argument constructor required by Firestore
        public Restaurant() {
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public boolean isApproved() {
            return isApproved;
        }

        public void setApproved(boolean approved) {
            isApproved = approved;
        }
    }
}