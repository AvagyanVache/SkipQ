package com.example.skipq.Activity;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.skipq.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RestaurantSignUpActivity2 extends AppCompatActivity {

    private EditText restaurantName, restaurantApiLink, addressInput;
    private RecyclerView recyclerViewAddresses;
    private AddressAdapter addressAdapter;
    private List<RestaurantAddress> addressList = new ArrayList<>();
    private Button uploadLogoButton, signUpButton, addAddressButton;
    private TextView backButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri logoUri;
    private String email, password, uid;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_signup2);

        backButton = findViewById(R.id.backButton);
        restaurantName = findViewById(R.id.restaurant_name);
        restaurantApiLink = findViewById(R.id.restaurant_api_link);
        addressInput = findViewById(R.id.address_input);
        recyclerViewAddresses = findViewById(R.id.recycler_view_addresses);
        addAddressButton = findViewById(R.id.add_address_button);
        uploadLogoButton = findViewById(R.id.uploadLogoButton);
        signUpButton = findViewById(R.id.SignUpButton);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        uid = intent.getStringExtra("uid");

        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        addressAdapter = new AddressAdapter(addressList);
        recyclerViewAddresses.setAdapter(addressAdapter);

        backButton.setOnClickListener(v -> finish());

        uploadLogoButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, PICK_IMAGE_REQUEST);
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

            if (validateInput(name, apiLink)) {
                if (logoUri != null) {
                    uploadLogoAndSaveData(name, apiLink);
                } else if (!apiLink.isEmpty()) {
                    fetchMenuFromApiAndSave(apiLink, name, null);
                } else {
                    saveRestaurantWithMenu(name, apiLink, null, null);
                }
            }
        });
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
        if (!apiLink.isEmpty() && !Patterns.WEB_URL.matcher(apiLink).matches()) {
            Toast.makeText(this, "Please enter a valid API URL (optional)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadLogoAndSaveData(String name, String apiLink) {
        StorageReference logoRef = storage.getReference().child("restaurant_logos/" + uid + "_logo.jpg");
        logoRef.putFile(logoUri)
                .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    if (!apiLink.isEmpty()) {
                        fetchMenuFromApiAndSave(apiLink, name, logoUrl);
                    } else {
                        saveRestaurantWithMenu(name, apiLink, logoUrl, null);
                    }
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload logo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchMenuFromApiAndSave(String apiLink, String restaurantName, String logoUrl) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiLink,
                response -> {
                    try {
                        JSONArray menuItems = new JSONArray(response);
                        Map<String, Object> menuData = new HashMap<>();
                        for (int i = 0; i < menuItems.length(); i++) {
                            JSONObject item = menuItems.getJSONObject(i);
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("name", item.getString("name"));
                            itemData.put("price", item.getInt("price"));
                            itemData.put("prepTime", item.getInt("prepTime"));
                            menuData.put(item.getString("name"), itemData);
                        }
                        saveRestaurantWithMenu(restaurantName, apiLink, logoUrl, menuData);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveRestaurantWithMenu(restaurantName, apiLink, logoUrl, null);
                    }
                },
                error -> {
                    Toast.makeText(this, "Failed to fetch menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    saveRestaurantWithMenu(restaurantName, apiLink, logoUrl, null);
                });
        queue.add(stringRequest);
    }

    private void saveRestaurantWithMenu(String name, String apiLink, String logoUrl, Map<String, Object> menuData) {
        db.collection("FoodPlaces").document(name).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Toast.makeText(this, "A restaurant with this name already exists.", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> restaurantInfo = new HashMap<>();
                        restaurantInfo.put("name", name);
                        restaurantInfo.put("email", email);
                        restaurantInfo.put("uid", uid);
                        restaurantInfo.put("role", "restaurant");
                        if (!apiLink.isEmpty()) {
                            restaurantInfo.put("apiLink", apiLink);
                        }
                        if (logoUrl != null) {
                            restaurantInfo.put("logoUrl", logoUrl);
                        }

                        db.collection("FoodPlaces").document(name)
                                .set(restaurantInfo)
                                .addOnSuccessListener(aVoid -> {
                                    // Save addresses to subcollection
                                    for (RestaurantAddress address : addressList) {
                                        Map<String, Object> addressData = new HashMap<>();
                                        addressData.put("address", address.getAddress());
                                        addressData.put("latitude", address.getLatitude());
                                        addressData.put("longitude", address.getLongitude());
                                        db.collection("FoodPlaces").document(name)
                                                .collection("Addresses").add(addressData);
                                    }

                                    // Save menu if provided
                                    String menuDocName = "DefaultMenu";
                                    if (menuData != null) {
                                        for (Map.Entry<String, Object> entry : menuData.entrySet()) {
                                            db.collection("FoodPlaces").document(name).collection("Menu")
                                                    .document(menuDocName).collection("Items")
                                                    .document(entry.getKey()).set(entry.getValue());
                                        }
                                    } else {
                                        db.collection("FoodPlaces").document(name).collection("Menu")
                                                .document(menuDocName).set(new HashMap<>());
                                    }

                                    Toast.makeText(this, "Restaurant registered successfully!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RestaurantSignUpActivity2.this, HomeActivity.class);
                                    intent.putExtra("userRole", "restaurant");
                                    intent.putExtra("restaurantId", name);
                                    intent.putExtra("FRAGMENT_TO_LOAD", "RESTAURANT_DASHBOARD");
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save restaurant: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // Address model class
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