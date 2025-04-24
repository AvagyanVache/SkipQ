package com.example.skipq.Fragment;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.skipq.Activity.ChangePasswordActivity;
import com.example.skipq.Activity.DeleteAccountActivity;
import com.example.skipq.Activity.HomeActivity;
import com.example.skipq.Activity.MainActivity;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;

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
    private LinearLayout restaurantProfileSection, addressesContainer, addressesSection;
    private Button saveRestaurantChanges, addAddressButton;
    private Button btnLogout;
    private String userRole, restaurantId;
    private List<Map<String, Object>> addresses;
    private List<String> addressIds;
    private Uri logoUri;
    private Uri profilePictureUri;
    private ActivityResultLauncher<Intent> logoPickerLauncher;
    private ActivityResultLauncher<Intent> profilePicturePickerLauncher;
    private String originalName = "";
    private String originalContactPhone = "";
    private String originalOperatingHours = "";
    private String originalLogoUrl = "";
    private List<Map<String, Object>> originalAddresses = new ArrayList<>();

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

        // Initialize logo picker
        logoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                logoUri = result.getData().getData();
                Glide.with(this).load(logoUri).into(restaurantLogo);
                Log.d(TAG, "Logo selected: " + logoUri);
                checkForChanges();
            }
        });

        profilePicturePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                profilePictureUri = result.getData().getData();
                Glide.with(this)
                        .load(profilePictureUri)
                        .transform(new CircleCrop())
                        .into(profilePicture);
                Log.d(TAG, "Profile picture selected: " + profilePictureUri);
                uploadProfilePicture();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d(TAG, "onCreateView: Inflating layout");

        // Get user role from HomeActivity
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached to activity");
            return view;
        }
        Intent intent = ((HomeActivity) requireActivity()).getIntent();
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
        addressesSection = view.findViewById(R.id.addressesSection);
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

        profilePicture.setOnClickListener(v -> {
            Log.d(TAG, "Profile picture clicked");
            String permission;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission = Manifest.permission.READ_MEDIA_IMAGES;
            } else {
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                    Toast.makeText(requireContext(), "Permission is needed to access your photos", Toast.LENGTH_LONG).show();
                }
                Log.d(TAG, "Requesting permission: " + permission);
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{permission}, 100);
            } else {
                Log.d(TAG, "Launching gallery picker");
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getContentIntent.setType("image/*");
                Intent chooserIntent = Intent.createChooser(pickIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{getContentIntent});
                if (chooserIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    profilePicturePickerLauncher.launch(chooserIntent);
                } else {
                    Log.e(TAG, "No app available to handle image picking");
                    Toast.makeText(getContext(), "No gallery app available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup navigation for password and delete account
        ImageView changePassword = view.findViewById(R.id.changePassword);
        changePassword.setOnClickListener(v -> {
            Log.d(TAG, "Change password clicked");
            Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        ImageView deleteAccount = view.findViewById(R.id.deleteAccount);
        deleteAccount.setOnClickListener(v -> {
            Log.d(TAG, "Delete account clicked");
            Intent intent = new Intent(requireActivity(), DeleteAccountActivity.class);
            startActivity(intent);
        });

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            profilePicturePickerLauncher.launch(pickIntent);
        } else if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            logoPickerLauncher.launch(pickIntent);
        } else {
            Toast.makeText(getContext(), "Permission denied to access images", Toast.LENGTH_SHORT).show();
        }
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
        addressesSection.setVisibility(View.VISIBLE);
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

        // Setup dropdown for addresses
        addressesSection.setOnClickListener(v -> {
            boolean isVisible = addressesContainer.getVisibility() == View.VISIBLE;
            addressesContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            addAddressButton.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            Log.d(TAG, "Addresses dropdown toggled: " + (isVisible ? "collapsed" : "expanded"));
        });

        // Setup text watchers for change detection
        restaurantName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
        });

        restaurantContactPhone.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
        });

        restaurantOperatingHours.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
        });

        restaurantLogo.setOnClickListener(v -> {
            Log.d(TAG, "Restaurant logo clicked");
            String permission;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission = Manifest.permission.READ_MEDIA_IMAGES;
            } else {
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                    Toast.makeText(requireContext(), "Permission is needed to access your photos", Toast.LENGTH_LONG).show();
                }
                Log.d(TAG, "Requesting permission: " + permission);
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{permission}, 101);
            } else {
                Log.d(TAG, "Launching gallery picker for logo");
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                logoPickerLauncher.launch(pickIntent);
            }
        });

        loadRestaurantData();

        addAddressButton.setOnClickListener(v -> {
            Log.d(TAG, "Add address button clicked");
            addNewAddressField();
            // Ensure dropdown is expanded
            addressesContainer.setVisibility(View.VISIBLE);
            addAddressButton.setVisibility(View.VISIBLE);
        });

        saveRestaurantChanges.setOnClickListener(v -> {
            Log.d(TAG, "Save changes button clicked");
            saveRestaurantChanges();
        });

        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && isAdded()) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phoneNumber");
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                            userNameSurname.setText(name != null ? name : "Name Surname");
                            userEmail.setText(email != null ? email : "Gmail@gmail.com");
                            userPhoneNumber.setText(phone != null ? phone : "+123-456-7890");
                            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .transform(new CircleCrop())
                                        .into(profilePicture);
                            } else {
                                Glide.with(this)
                                        .load(R.drawable.profile_picture)
                                        .transform(new CircleCrop())
                                        .into(profilePicture);
                            }
                            Log.d(TAG, "User data loaded: name=" + name + ", email=" + email + ", profilePictureUrl=" + profilePictureUrl);
                        } else {
                            Log.w(TAG, "User document does not exist");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Log.e(TAG, "Error loading user data", e);
                            Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.w(TAG, "No user logged in or fragment not attached");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d(TAG, "Network available: " + isConnected);
        return isConnected;
    }

    private void uploadProfilePicture() {
        FirebaseUser user = mAuth.getCurrentUser();
        Log.d(TAG, "Upload attempt: user=" + (user != null ? user.getUid() : "null") + ", uri=" + profilePictureUri);
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No internet connection");
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user != null && profilePictureUri != null && isAdded()) {
            StorageReference profilePicRef = storage.getReference().child("profile_pictures/" + user.getUid() + ".jpg");
            profilePicRef.putFile(profilePictureUri)
                    .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profilePictureUrl = uri.toString();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profilePictureUrl", profilePictureUrl);
                        updates.put("profileImage", FieldValue.delete()); // Remove old Base64 field
                        db.collection("users").document(user.getUid())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Profile picture URL updated: " + profilePictureUrl);
                                    Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update profile picture URL", e);
                                    Toast.makeText(getContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                                });
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload profile picture", e);
                        Toast.makeText(getContext(), "Failed to upload profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Log.w(TAG, "Cannot upload profile picture: user null or URI null");
            Toast.makeText(getContext(), "Please log in to upload profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRestaurantData() {
        if (restaurantId != null && isAdded()) {
            Log.d(TAG, "Loading restaurant data for ID: " + restaurantId);
            db.collection("FoodPlaces").document(restaurantId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("name");
                            String contactPhone = documentSnapshot.getString("contactPhone");
                            String operatingHours = documentSnapshot.getString("operatingHours");

                            // Store original values
                            originalName = name != null ? name : "";
                            originalContactPhone = contactPhone != null ? contactPhone : "";
                            originalOperatingHours = operatingHours != null ? operatingHours : "";
                            originalLogoUrl = documentSnapshot.getString("logoUrl") != null ? documentSnapshot.getString("logoUrl") : "";

                            restaurantName.setText(originalName);
                            restaurantContactPhone.setText(originalContactPhone);
                            restaurantOperatingHours.setText(originalOperatingHours);

                            // Load logo from Firestore logoUrl
                            Log.d(TAG, "Fetched logoUrl: " + originalLogoUrl);
                            Log.d(TAG, "Network available: " + isNetworkAvailable());
                            Log.d(TAG, "restaurantLogo visibility: " + (restaurantLogo.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE/INVISIBLE"));
                            if (!isNetworkAvailable()) {
                                Log.w(TAG, "No internet connection, loading default logo");
                                Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                                Glide.with(ProfileFragment.this)
                                        .load(R.drawable.white) // Replace with your default image resource
                                        .apply(new RequestOptions()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true))
                                        .into(restaurantLogo);
                            } else if (originalLogoUrl != null && !originalLogoUrl.isEmpty()) {
                                Log.d(TAG, "Loading logo from Firestore logoUrl: " + originalLogoUrl);
                                Glide.with(ProfileFragment.this)
                                        .load(originalLogoUrl)
                                        .apply(new RequestOptions()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true))
                                        .error(R.drawable.white) // Fallback if URL fails to load
                                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                                Log.e(TAG, "Glide failed to load logo: " + originalLogoUrl, e);
                                                Toast.makeText(getContext(), "Failed to load logo", Toast.LENGTH_SHORT).show();
                                                return false; // Let Glide handle the error drawable
                                            }

                                            @Override
                                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                                Log.d(TAG, "Glide successfully loaded logo: " + originalLogoUrl);
                                                return false;
                                            }
                                        })
                                        .into(restaurantLogo);
                            } else {
                                Log.w(TAG, "logoUrl is null or empty, loading default logo");
                                Toast.makeText(getContext(), "No logo available", Toast.LENGTH_SHORT).show();
                                Glide.with(ProfileFragment.this)
                                        .load(R.drawable.white) // Replace with your default image resource
                                        .apply(new RequestOptions()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true))
                                        .into(restaurantLogo);
                            }

                            // Load addresses
                            db.collection("FoodPlaces").document(restaurantId).collection("Addresses").get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (isAdded()) {
                                            addresses.clear();
                                            addressIds.clear();
                                            originalAddresses.clear();
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
                                                    originalAddresses.add(new HashMap<>(addr));
                                                    addressIds.add(doc.getId());
                                                }
                                            } else {
                                                addNewAddressField();
                                            }
                                            checkForChanges();
                                            Log.d(TAG, "Restaurant data loaded: name=" + name + ", addresses=" + addresses.size());
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (isAdded()) {
                                            Log.e(TAG, "Error loading addresses", e);
                                            Toast.makeText(getContext(), "Error loading addresses", Toast.LENGTH_SHORT).show();
                                            addNewAddressField();
                                        }
                                    });
                        } else {
                            if (isAdded()) {
                                Log.w(TAG, "Restaurant document does not exist");
                                addNewAddressField();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Log.e(TAG, "Error loading restaurant data", e);
                            Toast.makeText(getContext(), "Error loading restaurant data", Toast.LENGTH_SHORT).show();
                            addNewAddressField();
                        }
                    });
        } else {
            if (isAdded()) {
                Log.w(TAG, "Restaurant ID is null or fragment not attached");
                addNewAddressField();
            }
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
        checkForChanges();
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
        availabilityToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int index = addressesContainer.indexOfChild(addressLayout);
            if (index >= 0 && index < addresses.size()) {
                addresses.get(index).put("isAvailable", isChecked);
                checkForChanges();
            }
        });

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
                checkForChanges();
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
                    checkForChanges();
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

        FirebaseUser user = mAuth.getCurrentUser();
        DocumentReference restaurantRef = db.collection("FoodPlaces").document(restaurantId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("contactPhone", contactPhone);
        updates.put("operatingHours", operatingHours);
        updates.put("rememberMe", true);
        updates.put("role", "restaurant");
        updates.put("uid", user != null ? user.getUid() : "");

        if (logoUri != null) {
            StorageReference logoRef = storage.getReference().child("restaurant_logos/" + restaurantId + "_logo.jpg");
            logoRef.putFile(logoUri)
                    .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String logoUrl = uri.toString();
                        Log.d(TAG, "Logo uploaded successfully, URL: " + logoUrl);
                        updates.put("logoUrl", logoUrl);
                        // Update users collection with logoUrl as profilePictureUrl
                        if (user != null) {
                            Map<String, Object> userUpdates = new HashMap<>();
                            userUpdates.put("profilePictureUrl", logoUrl);
                            db.collection("users").document(user.getUid())
                                    .update(userUpdates)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile picture URL updated: " + logoUrl))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update user profile picture URL", e));
                        }
                        // Update Firestore and refresh logo
                        updateFirestore(restaurantRef, updates, updatedAddresses);
                        if (isAdded()) {
                            Log.d(TAG, "Refreshing logo in ProfileFragment with URL: " + logoUrl);
                            originalLogoUrl = logoUrl; // Update originalLogoUrl
                            Glide.with(ProfileFragment.this)
                                    .load(logoUrl)
                                    .apply(new RequestOptions()
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true))
                                    .error(R.drawable.white)
                                    .into(restaurantLogo);
                            logoUri = null; // Clear local logoUri
                        }
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

    private void checkForChanges() {
        boolean hasChanges = false;

        // Check restaurant name
        String currentName = restaurantName.getText().toString().trim();
        if (!currentName.equals(originalName)) {
            hasChanges = true;
        }

        // Check contact phone
        String currentPhone = restaurantContactPhone.getText().toString().trim();
        if (!currentPhone.equals(originalContactPhone)) {
            hasChanges = true;
        }

        // Check operating hours
        String currentHours = restaurantOperatingHours.getText().toString().trim();
        if (!currentHours.equals(originalOperatingHours)) {
            hasChanges = true;
        }

        // Check logo
        if (logoUri != null && !logoUri.toString().equals(originalLogoUrl)) {
            hasChanges = true;
        }

        // Check addresses
        if (addresses.size() != originalAddresses.size()) {
            hasChanges = true;
        } else {
            for (int i = 0; i < addresses.size(); i++) {
                Map<String, Object> currentAddr = addresses.get(i);
                Map<String, Object> originalAddr = originalAddresses.get(i);
                if (!currentAddr.get("address").equals(originalAddr.get("address")) ||
                        !currentAddr.get("latitude").equals(originalAddr.get("latitude")) ||
                        !currentAddr.get("longitude").equals(originalAddr.get("longitude")) ||
                        !currentAddr.get("isAvailable").equals(originalAddr.get("isAvailable"))) {
                    hasChanges = true;
                    break;
                }
            }
        }

        saveRestaurantChanges.setVisibility(hasChanges ? View.VISIBLE : View.GONE);
    }
}