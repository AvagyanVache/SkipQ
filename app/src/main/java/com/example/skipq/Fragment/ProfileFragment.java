package com.example.skipq.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    // User-specific views
    //  private TextView userNameSurname, userEmail, userPhoneNumber, profileTitle;
    private ImageView profilePicture;
    private TextView userNameSurname;
    private TextView profileTitle;
    private TextView restaurantNameDisplay, restaurantPhoneDisplay;
    private ImageView restaurantLogo;
    private CheckBox mondayCheckbox, tuesdayCheckbox, wednesdayCheckbox, thursdayCheckbox, fridayCheckbox, saturdayCheckbox, sundayCheckbox;
    private EditText mondayHours, tuesdayHours, wednesdayHours, thursdayHours, fridayHours, saturdayHours, sundayHours;
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
    private Map<String, String> originalOperatingHours = new HashMap<>();
    private String originalLogoUrl = "";
    private List<Map<String, Object>> originalAddresses = new ArrayList<>();

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        addresses = new ArrayList<>();
        addressIds = new ArrayList<>();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });
        logoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                logoUri = result.getData().getData();
                if (logoUri != null && isAdded()) {
                    Glide.with(this).load(logoUri).into(restaurantLogo);
                    Log.d(TAG, "Logo selected: " + logoUri);

                    // Upload the new logo to Firebase Storage
                    if (!isNetworkAvailable()) {
                        Log.w(TAG, "No internet connection");
                        Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String sanitizedName = restaurantId.replaceAll("[^a-zA-Z0-9]", "_");
                    StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
                    logoRef.putFile(logoUri)
                            .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String logoUrl = uri.toString();
                                Log.d(TAG, "Logo uploaded successfully, URL: " + logoUrl);

                                // Update Firestore with the new logo URL
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("logoUrl", logoUrl);
                                db.collection("FoodPlaces").document(restaurantId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Restaurant logo URL updated in Firestore: " + logoUrl);
                                            Toast.makeText(getContext(), "Restaurant logo updated", Toast.LENGTH_SHORT).show();

                                            // Update users collection with logoUrl as profilePictureUrl
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                Map<String, Object> userUpdates = new HashMap<>();
                                                userUpdates.put("profilePictureUrl", logoUrl);
                                                db.collection("users").document(user.getUid())
                                                        .set(userUpdates, SetOptions.merge())
                                                        .addOnSuccessListener(aVoid2 -> Log.d(TAG, "User profile picture URL updated: " + logoUrl))
                                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update user profile picture URL", e));
                                            }

                                            // Update originalLogoUrl to reflect the new URL
                                            originalLogoUrl = logoUrl;
                                            logoUri = null; // Reset logoUri
                                            checkForChanges(); // Update save button visibility
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to update restaurant logo URL in Firestore", e);
                                            Toast.makeText(getContext(), "Failed to update logo", Toast.LENGTH_SHORT).show();
                                        });
                            }))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to upload logo to Storage", e);
                                Toast.makeText(getContext(), "Failed to upload logo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } else {
                    Log.w(TAG, "Logo URI is null");
                    Toast.makeText(getContext(), "Failed to select logo", Toast.LENGTH_SHORT).show();
                }
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
        profilePicture = view.findViewById(R.id.profilePicture);
        btnLogout = view.findViewById(R.id.btnLogout);
        restaurantProfileSection = view.findViewById(R.id.restaurantProfileSection);
        restaurantLogo = view.findViewById(R.id.restaurantLogo);
        addressesContainer = view.findViewById(R.id.addressesContainer);
        addressesSection = view.findViewById(R.id.addressesSection);
        saveRestaurantChanges = view.findViewById(R.id.saveRestaurantChanges);
        addAddressButton = view.findViewById(R.id.addAddressButton);
        restaurantNameDisplay = view.findViewById(R.id.RestaurantName);
        restaurantPhoneDisplay = view.findViewById(R.id.RestaurantPhone);

        mondayCheckbox = view.findViewById(R.id.mondayCheckbox);
        tuesdayCheckbox = view.findViewById(R.id.tuesdayCheckbox);
        wednesdayCheckbox = view.findViewById(R.id.wednesdayCheckbox);
        thursdayCheckbox = view.findViewById(R.id.thursdayCheckbox);
        fridayCheckbox = view.findViewById(R.id.fridayCheckbox);
        saturdayCheckbox = view.findViewById(R.id.saturdayCheckbox);
        sundayCheckbox = view.findViewById(R.id.sundayCheckbox);
        mondayHours = view.findViewById(R.id.mondayHours);
        tuesdayHours = view.findViewById(R.id.tuesdayHours);
        wednesdayHours = view.findViewById(R.id.wednesdayHours);
        thursdayHours = view.findViewById(R.id.thursdayHours);
        fridayHours = view.findViewById(R.id.fridayHours);
        saturdayHours = view.findViewById(R.id.saturdayHours);
        sundayHours = view.findViewById(R.id.sundayHours);

        scaleUIElements(view);

        if ("restaurant".equals(userRole)) {
            setupRestaurantProfile(view);
        } else {
            setupUserProfile(view);
        }

        // Setup click listeners
        profilePicture.setOnClickListener(v -> {
            Log.d(TAG, "Profile picture clicked");
            checkAndRequestImagePermission();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent1 = new Intent(requireActivity(), MainActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent1);
            requireActivity().finish();
        });

        ImageView settingsRedirect = view.findViewById(R.id.SettingsRedirect);
        settingsRedirect.setOnClickListener(v -> {
            Log.d(TAG, "Settings redirect clicked");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new ProfileSettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        ImageView accountRedirect = view.findViewById(R.id.AccountRedirect);
        accountRedirect.setOnClickListener(v -> {
            Log.d(TAG, "Account redirect clicked");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new ProfileAccountFragment())
                    .addToBackStack(null)
                    .commit();
        });

        ImageView AboutDeveloperRedirect = view.findViewById(R.id.AboutDeveloperRedirect);
        AboutDeveloperRedirect.setOnClickListener(v -> {
            Log.d(TAG, "About Developer clicked");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new AboutDeveloperFragment())
                    .addToBackStack(null)
                    .commit();
        });

        ImageView FAQRedirect = view.findViewById(R.id.FAQ_Redirect);
        FAQRedirect.setOnClickListener(v -> {
            Log.d(TAG, "FAQ Fragment clicked");
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new FAQFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadUserData();
        return view;
    }
    private void scaleUIElements(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = Math.min(screenWidth / (360 * density), 1.5f); // Reference width: 360dp, cap at 1.5x

        // Scale Profile Title
        if (profileTitle != null) {
            profileTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28 * scaleFactor);
            ViewGroup.MarginLayoutParams titleParams = (ViewGroup.MarginLayoutParams) profileTitle.getLayoutParams();
            titleParams.topMargin = (int) (8 * density * scaleFactor);
            profileTitle.setLayoutParams(titleParams);
        }

        // Scale User Profile Section
        if (profilePicture != null) {
            ViewGroup.LayoutParams picParams = profilePicture.getLayoutParams();
            picParams.width = (int) (120 * density * scaleFactor);
            picParams.height = (int) (120 * density * scaleFactor);
            profilePicture.setLayoutParams(picParams);
        }
        if (userNameSurname != null) {
            userNameSurname.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22 * scaleFactor);
            ViewGroup.MarginLayoutParams nameParams = (ViewGroup.MarginLayoutParams) userNameSurname.getLayoutParams();
            nameParams.topMargin = (int) (8 * density * scaleFactor);
            userNameSurname.setLayoutParams(nameParams);
        }

        // Scale Restaurant Profile Section
        if (restaurantLogo != null) {
            ViewGroup.LayoutParams logoParams = restaurantLogo.getLayoutParams();
            logoParams.width = (int) (120 * density * scaleFactor);
            logoParams.height = (int) (120 * density * scaleFactor);
            restaurantLogo.setLayoutParams(logoParams);
        }
        if (restaurantNameDisplay != null) {
            restaurantNameDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }
        if (restaurantPhoneDisplay != null) {
            restaurantPhoneDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }

        // Scale Operating Hours
        CheckBox[] checkboxes = {mondayCheckbox, tuesdayCheckbox, wednesdayCheckbox, thursdayCheckbox, fridayCheckbox, saturdayCheckbox, sundayCheckbox};
        EditText[] hoursFields = {mondayHours, tuesdayHours, wednesdayHours, thursdayHours, fridayHours, saturdayHours, sundayHours};
        for (CheckBox checkbox : checkboxes) {
            if (checkbox != null) {
                checkbox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            }
        }
        for (EditText hoursField : hoursFields) {
            if (hoursField != null) {
                hoursField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) hoursField.getLayoutParams();
                params.leftMargin = (int) (12 * density * scaleFactor);
                hoursField.setLayoutParams(params);
            }
        }

        // Scale Buttons
        if (saveRestaurantChanges != null) {
            saveRestaurantChanges.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            saveRestaurantChanges.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor)
            );
        }
        if (addAddressButton != null) {
            addAddressButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            addAddressButton.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor)
            );
        }
        if (btnLogout != null) {
            btnLogout.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            btnLogout.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor)
            );
        }

        // Scale Menu Section Icons and Text
        int[] iconIds = {R.id.SettingsIcon, R.id.AccountIcon, R.id.AboutDeveloperIcon, R.id.FAQ_Icon, R.id.SettingsRedirect, R.id.AccountRedirect, R.id.AboutDeveloperRedirect, R.id.FAQ_Redirect};
        int[] textIds = {R.id.Settings, R.id.Account, R.id.About_Developer, R.id.FAQ};
        for (int iconId : iconIds) {
            ImageView icon = view.findViewById(iconId);
            if (icon != null) {
                ViewGroup.LayoutParams params = icon.getLayoutParams();
                params.width = (int) ((iconId == R.id.SettingsRedirect || iconId == R.id.AccountRedirect || iconId == R.id.AboutDeveloperRedirect || iconId == R.id.FAQ_Redirect ? 32 : 36) * density * scaleFactor);
                params.height = (int) ((iconId == R.id.SettingsRedirect || iconId == R.id.AccountRedirect || iconId == R.id.AboutDeveloperRedirect || iconId == R.id.FAQ_Redirect ? 32 : 36) * density * scaleFactor);
                icon.setLayoutParams(params);
            }
        }
        for (int textId : textIds) {
            TextView text = view.findViewById(textId);
            if (text != null) {
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
            }
        }
    }


    private void setupUserProfile(View view) {
        Log.d(TAG, "Setting up user profile");
        profileTitle.setText("Your Profile");
        profileTitle.setVisibility(View.VISIBLE);
        userNameSurname.setVisibility(View.VISIBLE);
        profilePicture.setVisibility(View.VISIBLE);
        view.findViewById(R.id.userProfileSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.menuSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.btnLogout).setVisibility(View.VISIBLE);
        restaurantProfileSection.setVisibility(View.GONE);
    }

    private void setupRestaurantProfile(View view) {
        Log.d(TAG, "Setting up restaurant profile");
        profileTitle.setText("Restaurant Profile");
        profileTitle.setVisibility(View.VISIBLE);
        restaurantProfileSection.setVisibility(View.VISIBLE);
        view.findViewById(R.id.RestaurantNameSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.RestaurantPhoneSection).setVisibility(View.VISIBLE);
        restaurantLogo.setVisibility(View.VISIBLE);
        addressesSection.setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider7).setVisibility(View.VISIBLE);
        view.findViewById(R.id.divider8).setVisibility(View.VISIBLE);
        view.findViewById(R.id.operatingHoursSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.addressesSection).setVisibility(View.VISIBLE);
        view.findViewById(R.id.saveRestaurantChanges).setVisibility(View.VISIBLE);
        view.findViewById(R.id.btnLogout).setVisibility(View.VISIBLE);
        userNameSurname.setVisibility(View.GONE);
        profilePicture.setVisibility(View.GONE);
        view.findViewById(R.id.userProfileSection).setVisibility(View.GONE);
        view.findViewById(R.id.menuSection).setVisibility(View.GONE);

        // Setup operating hours checkboxes
        setupOperatingHoursCheckbox(mondayCheckbox, mondayHours);
        setupOperatingHoursCheckbox(tuesdayCheckbox, tuesdayHours);
        setupOperatingHoursCheckbox(wednesdayCheckbox, wednesdayHours);
        setupOperatingHoursCheckbox(thursdayCheckbox, thursdayHours);
        setupOperatingHoursCheckbox(fridayCheckbox, fridayHours);
        setupOperatingHoursCheckbox(saturdayCheckbox, saturdayHours);
        setupOperatingHoursCheckbox(sundayCheckbox, sundayHours);

        // Setup text watchers for operating hours
        setupOperatingHoursTextWatcher(mondayHours);
        setupOperatingHoursTextWatcher(tuesdayHours);
        setupOperatingHoursTextWatcher(wednesdayHours);
        setupOperatingHoursTextWatcher(thursdayHours);
        setupOperatingHoursTextWatcher(fridayHours);
        setupOperatingHoursTextWatcher(saturdayHours);
        setupOperatingHoursTextWatcher(sundayHours);

        LinearLayout operatingHoursSection = view.findViewById(R.id.operatingHoursSection);
        ImageView dropdownOperatingHours = view.findViewById(R.id.dropdownOperatingHours);
        ImageView dropdownAddresses = view.findViewById(R.id.dropdownAddresses);

        for (int i = 1; i < operatingHoursSection.getChildCount(); i++) {
            operatingHoursSection.getChildAt(i).setVisibility(View.GONE);
        }

        // Setup dropdown for operating hours
        dropdownOperatingHours.setOnClickListener(v -> {
            boolean isVisible = operatingHoursSection.getChildAt(1).getVisibility() == View.VISIBLE;
            for (int i = 1; i < operatingHoursSection.getChildCount(); i++) {
                operatingHoursSection.getChildAt(i).setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }
            dropdownOperatingHours.animate().rotation(isVisible ? 0 : 180).setDuration(200).start();
            Log.d(TAG, "Operating hours dropdown toggled: " + (isVisible ? "collapsed" : "expanded"));
        });

        // Setup dropdown for addresses
        dropdownAddresses.setOnClickListener(v -> {
            boolean isVisible = addressesContainer.getVisibility() == View.VISIBLE;
            addressesContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            addAddressButton.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            dropdownAddresses.animate().rotation(isVisible ? 0 : 180).setDuration(200).start();
            Log.d(TAG, "Addresses dropdown toggled: " + (isVisible ? "collapsed" : "expanded"));
        });

        ImageView changeRestaurantNumber = view.findViewById(R.id.changeRestaurantNumber);
        changeRestaurantNumber.setOnClickListener(v -> {
            Log.d(TAG, "Change restaurant phone clicked");
            showChangePhoneDialog();
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

        addAddressButton.setOnClickListener(v -> {
            Log.d(TAG, "Add address button clicked");
            addNewAddressField();
            addressesContainer.setVisibility(View.VISIBLE);
            addAddressButton.setVisibility(View.VISIBLE);
            dropdownAddresses.setRotation(180);
        });

        saveRestaurantChanges.setOnClickListener(v -> {
            Log.d(TAG, "Save changes button clicked");
            saveRestaurantChanges();
        });

        loadRestaurantData();
    }
    private void setupOperatingHoursCheckbox(CheckBox checkBox, EditText hoursField) {
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hoursField.setEnabled(isChecked);
            if (!isChecked) {
                hoursField.setText("");
            }
            checkForChanges();
        });
    }

    private void setupOperatingHoursTextWatcher(EditText hoursField) {
        hoursField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkForChanges();
            }
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

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && isAdded()) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("name");
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                            // Set user name
                            userNameSurname.setText(name != null ? name : "Name Surname");

                            // Load profile picture
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
                            Log.d(TAG, "User data loaded: name=" + name + ", profilePictureUrl=" + profilePictureUrl);
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
    private void checkAndRequestImagePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                Toast.makeText(requireContext(), "Permission is needed to access your photos", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(requireActivity(), new String[]{permission}, 100);
        } else {
            openImagePicker();
        }
    }
    private void openImagePicker() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(pickIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{getContentIntent});

        if (chooserIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            profilePicturePickerLauncher.launch(chooserIntent);
        } else {
            Toast.makeText(getContext(), "No gallery app available", Toast.LENGTH_SHORT).show();
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
            String sanitizedName = restaurantId.replaceAll("[^a-zA-Z0-9]", "_"); // Sanitize restaurantId
            db.collection("FoodPlaces").document(restaurantId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("name");
                            String contactPhone = documentSnapshot.getString("contactPhone");
                            Map<String, Object> operatingHoursMap = (Map<String, Object>) documentSnapshot.get("operatingHours");
                            String logoUrl = documentSnapshot.getString("logoUrl");

                            // Store original values
                            originalName = name != null ? name : "";
                            originalContactPhone = contactPhone != null ? contactPhone : "";
                            originalOperatingHours.clear();
                            if (operatingHoursMap != null) {
                                originalOperatingHours.putAll(
                                        operatingHoursMap.entrySet().stream()
                                                .filter(e -> e.getValue() instanceof String)
                                                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()))
                                );
                            }
                            restaurantNameDisplay.setText(name != null ? name : "Restaurant Name");
                            restaurantPhoneDisplay.setText(contactPhone != null ? contactPhone : "+123-456-7890");

                            // Load operating hours
                            loadOperatingHours(operatingHoursMap);

                            if (logoUrl != null && !logoUrl.isEmpty()) {
                                originalLogoUrl = logoUrl;
                                Glide.with(ProfileFragment.this)
                                        .load(logoUrl)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .placeholder(R.drawable.white)
                                        .into(restaurantLogo);
                            } else {
                                loadLogoFromStorage(sanitizedName);
                            }

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

    private void loadOperatingHours(Map<String, Object> operatingHoursMap) {
        // Reset all checkboxes and fields
        mondayCheckbox.setChecked(false);
        tuesdayCheckbox.setChecked(false);
        wednesdayCheckbox.setChecked(false);
        thursdayCheckbox.setChecked(false);
        fridayCheckbox.setChecked(false);
        saturdayCheckbox.setChecked(false);
        sundayCheckbox.setChecked(false);
        mondayHours.setText("");
        tuesdayHours.setText("");
        wednesdayHours.setText("");
        thursdayHours.setText("");
        fridayHours.setText("");
        saturdayHours.setText("");
        sundayHours.setText("");
        mondayHours.setEnabled(false);
        tuesdayHours.setEnabled(false);
        wednesdayHours.setEnabled(false);
        thursdayHours.setEnabled(false);
        fridayHours.setEnabled(false);
        saturdayHours.setEnabled(false);
        sundayHours.setEnabled(false);

        if (operatingHoursMap != null) {
            if (operatingHoursMap.containsKey("Monday")) {
                mondayCheckbox.setChecked(true);
                mondayHours.setEnabled(true);
                mondayHours.setText((String) operatingHoursMap.get("Monday"));
            }
            if (operatingHoursMap.containsKey("Tuesday")) {
                tuesdayCheckbox.setChecked(true);
                tuesdayHours.setEnabled(true);
                tuesdayHours.setText((String) operatingHoursMap.get("Tuesday"));
            }
            if (operatingHoursMap.containsKey("Wednesday")) {
                wednesdayCheckbox.setChecked(true);
                wednesdayHours.setEnabled(true);
                wednesdayHours.setText((String) operatingHoursMap.get("Wednesday"));
            }
            if (operatingHoursMap.containsKey("Thursday")) {
                thursdayCheckbox.setChecked(true);
                thursdayHours.setEnabled(true);
                thursdayHours.setText((String) operatingHoursMap.get("Thursday"));
            }
            if (operatingHoursMap.containsKey("Friday")) {
                fridayCheckbox.setChecked(true);
                fridayHours.setEnabled(true);
                fridayHours.setText((String) operatingHoursMap.get("Friday"));
            }
            if (operatingHoursMap.containsKey("Saturday")) {
                saturdayCheckbox.setChecked(true);
                saturdayHours.setEnabled(true);
                saturdayHours.setText((String) operatingHoursMap.get("Saturday"));
            }
            if (operatingHoursMap.containsKey("Sunday")) {
                sundayCheckbox.setChecked(true);
                sundayHours.setEnabled(true);
                sundayHours.setText((String) operatingHoursMap.get("Sunday"));
            }
        }
    }

    private void loadLogoFromStorage(String sanitizedName) {
        StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
        logoRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    Log.d(TAG, "Loaded logo from Storage: " + logoUrl);
                    originalLogoUrl = logoUrl;
                    Glide.with(ProfileFragment.this)
                            .load(logoUrl)
                            .apply(new RequestOptions()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .error(R.drawable.white)) // Fallback image on error
                            .into(restaurantLogo);
                    // Update Firestore with the correct logoUrl
                    db.collection("FoodPlaces").document(restaurantId)
                            .update("logoUrl", logoUrl)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated logoUrl in Firestore"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update logoUrl in Firestore", e));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to load logo from Storage: " + e.getMessage());
                    Glide.with(ProfileFragment.this)
                            .load(R.drawable.white) // Use default logo on failure
                            .apply(new RequestOptions()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(restaurantLogo);
                    Toast.makeText(getContext(), "Unable to load logo", Toast.LENGTH_SHORT).show();
                });
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
        addressInput.setTextColor(getResources().getColor(android.R.color.black));
        addressInput.setHintTextColor(0xB0FFFFFF);
        addressInput.setBackgroundTintList(getResources().getColorStateList(android.R.color.black));

        TextView coordinatesText = new TextView(getContext());
        coordinatesText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));
        coordinatesText.setText(String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitude, longitude));
        coordinatesText.setTextColor(getResources().getColor(android.R.color.black));
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
        availabilityToggle.setTextColor(getResources().getColor(android.R.color.black));
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
        deleteButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_orange_dark));
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

        ;
        String sanitizedName = restaurantId.replaceAll("[^a-zA-Z0-9]", "_"); // Sanitize restaurantId


        Map<String, String> operatingHours = new HashMap<>();
        if (mondayCheckbox.isChecked() && !mondayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Monday", mondayHours.getText().toString().trim());
        }
        if (tuesdayCheckbox.isChecked() && !tuesdayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Tuesday", tuesdayHours.getText().toString().trim());
        }
        if (wednesdayCheckbox.isChecked() && !wednesdayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Wednesday", wednesdayHours.getText().toString().trim());
        }
        if (thursdayCheckbox.isChecked() && !thursdayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Thursday", thursdayHours.getText().toString().trim());
        }
        if (fridayCheckbox.isChecked() && !fridayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Friday", fridayHours.getText().toString().trim());
        }
        if (saturdayCheckbox.isChecked() && !saturdayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Saturday", saturdayHours.getText().toString().trim());
        }
        if (sundayCheckbox.isChecked() && !sundayHours.getText().toString().trim().isEmpty()) {
            operatingHours.put("Sunday", sundayHours.getText().toString().trim());
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
        updates.put("operatingHours", operatingHours);
        updates.put("rememberMe", true);
        updates.put("role", "restaurant");
        updates.put("uid", user != null ? user.getUid() : "");
        if (logoUri != null) {
            StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
            logoRef.putFile(logoUri)
                    .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String logoUrl = uri.toString();
                        Log.d(TAG, "Logo uploaded successfully, URL: " + logoUrl);
                        updates.put("logoUrl", logoUrl);
                        // Update users collection with logoUrl as profilePictureUrl
                        if (user != null) {
                            Map<String, Object> userUpdates = new HashMap<>();
                            userUpdates.put("profilePictureUrl", logoUrl);
                            userUpdates.put("role", "restaurant");
                            userUpdates.put("restaurantId", restaurantId);
                            db.collection("users").document(user.getUid())
                                    .set(userUpdates, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile picture URL updated: " + logoUrl))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update user profile picture URL", e));
                        }
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                updates.put("deviceToken", token); // Add to FoodPlaces document
                                Log.d(TAG, "Saving deviceToken: " + token);
                            } else {
                                Log.e(TAG, "Failed to get FCM token", task.getException());
                            }
                            updateFirestore(restaurantRef, updates, updatedAddresses);
                        });

                        updateFirestore(restaurantRef, updates, updatedAddresses);
                        if (isAdded()) {
                            Glide.with(ProfileFragment.this)
                                    .load(logoUrl)
                                    .apply(new RequestOptions()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .error(R.drawable.white))
                                    .into(restaurantLogo);
                            logoUri = null;
                            originalLogoUrl = logoUrl;
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

        Map<String, String> currentOperatingHours = new HashMap<>();
        if (mondayCheckbox.isChecked())
            currentOperatingHours.put("Monday", mondayHours.getText().toString().trim());
        if (tuesdayCheckbox.isChecked())
            currentOperatingHours.put("Tuesday", tuesdayHours.getText().toString().trim());
        if (wednesdayCheckbox.isChecked())
            currentOperatingHours.put("Wednesday", wednesdayHours.getText().toString().trim());
        if (thursdayCheckbox.isChecked())
            currentOperatingHours.put("Thursday", thursdayHours.getText().toString().trim());
        if (fridayCheckbox.isChecked())
            currentOperatingHours.put("Friday", fridayHours.getText().toString().trim());
        if (saturdayCheckbox.isChecked())
            currentOperatingHours.put("Saturday", saturdayHours.getText().toString().trim());
        if (sundayCheckbox.isChecked())
            currentOperatingHours.put("Sunday", sundayHours.getText().toString().trim());

        if (!currentOperatingHours.equals(originalOperatingHours)) {
            hasChanges = true;
        }

        if (logoUri != null && !logoUri.toString().equals(originalLogoUrl)) {
            hasChanges = true;
        }

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
    private void showChangePhoneDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Change Contact Phone");

        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        TextInputEditText input = new TextInputEditText(requireContext());

        input.setText(restaurantPhoneDisplay.getText().toString());
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        inputLayout.setHint("Enter new phone number");
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        inputLayout.addView(input);
        int padding = (int) (16 * getResources().getDisplayMetrics().density); // 16dp padding
        inputLayout.setPadding(padding, padding, padding, padding);
        builder.setView(inputLayout); // <-- This is the fix

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();
            if (newPhone.isEmpty()) {
                Toast.makeText(getContext(), "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentReference restaurantRef = db.collection("FoodPlaces").document(restaurantId);
            restaurantRef.update("contactPhone", newPhone)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Contact phone updated to: " + newPhone);
                        restaurantPhoneDisplay.setText(newPhone);
                        originalContactPhone = newPhone;
                        Toast.makeText(getContext(), "Contact phone updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update contact phone", e);
                        Toast.makeText(getContext(), "Failed to update phone", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}