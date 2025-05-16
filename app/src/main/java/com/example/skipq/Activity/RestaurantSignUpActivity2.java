package com.example.skipq.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.UploadTask;

public class RestaurantSignUpActivity2 extends AppCompatActivity {

    private EditText restaurantName, restaurantApiLink, addressInput, phoneInput;
    private RecyclerView recyclerViewAddresses;
    private AddressAdapter addressAdapter;
    private List<RestaurantAddress> addressList = new ArrayList<>();
    private Button signUpButton, addAddressButton;
    private TextView backButton;
    private ImageView uploadLogo;
    private AutoCompleteTextView categoryDropdown;
    private String selectedCategory = "Restaurant/Cafe";
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri logoUri;
    private String email, password, uid;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_signup2);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        backButton = findViewById(R.id.backButton);
        restaurantName = findViewById(R.id.restaurant_name);
        restaurantApiLink = findViewById(R.id.restaurant_api_link);
        addressInput = findViewById(R.id.address_input);
        phoneInput = findViewById(R.id.phone_input);
        recyclerViewAddresses = findViewById(R.id.recycler_view_addresses);
        addAddressButton = findViewById(R.id.add_address_button);
        uploadLogo = findViewById(R.id.upload_logo);
        signUpButton = findViewById(R.id.SignUpButton);
        categoryDropdown = findViewById(R.id.dropdown_menu);


        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        uid = intent.getStringExtra("uid");

        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        addressAdapter = new AddressAdapter(addressList);
        recyclerViewAddresses.setAdapter(addressAdapter);
        if (uid == null || uid.isEmpty()) {
            Log.e("RestaurantSignUp", "UID is null or empty");
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.restaurant_categories, android.R.layout.simple_dropdown_item_1line);
        categoryDropdown.setAdapter(adapter);
        categoryDropdown.setText("Restaurant/Cafe", false);
        selectedCategory = "Restaurant/Cafe";

        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = parent.getItemAtPosition(position).toString();
            Log.d("RestaurantSignUp", "Selected category: " + selectedCategory);
        });

        backButton.setOnClickListener(v -> finish());

        uploadLogo.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, PICK_IMAGE_REQUEST);
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
        addAddressButton.setOnClickListener(v -> {
            String addressText = addressInput.getText().toString().trim();
            if (!addressText.isEmpty()) {
                RestaurantAddress address = geocodeAddress(addressText);
                if (address != null) {
                    addressList.add(address);
                    addressAdapter.notifyDataSetChanged();
                    addressInput.setText(""); // Clear input after adding
                    Toast.makeText(this, "Address added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Could not find coordinates for this address", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            }
        });
        signUpButton.setOnClickListener(v -> {
            String name = restaurantName.getText().toString().trim();
            String apiLink = restaurantApiLink.getText().toString().trim();

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Creating...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            if (validateInput(name, apiLink)) {
                if (logoUri != null) {
                    uploadLogoAndSaveData(name, apiLink, progressDialog);
                } else {
                    saveRestaurantWithMenu(name, apiLink, null, progressDialog);
                }
            } else {
                progressDialog.dismiss();
            }
        });
        }
    @Override
    public void onBackPressed() {

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            logoUri = data.getData();
            Toast.makeText(this, "Logo selected successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private RestaurantAddress geocodeAddress(String addressText) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new RestaurantAddress(addressText, address.getLatitude(), address.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private boolean validateInput(String name, String apiLink) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Restaurant name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (addressList.isEmpty()) {
            Toast.makeText(this, "At least one restaurant address is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        String phone = phoneInput.getText().toString().trim();
        if (phone.isEmpty()) {
            Toast.makeText(this, "Contact phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!apiLink.isEmpty()) {
            if (!Patterns.WEB_URL.matcher(apiLink).matches()) {
                Toast.makeText(this, "Please enter a valid API URL", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }


    private void fetchMenuFromApiAndSave(String restaurantName, String apiLink, Runnable onComplete) {
        db.collection("FoodPlaces").document(restaurantName).collection("Menu")
                .document("DefaultMenu").collection("Items")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d("RestaurantSignUp", "Menu already exists in Firestore, skipping API fetch");
                        onComplete.run();
                        return;
                    }
                    RequestQueue queue = Volley.newRequestQueue(this);
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, apiLink,
                            response -> {
                                Log.d("RestaurantSignUp", "API Response: " + response);
                                try {
                                    JSONArray menuItems = new JSONArray(response);
                                    Map<String, Object> menuData = new HashMap<>();
                                    Map<String, String> imageUrls = new HashMap<>();
                                    int totalItems = menuItems.length();

                                    if (totalItems == 0) {
                                        Log.d("RestaurantSignUp", "No menu items in API response");
                                        onComplete.run();
                                        return;
                                    }

                                    for (int i = 0; i < totalItems; i++) {
                                        JSONObject item = menuItems.getJSONObject(i);
                                        Map<String, Object> itemData = new HashMap<>();
                                        String tempItemName = item.optString("name", null);
                                        if (tempItemName == null || tempItemName.trim().isEmpty()) {
                                            tempItemName = "Item_" + UUID.randomUUID().toString();
                                        }
                                        final String itemName = tempItemName;
                                        itemData.put("Item Name", itemName);
                                        itemData.put("Item Price", String.valueOf(item.optDouble("price", 0.0)));
                                        int prepTime = item.optInt("prepTime", -1);
                                        itemData.put("Prep Time", prepTime == -1 ? 10 : prepTime);
                                        itemData.put("Item Description", item.optString("description", "No description available"));
                                        itemData.put("Available", true);

                                        String imageUrl = item.optString("image", "");
                                        if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                                            String imageId = UUID.randomUUID().toString();
                                            StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantName + "/" + imageId + ".jpg");
                                            try {
                                                URL url = new URL(imageUrl);
                                                new Thread(() -> {
                                                    try {
                                                        InputStream inputStream = url.openStream();
                                                        UploadTask uploadTask = imageRef.putStream(inputStream);
                                                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                                                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                                                synchronized (imageUrls) {
                                                                    imageUrls.put(itemName, uri.toString());
                                                                    if (imageUrls.size() == totalItems) {
                                                                        updateMenuDataWithImages(menuData, imageUrls);
                                                                        saveMenuToFirestore(restaurantName, menuData, onComplete);
                                                                    }
                                                                }
                                                            });
                                                        }).addOnFailureListener(e -> {
                                                            Log.e("RestaurantSignUp", "Failed to upload image for " + itemName + ": " + e.getMessage());
                                                            synchronized (imageUrls) {
                                                                imageUrls.put(itemName, "");
                                                                if (imageUrls.size() == totalItems) {
                                                                    updateMenuDataWithImages(menuData, imageUrls);
                                                                    saveMenuToFirestore(restaurantName, menuData, onComplete);
                                                                }
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        Log.e("RestaurantSignUp", "Error downloading image for " + itemName + ": " + e.getMessage());
                                                        synchronized (imageUrls) {
                                                            imageUrls.put(itemName, "");
                                                            if (imageUrls.size() == totalItems) {
                                                                updateMenuDataWithImages(menuData, imageUrls);
                                                                saveMenuToFirestore(restaurantName, menuData, onComplete);
                                                            }
                                                        }
                                                    }
                                                }).start();
                                            } catch (Exception e) {
                                                Log.e("RestaurantSignUp", "Invalid image URL for " + itemName + ": " + imageUrl);
                                                synchronized (imageUrls) {
                                                    imageUrls.put(itemName, "");
                                                    if (imageUrls.size() == totalItems) {
                                                        updateMenuDataWithImages(menuData, imageUrls);
                                                        saveMenuToFirestore(restaurantName, menuData, onComplete);
                                                    }
                                                }
                                            }
                                        } else {
                                            synchronized (imageUrls) {
                                                imageUrls.put(itemName, "");
                                                if (imageUrls.size() == totalItems) {
                                                    updateMenuDataWithImages(menuData, imageUrls);
                                                    saveMenuToFirestore(restaurantName, menuData, onComplete);
                                                }
                                            }
                                        }

                                        String sanitizedItemName = itemName.replaceAll("[^a-zA-Z0-9]", "_");
                                        menuData.put(sanitizedItemName, itemData);
                                    }
                                } catch (JSONException e) {
                                    Log.e("RestaurantSignUp", "Error parsing menu: " + e.getMessage(), e);
                                    Toast.makeText(this, "Error parsing menu", Toast.LENGTH_SHORT).show();
                                    onComplete.run();
                                }
                            },
                            error -> {
                                Log.e("RestaurantSignUp", "Failed to fetch menu: " + error.getMessage(), error);
                                Toast.makeText(this, "Failed to fetch menu", Toast.LENGTH_SHORT).show();
                                onComplete.run();
                            });
                    queue.add(stringRequest);
                })
                .addOnFailureListener(e -> {
                    Log.e("RestaurantSignUp", "Failed to check Firestore for existing menu: " + e.getMessage(), e);
                    Toast.makeText(this, "Error checking existing menu", Toast.LENGTH_SHORT).show();
                    onComplete.run();
                });
    }
    private void saveMenuToFirestore(String restaurantName, Map<String, Object> menuData, Runnable onComplete) {
        String menuDocName = "DefaultMenu";
        for (Map.Entry<String, Object> entry : menuData.entrySet()) {
            db.collection("FoodPlaces").document(restaurantName).collection("Menu")
                    .document(menuDocName).collection("Items")
                    .document(entry.getKey()).set(entry.getValue())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("RestaurantSignUp", "Menu item saved: " + entry.getKey());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RestaurantSignUp", "Failed to save menu item " + entry.getKey() + ": " + e.getMessage(), e);
                    });
        }
        onComplete.run();
    }
    private void updateMenuDataWithImages(Map<String, Object> menuData, Map<String, String> imageUrls) {
        for (Map.Entry<String, String> entry : imageUrls.entrySet()) {
            String itemName = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_");
            if (menuData.containsKey(itemName)) {
                ((Map<String, Object>) menuData.get(itemName)).put("Item Img", entry.getValue());
            }
        }
    }
    private void uploadLogoAndSaveData(String name, String apiLink, ProgressDialog progressDialog) {
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
        logoRef.putFile(logoUri)
                .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    saveRestaurantWithMenu(name, apiLink, logoUrl, progressDialog);
                }))
                .addOnFailureListener(e -> {
                    Log.e("RestaurantSignUp", "Failed to upload logo: " + e.getMessage(), e);
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to upload logo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveRestaurantWithMenu(name, apiLink, null, progressDialog);
                });
    }
    private void saveRestaurantWithMenu(String name, String apiLink, String logoUrl, ProgressDialog progressDialog) {
        db.collection("System").document("DeletedRestaurants")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> deletedRestaurantNames = (List<String>) documentSnapshot.get("deletedRestaurantNames");
                        if (deletedRestaurantNames != null && deletedRestaurantNames.contains(name)) {
                            Log.d("RestaurantSignUp", "Restaurant " + name + " was previously deleted. Aborting save.");
                            progressDialog.dismiss();
                            Toast.makeText(this, "This restaurant was previously deleted. Please use a different name.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null || !uid.equals(currentUser.getUid())) {
                        Log.e("RestaurantSignUp", "User not authenticated or UID mismatch. Provided UID: " + uid + ", Current User UID: " + (currentUser != null ? currentUser.getUid() : "null"));
                        progressDialog.dismiss();
                        Toast.makeText(this, "Authentication error. Please try again.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    db.collection("FoodPlaces").document(name).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult().exists()) {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "A restaurant with this name already exists.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Map<String, Object> restaurantInfo = new HashMap<>();
                                    restaurantInfo.put("name", name);
                                    restaurantInfo.put("email", email);
                                    restaurantInfo.put("uid", uid);
                                    restaurantInfo.put("role", "restaurant");
                                    restaurantInfo.put("category", selectedCategory);
                                    restaurantInfo.put("contactPhone", phoneInput.getText().toString().trim());
                                    if (!apiLink.isEmpty()) {
                                        restaurantInfo.put("apiLink", apiLink);
                                    }
                                    restaurantInfo.put("isApproved", false);
                                    if (logoUrl != null) {
                                        restaurantInfo.put("logoUrl", logoUrl);
                                    }

                                    Map<String, Object> userInfo = new HashMap<>();
                                    userInfo.put("name", name);
                                    userInfo.put("email", email);
                                    userInfo.put("role", "restaurant");
                                    userInfo.put("restaurantId", name);
                                    userInfo.put("phoneNumber", phoneInput.getText().toString().trim());
                                    if (logoUrl != null) {
                                        userInfo.put("profilePictureUrl", logoUrl);
                                    }

                                    // Increment pending tasks to include device token saving
                                    AtomicInteger pendingTasks = new AtomicInteger(addressList.size() + 3); // +3 for user, restaurant, and device token
                                    List<Exception> errors = new ArrayList<>();

                                    // Save user document
                                    db.collection("users").document(uid)
                                            .set(userInfo, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("RestaurantSignUp", "User document created for UID: " + uid);
                                                if (pendingTasks.decrementAndGet() == 0 && errors.isEmpty()) {
                                                    setupApprovalListener(name, apiLink);
                                                    progressDialog.dismiss();
                                                    proceedToPendingActivity(name);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                errors.add(e);
                                                Log.e("RestaurantSignUp", "Failed to save user document: " + e.getMessage(), e);
                                                if (pendingTasks.decrementAndGet() == 0) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(this, "Failed to save user data: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                    // Save restaurant document
                                    db.collection("FoodPlaces").document(name)
                                            .set(restaurantInfo)
                                            .addOnSuccessListener(aVoid -> {
                                                if (pendingTasks.decrementAndGet() == 0 && errors.isEmpty()) {
                                                    setupApprovalListener(name, apiLink);
                                                    progressDialog.dismiss();
                                                    proceedToPendingActivity(name);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                errors.add(e);
                                                Log.e("RestaurantSignUp", "Failed to save restaurant: " + e.getMessage(), e);
                                                if (pendingTasks.decrementAndGet() == 0) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(this, "Failed to save restaurant: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                    // Save device token
                                    FirebaseMessaging.getInstance().getToken()
                                            .addOnCompleteListener(tokenTask -> {
                                                if (tokenTask.isSuccessful()) {
                                                    String deviceToken = tokenTask.getResult();
                                                    Map<String, Object> tokenData = new HashMap<>();
                                                    tokenData.put("deviceToken", deviceToken);
                                                    tokenData.put("uid", uid);

                                                    // Save to user's deviceTokens collection
                                                    db.collection("users").document(uid)
                                                            .collection("deviceTokens").document(deviceToken)
                                                            .set(tokenData)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Log.d("RestaurantSignUp", "Device token saved for UID: " + uid);

                                                                // ALSO save to the restaurant document
                                                                db.collection("FoodPlaces").document(name)
                                                                        .update("deviceToken", deviceToken)
                                                                        .addOnSuccessListener(aVoid2 -> {
                                                                            Log.d("RestaurantSignUp", "Device token saved to restaurant document");
                                                                            if (pendingTasks.decrementAndGet() == 0 && errors.isEmpty()) {
                                                                                setupApprovalListener(name, apiLink);
                                                                                progressDialog.dismiss();
                                                                                proceedToPendingActivity(name);
                                                                            }
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            errors.add(e);
                                                                            Log.e("RestaurantSignUp", "Failed to save device token to restaurant: " + e.getMessage(), e);
                                                                            if (pendingTasks.decrementAndGet() == 0) {
                                                                                progressDialog.dismiss();
                                                                                Toast.makeText(this, "Failed to save device token: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                errors.add(e);
                                                                Log.e("RestaurantSignUp", "Failed to save device token: " + e.getMessage(), e);
                                                                if (pendingTasks.decrementAndGet() == 0) {
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(this, "Failed to save device token: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    errors.add(tokenTask.getException());
                                                    Log.e("RestaurantSignUp", "Failed to retrieve device token: " + tokenTask.getException().getMessage());
                                                    if (pendingTasks.decrementAndGet() == 0) {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(this, "Failed to retrieve device token: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                    // Save addresses
                                    for (RestaurantAddress address : addressList) {
                                        Map<String, Object> addressData = new HashMap<>();
                                        addressData.put("address", address.getAddress());
                                        addressData.put("latitude", address.getLatitude());
                                        addressData.put("longitude", address.getLongitude());
                                        addressData.put("isAvailable", true);
                                        db.collection("FoodPlaces").document(name)
                                                .collection("Addresses").document(UUID.randomUUID().toString())
                                                .set(addressData)
                                                .addOnSuccessListener(aVoid -> {
                                                    if (pendingTasks.decrementAndGet() == 0 && errors.isEmpty()) {
                                                        setupApprovalListener(name, apiLink);
                                                        progressDialog.dismiss();
                                                        proceedToPendingActivity(name);
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    errors.add(e);
                                                    Log.e("RestaurantSignUp", "Failed to save address: " + e.getMessage(), e);
                                                    if (pendingTasks.decrementAndGet() == 0) {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(this, "Failed to save address: " + errors.get(0).getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("RestaurantSignUp", "Failed to check deleted restaurants: " + e.getMessage(), e);
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error checking deleted restaurants, cannot proceed", Toast.LENGTH_SHORT).show();
                });
    }
    private void setupApprovalListener(String restaurantName, String apiLink) {
        DocumentReference restaurantRef = db.collection("FoodPlaces").document(restaurantName);
        restaurantRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("RestaurantSignUp", "Listen failed: " + e.getMessage(), e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Boolean isApproved = snapshot.getBoolean("isApproved");
                if (isApproved != null && isApproved) {
                    Log.d("RestaurantSignUp", "Restaurant " + restaurantName + " approved");
                    // Remove the listener to prevent multiple triggers
                    restaurantRef.addSnapshotListener((s, ex) -> {}).remove();
                    if (apiLink != null && !apiLink.isEmpty()) {
                        // Fetch menu if apiLink is provided
                        fetchMenuFromApiAndSave(restaurantName, apiLink, () -> {
                            Log.d("RestaurantSignUp", "Menu fetch and save completed for " + restaurantName);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Menu loaded for " + restaurantName, Toast.LENGTH_SHORT).show();
                                proceedToDashboard(restaurantName);
                            });
                        });
                    } else {
                        // No apiLink, proceed directly to dashboard
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Restaurant approved: " + restaurantName, Toast.LENGTH_SHORT).show();
                            proceedToDashboard(restaurantName);
                        });
                    }
                }
            }
        });
    }
    private void proceedToDashboard(String restaurantName) {
        Intent intent = new Intent(RestaurantSignUpActivity2.this, HomeActivity.class);
        intent.putExtra("userRole", "restaurant");
        intent.putExtra("restaurantId", restaurantName);
        intent.putExtra("FRAGMENT_TO_LOAD", "RESTAURANT_DASHBOARD");
        startActivity(intent);
        finish();
    }
    private void proceedToPendingActivity(String restaurantName) {
        Intent intent = new Intent(RestaurantSignUpActivity2.this, RestaurantPendingActivity.class);
        intent.putExtra("restaurantId", restaurantName);
        startActivity(intent);
        finish();
    }
    private static class RestaurantAddress {
        private String address;
        private double latitude;
        private double longitude;

        public RestaurantAddress(String address, double latitude, double longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    // RecyclerView Adapter for addresses
    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
        private List<RestaurantAddress> addresses;

        public AddressAdapter(List<RestaurantAddress> addresses) {
            this.addresses = addresses;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RestaurantAddress address = addresses.get(position);
            holder.textView.setText(address.getAddress());
        }

        @Override
        public int getItemCount() {
            return addresses.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}