package com.example.skipq.Fragment;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.skipq.Activity.HomeActivity;
import com.example.skipq.Activity.MainActivity;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    // User-specific views
    private TextView userNameSurname, userEmail, userPhoneNumber, profileTitle;
    private ImageView profilePicture;
    // Restaurant-specific views
    private EditText restaurantName, restaurantContactPhone, restaurantOperatingHours;
    private ImageView restaurantLogo;
    private LinearLayout restaurantProfileSection, addressesContainer;
    private Button saveRestaurantChanges, addAddressButton;
    private Button btnLogout;
    private String userRole, restaurantId;
    private List<Map<String, Object>> addresses;
    private List<String> addressIds;
    private Uri logoUri;
    private ActivityResultLauncher<Intent> logoPickerLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        addresses = new ArrayList<>();
        addressIds = new ArrayList<>();
        logoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                logoUri = result.getData().getData();
                Glide.with(this).load(logoUri).into(restaurantLogo);
                Log.d(TAG, "Logo selected: " + logoUri);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d(TAG, "onCreateView: Inflating layout");

        // Get user role from HomeActivity
        Intent intent = ((HomeActivity) getActivity()).getIntent();
        userRole = intent.getStringExtra("userRole");
        restaurantId = intent.getStringExtra("restaurantId");
        Log.d(TAG, "UserRole: " + userRole + ", RestaurantId: " + restaurantId);

        // Initialize views
        profileTitle = view.findViewById(R.id.profileTitle);
        userNameSurname = view.findViewById(R.id.UserNameSurname);
        userEmail = view.findViewById(R.id.UserEmail);
        userPhoneNumber = view.findViewById(R.id.userPhoneNumber);
        profilePicture = view.findViewById(R.id.profilePicture);
        restaurantProfileSection = view.findViewById(R.id.restaurantProfileSection);
        restaurantName = view.findViewById(R.id.restaurantName);
        restaurantContactPhone = view.findViewById(R.id.restaurantContactPhone);
        restaurantOperatingHours = view.findViewById(R.id.restaurantOperatingHours);
        restaurantLogo = view.findViewById(R.id.restaurantLogo);
        addressesContainer = view.findViewById(R.id.addressesContainer);
        saveRestaurantChanges = view.findViewById(R.id.saveRestaurantChanges);
        addAddressButton = view.findViewById(R.id.addAddressButton);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Setup UI based on role
        if ("restaurant".equals(userRole)) {
            setupRestaurantProfile(view);
        } else {
            setupUserProfile(view);
        }

        return view;
    }

    private void setupUserProfile(View view) {
        Log.d(TAG, "Setting up user profile");
        profileTitle.setText("Your Profile");
        profileTitle.setVisibility(View.VISIBLE);
        userNameSurname.setVisibility(View.VISIBLE);
        userEmail.setVisibility(View.VISIBLE);
        userPhoneNumber.setVisibility(View.VISIBLE);
        profilePicture.setVisibility(View.VISIBLE);
        view.findViewById(R.id.userProfileSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.aboutMeSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.emailSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.phoneSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.paymentSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.settingsSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.languageSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.passwordSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.deleteAccountSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider1).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider2).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider3).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider4).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider5).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider6).setVisibility(View.VISIBLE);
        restaurantProfileSection.setVisibility(View.GONE);

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void setupRestaurantProfile(View view) {
        Log.d(TAG, "Setting up restaurant profile");
        profileTitle.setText("Restaurant Profile");
        profileTitle.setVisibility(View.VISIBLE);
        restaurantProfileSection.setVisibility(View.VISIBLE);
        restaurantName.setVisibility(View.VISIBLE);
        restaurantContactPhone.setVisibility(View.VISIBLE);
        restaurantOperatingHours.setVisibility(View.VISIBLE);
        restaurantLogo.setVisibility(View.VISIBLE);
        addressesContainer.setVisibility(View.VISIBLE);
        saveRestaurantChanges.setVisibility(View.VISIBLE);
        addAddressButton.setVisibility(View.VISIBLE);
        userNameSurname.setVisibility(View.GONE);
        userEmail.setVisibility(View.GONE);
        userPhoneNumber.setVisibility(View.GONE);
        profilePicture.setVisibility(View.GONE);
        view.findViewById(R.id.userProfileSection).setVisibility(View.GONE);
        view.findViewById(R.id.aboutMeSection).setVisibility(View.GONE);
        view.findViewById(R.id.emailSection).setVisibility(View.GONE);
        view.findViewById(R.id.phoneSection).setVisibility(View.GONE);
        view.findViewById(R.id.paymentSection).setVisibility(View.GONE);
        view.findViewById(R.id.settingsSection).setVisibility(View.GONE);
        view.findViewById(R.id.languageSection).setVisibility(View.GONE);
        view.findViewById(R.id.passwordSection).setVisibility(View.GONE);
        view.findViewById(R.id.deleteAccountSection).setVisibility(View.GONE);
        view.findViewById(R.id.divider1).setVisibility(View.GONE);
        view.findViewById(R.id.divider2).setVisibility(View.GONE);
        view.findViewById(R.id.divider3).setVisibility(View.GONE);
        view.findViewById(R.id.divider4).setVisibility(View.GONE);
        view.findViewById(R.id.divider5).setVisibility(View.GONE);
        view.findViewById(R.id.divider6).setVisibility(View.GONE);

        restaurantLogo.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            logoPickerLauncher.launch(pickIntent);
        });

        loadRestaurantData();

        addAddressButton.setOnClickListener(v -> {
            Log.d(TAG, "Add address button clicked");
            addNewAddressField();
        });

        saveRestaurantChanges.setOnClickListener(v -> {
            Log.d(TAG, "Save changes button clicked");
            saveRestaurantChanges();
        });

        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phoneNumber");

                            userNameSurname.setText(name != null ? name : "Name Surname");
                            userEmail.setText(email != null ? email : "Gmail@gmail.com");
                            userPhoneNumber.setText(phone != null ? phone : "+123-456-7890");

                            Log.d(TAG, "User data loaded: name=" + name + ", email=" + email);
                        } else {
                            Log.w(TAG, "User document does not exist");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user data", e);
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "No user logged in");
        }
    }

    private void loadRestaurantData() {
        if (restaurantId != null) {
            db.collection("FoodPlaces").document(restaurantId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String contactPhone = documentSnapshot.getString("contactPhone");
                            String operatingHours = documentSnapshot.getString("operatingHours");
                            String logoUrl = documentSnapshot.getString("logoUrl");

                            restaurantName.setText(name != null ? name : "");
                            restaurantContactPhone.setText(contactPhone != null ? contactPhone : "");
                            restaurantOperatingHours.setText(operatingHours != null ? operatingHours : "");
                            if (logoUrl != null) {
                                Glide.with(this).load(logoUrl).into(restaurantLogo);
                            }

                            db.collection("FoodPlaces").document(restaurantId).collection("Addresses").get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        addresses.clear();
                                        addressIds.clear();
                                        addressesContainer.removeAllViews();
                                        if (!querySnapshot.isEmpty()) {
                                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                Map<String, Object> addr = doc.getData();
                                                String addressText = (String) addr.get("address");
                                                Double latitude = addr.get("latitude") instanceof Number ? ((Number) addr.get("latitude")).doubleValue() : 0.0;
                                                Double longitude = addr.get("longitude") instanceof Number ? ((Number) addr.get("longitude")).doubleValue() : 0.0;
                                                Boolean isAvailable = addr.get("isAvailable") instanceof Boolean ? (Boolean) addr.get("isAvailable") : true;
                                                addAddressField(addressText, latitude, longitude, isAvailable);
                                                addresses.add(addr);
                                                addressIds.add(doc.getId());
                                            }
                                        } else {
                                            addNewAddressField();
                                        }
                                        Log.d(TAG, "Restaurant data loaded: name=" + name + ", addresses=" + addresses.size());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading addresses", e);
                                        Toast.makeText(getContext(), "Error loading addresses", Toast.LENGTH_SHORT).show();
                                        addNewAddressField();
                                    });
                        } else {
                            Log.w(TAG, "Restaurant document does not exist");
                            addNewAddressField();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading restaurant data", e);
                        Toast.makeText(getContext(), "Error loading restaurant data", Toast.LENGTH_SHORT).show();
                        addNewAddressField();
                    });
        } else {
            Log.w(TAG, "Restaurant ID is null");
            addNewAddressField();
        }
    }

    private void addNewAddressField() {
        Log.d(TAG, "Adding new address field");
        addAddressField("", 0.0, 0.0, true);
        Map<String, Object> newAddress = new HashMap<>();
        newAddress.put("address", "");
        newAddress.put("latitude", 0.0);
        newAddress.put("longitude", 0.0);
        newAddress.put("isAvailable", true);
        addresses.add(newAddress);
        addressIds.add(null);
    }

    private void addAddressField(String addressText, double latitude, double longitude, boolean isAvailable) {
        LinearLayout addressLayout = new LinearLayout(getContext());
        addressLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        addressLayout.setOrientation(LinearLayout.VERTICAL);
        addressLayout.setPadding(0, 8, 0, 8);

        LinearLayout inputRow = new LinearLayout(getContext());
        inputRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        inputRow.setOrientation(LinearLayout.HORIZONTAL);

        EditText addressInput = new EditText(getContext());
        addressInput.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));
        addressInput.setText(addressText);
        addressInput.setHint("Address (e.g., 1 Northern Ave, Yerevan)");
        addressInput.setTextColor(getResources().getColor(android.R.color.white));
        addressInput.setHintTextColor(0xB0FFFFFF);
        addressInput.setBackgroundTintList(getResources().getColorStateList(android.R.color.white));

        TextView coordinatesText = new TextView(getContext());
        coordinatesText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));
        coordinatesText.setText(String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitude, longitude));
        coordinatesText.setTextColor(getResources().getColor(android.R.color.white));
        coordinatesText.setPadding(8, 0, 8, 0);

        inputRow.addView(addressInput);
        inputRow.addView(coordinatesText);

        LinearLayout controlRow = new LinearLayout(getContext());
        controlRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setGravity(android.view.Gravity.END);

        CheckBox availabilityToggle = new CheckBox(getContext());
        availabilityToggle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        availabilityToggle.setText("Available");
        availabilityToggle.setTextColor(getResources().getColor(android.R.color.white));
        availabilityToggle.setChecked(isAvailable);
        availabilityToggle.setPadding(8, 0, 8, 0);

        Button deleteButton = new Button(getContext());
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        deleteButton.setText("Delete");
        deleteButton.setTextColor(getResources().getColor(android.R.color.white));
        deleteButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
        deleteButton.setPadding(8, 0, 8, 0);
        deleteButton.setTextSize(12);
        deleteButton.setOnClickListener(v -> {
            Log.d(TAG, "Deleting address field: " + addressText);
            int index = addressesContainer.indexOfChild(addressLayout);
            if (index >= 0 && index < addresses.size()) {
                addressesContainer.removeView(addressLayout);
                addresses.remove(index);
                addressIds.remove(index);
            }
        });

        controlRow.addView(availabilityToggle);
        controlRow.addView(deleteButton);

        addressLayout.addView(inputRow);
        addressLayout.addView(controlRow);
        addressesContainer.addView(addressLayout);

        addressInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String newAddress = s.toString().trim();
                int index = addressesContainer.indexOfChild(addressLayout);
                if (index >= 0 && index < addresses.size()) {
                    Map<String, Object> addr = addresses.get(index);
                    addr.put("address", newAddress);
                    if (!newAddress.isEmpty()) {
                        Map<String, Object> coords = geocodeAddress(newAddress);
                        if (coords != null) {
                            double newLat = (Double) coords.get("latitude");
                            double newLon = (Double) coords.get("longitude");
                            addr.put("latitude", newLat);
                            addr.put("longitude", newLon);
                            coordinatesText.setText(String.format(Locale.US, "Lat: %.6f, Lon: %.6f", newLat, newLon));
                        } else {
                            addr.put("latitude", 0.0);
                            addr.put("longitude", 0.0);
                            coordinatesText.setText("Lat: 0.0, Lon: 0.0");
                        }
                    } else {
                        addr.put("latitude", 0.0);
                        addr.put("longitude", 0.0);
                        coordinatesText.setText("Lat: 0.0, Lon: 0.0");
                    }
                }
            }
        });

        Log.d(TAG, "Address field added: text=" + addressText + ", lat=" + latitude + ", lon=" + longitude + ", available=" + isAvailable);
    }

    private Map<String, Object> geocodeAddress(String addressText) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(addressText, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                Map<String, Object> coords = new HashMap<>();
                coords.put("latitude", address.getLatitude());
                coords.put("longitude", address.getLongitude());
                return coords;
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding failed for address: " + addressText, e);
        }
        return null;
    }

    private void saveRestaurantChanges() {
        if (restaurantId == null) {
            Log.e(TAG, "Restaurant ID is null");
            Toast.makeText(getContext(), "Restaurant ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = restaurantName.getText().toString().trim();
        String contactPhone = restaurantContactPhone.getText().toString().trim();
        String operatingHours = restaurantOperatingHours.getText().toString().trim();

        if (name.isEmpty()) {
            Log.w(TAG, "Restaurant name is empty");
            Toast.makeText(getContext(), "Restaurant name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> updatedAddresses = new ArrayList<>();
        for (int i = 0; i < addressesContainer.getChildCount(); i++) {
            LinearLayout addressLayout = (LinearLayout) addressesContainer.getChildAt(i);
            LinearLayout inputRow = (LinearLayout) addressLayout.getChildAt(0);
            LinearLayout controlRow = (LinearLayout) addressLayout.getChildAt(1);
            EditText addressInput = (EditText) inputRow.getChildAt(0);
            CheckBox availabilityToggle = (CheckBox) controlRow.getChildAt(0);

            String addressText = addressInput.getText().toString().trim();
            if (!addressText.isEmpty()) {
                Map<String, Object> addr = addresses.get(i);
                Map<String, Object> addressData = new HashMap<>();
                addressData.put("address", addressText);
                addressData.put("latitude", addr.get("latitude") != null ? addr.get("latitude") : 0.0);
                addressData.put("longitude", addr.get("longitude") != null ? addr.get("longitude") : 0.0);
                addressData.put("isAvailable", availabilityToggle.isChecked());
                updatedAddresses.add(addressData);
            }
        }

        if (updatedAddresses.isEmpty()) {
            Log.w(TAG, "No valid addresses provided");
            Toast.makeText(getContext(), "At least one address is required", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference restaurantRef = db.collection("FoodPlaces").document(restaurantId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("contactPhone", contactPhone);
        updates.put("operatingHours", operatingHours);
        updates.put("rememberMe", true);
        updates.put("role", "restaurant");
        updates.put("uid", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "");

        if (logoUri != null) {
            StorageReference logoRef = storage.getReference().child("restaurant_logos/" + restaurantId + "_logo.jpg");
            logoRef.putFile(logoUri)
                    .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updates.put("logoUrl", uri.toString());
                        updateFirestore(restaurantRef, updates, updatedAddresses);
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload logo", e);
                        Toast.makeText(getContext(), "Failed to upload logo", Toast.LENGTH_SHORT).show();
                        updateFirestore(restaurantRef, updates, updatedAddresses);
                    });
        } else {
            updateFirestore(restaurantRef, updates, updatedAddresses);
        }
    }

    private void updateFirestore(DocumentReference restaurantRef, Map<String, Object> updates, List<Map<String, Object>> updatedAddresses) {
        restaurantRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    com.google.firebase.firestore.CollectionReference addressCollection = restaurantRef.collection("Addresses");
                    addressCollection.get().addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                        for (Map<String, Object> addr : updatedAddresses) {
                            addressCollection.add(addr);
                        }
                        Log.d(TAG, "Restaurant data updated successfully");
                        Toast.makeText(getContext(), "Changes saved successfully", Toast.LENGTH_SHORT).show();
                        loadRestaurantData();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating addresses", e);
                        Toast.makeText(getContext(), "Error saving addresses", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving restaurant changes", e);
                    Toast.makeText(getContext(), "Error saving changes", Toast.LENGTH_SHORT).show();
                });
    }
}