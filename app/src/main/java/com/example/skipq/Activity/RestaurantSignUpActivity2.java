package com.example.skipq.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

import java.util.HashMap;
import java.util.Map;

public class RestaurantSignUpActivity2 extends AppCompatActivity {

    private EditText restaurantName, restaurantAddress, restaurantApiLink;
    private Button uploadLogoButton, signUpButton;
    private TextView backButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri logoUri;
    private String email, password, uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_signup2);

        backButton = findViewById(R.id.backButton);
        restaurantName = findViewById(R.id.restaurant_name);
        restaurantAddress = findViewById(R.id.restaurant_address);
        restaurantApiLink = findViewById(R.id.restaurant_api_link);
        uploadLogoButton = findViewById(R.id.uploadLogoButton);
        signUpButton = findViewById(R.id.SignUpButton);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        uid = intent.getStringExtra("uid");

        backButton.setOnClickListener(v -> finish());

        uploadLogoButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, 1);
        });

        signUpButton.setOnClickListener(v -> {
            String name = restaurantName.getText().toString().trim();
            String address = restaurantAddress.getText().toString().trim();
            String apiLink = restaurantApiLink.getText().toString().trim();

            if (validateInput(name, address, apiLink)) {
                if (logoUri != null) {
                    uploadLogoAndSaveData(name, address, apiLink);
                } else if (!apiLink.isEmpty()) {
                    fetchMenuFromApiAndSave(apiLink, name, null);
                } else {
                    saveRestaurantWithMenu(name, address, apiLink, null, null);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            logoUri = data.getData();
            Toast.makeText(this, "Logo selected successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String name, String address, String apiLink) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Restaurant name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Restaurant address is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!apiLink.isEmpty() && !Patterns.WEB_URL.matcher(apiLink).matches()) {
            Toast.makeText(this, "Please enter a valid API URL (optional)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadLogoAndSaveData(String name, String address, String apiLink) {
        StorageReference logoRef = storage.getReference().child("restaurant_logos/" + uid + "_logo.jpg");
        logoRef.putFile(logoUri)
                .addOnSuccessListener(taskSnapshot -> logoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    if (!apiLink.isEmpty()) {
                        fetchMenuFromApiAndSave(apiLink, name, logoUrl);
                    } else {
                        saveRestaurantWithMenu(name, address, apiLink, logoUrl, null);
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
                        saveRestaurantWithMenu(restaurantName, restaurantAddress.getText().toString().trim(), apiLink, logoUrl, menuData);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveRestaurantWithMenu(restaurantName, restaurantAddress.getText().toString().trim(), apiLink, logoUrl, null);
                    }
                },
                error -> {
                    Toast.makeText(this, "Failed to fetch menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    saveRestaurantWithMenu(restaurantName, restaurantAddress.getText().toString().trim(), apiLink, logoUrl, null);
                });
        queue.add(stringRequest);
    }

    private void saveRestaurantWithMenu(String name, String address, String apiLink, String logoUrl, Map<String, Object> menuData) {
        db.collection("FoodPlaces").document(name).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Toast.makeText(this, "A restaurant with this name already exists.", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> restaurantInfo = new HashMap<>();
                        restaurantInfo.put("name", name);
                        restaurantInfo.put("email", email);
                        restaurantInfo.put("address", address);
                        restaurantInfo.put("uid", uid);
                        restaurantInfo.put("role", "restaurant"); // Add role field
                        if (!apiLink.isEmpty()) {
                            restaurantInfo.put("apiLink", apiLink);
                        }
                        if (logoUrl != null) {
                            restaurantInfo.put("logoUrl", logoUrl);
                        }

                        db.collection("FoodPlaces").document(name)
                                .set(restaurantInfo)
                                .addOnSuccessListener(aVoid -> {
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
}