package com.example.skipq.Fragment;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.MenuManagementAdapter;
import com.example.skipq.Domain.MenuDomain;
import android.Manifest;
import com.example.skipq.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MenuManagementFragment extends Fragment {

    private RecyclerView menuRecyclerView;
    private MenuManagementAdapter menuAdapter;
    private List<MenuDomain> menuItems;
    private EditText itemName, itemPrice, itemPrepTime, itemDescription;
    private Button addItemButton;
    private CardView cardView;
    private FirebaseFirestore db;
    private String restaurantId;
    private TextView itemImg, backButton; // Added backButton
    private boolean isUpdating = false; // Flag to track add vs update mode
    private String originalItemName; // Store original name for updating Firestore
    private String selectedImageBase64; // Store Base64 string for selected image
    private ActivityResultLauncher<String> pickImageLauncher; // Image picker
    private ActivityResultLauncher<String> requestPermissionLauncher; // Permission handlerz
    private Switch availabilitySwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_management, container, false);

        // Initialize UI elements
        menuRecyclerView = view.findViewById(R.id.menu_recycler_view);
        itemName = view.findViewById(R.id.item_name);
        itemPrice = view.findViewById(R.id.item_price);
        itemPrepTime = view.findViewById(R.id.item_prep_time);
        itemDescription = view.findViewById(R.id.item_description);
        itemImg = view.findViewById(R.id.item_img);
        addItemButton = view.findViewById(R.id.add_item_button);
        cardView = view.findViewById(R.id.cardView);
        backButton = view.findViewById(R.id.backButton);
        availabilitySwitch = view.findViewById(R.id.availability_switch);

        selectedImageBase64 = null;

        db = FirebaseFirestore.getInstance();
        restaurantId = getArguments().getString("restaurantId");

        menuItems = new ArrayList<>();
        menuAdapter = new MenuManagementAdapter(menuItems, this::updateItem, this::deleteItem);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecyclerView.setAdapter(menuAdapter);

        backButton.setVisibility(View.GONE);

        loadMenuItems();

        addItemButton.setOnClickListener(v -> {
            if (cardView.getVisibility() == View.GONE) {
                cardView.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                isUpdating = false;
                addItemButton.setText("Submit Item");
                clearInputs();
            } else {
                if (isUpdating) {
                    submitUpdate();
                } else {
                    addItem();
                }
            }
        });

        backButton.setOnClickListener(v -> {
            // Hide CardView and backButton, reset UI
            cardView.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            addItemButton.setText("Add Item");
            clearInputs();
            isUpdating = false;
        });
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    // Convert image to Base64
                    Bitmap bitmap = loadBitmapFromUri(uri);
                    if (bitmap != null) {
                        selectedImageBase64 = bitmapToBase64(bitmap);
                        itemImg.setText("Image selected"); // Visual feedback
                        Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

// Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        });

// Make itemImg clickable to select image
        itemImg.setOnClickListener(v -> checkAndRequestPermission());
        return view;
    }

    private void loadMenuItems() {
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    menuItems.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        MenuDomain item = new MenuDomain();
                        item.setItemName(doc.getString("Item Name"));
                        item.setItemPrice(doc.getString("Item Price"));
                        if (doc.contains("Prep Time")) {
                            item.setPrepTime(doc.getLong("Prep Time").intValue());
                        } else {
                            item.setPrepTime(0);
                        }
                        item.setItemDescription(doc.getString("Item Description"));
                        String itemImgBase64 = doc.getString("Item Img");
                        item.setItemImg(itemImgBase64 != null ? itemImgBase64 : "");
                        Boolean available = doc.getBoolean("Available");
                        item.setAvailable(available != null ? available : true);

                        menuItems.add(item);
                    }
                    menuAdapter.updateItems(menuItems); // Use updateItems instead of notifyDataSetChanged
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addItem() {
        String name = itemName.getText().toString().trim();
        String price = itemPrice.getText().toString().trim();
        String prepTime = itemPrepTime.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        boolean isAvailable = availabilitySwitch.isChecked();

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", selectedImageBase64 != null ? selectedImageBase64 : "");
        itemData.put("Available", isAvailable);

        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(name)
                .set(itemData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item added!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                    clearInputs();
                    cardView.setVisibility(View.GONE);
                    backButton.setVisibility(View.GONE);
                    addItemButton.setText("Add Item");
                })
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to add item", e);
                    Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void updateItem(MenuDomain item) {
        itemName.setText(item.getItemName());
        itemPrice.setText(item.getItemPrice());
        itemPrepTime.setText(String.valueOf(item.getPrepTime()));
        itemDescription.setText(item.getItemDescription());
        selectedImageBase64 = item.getItemImg();
        itemImg.setText(selectedImageBase64 != null && !selectedImageBase64.isEmpty() ? "Image selected" : "");
        availabilitySwitch.setChecked(item.isAvailable()); // Set switch state

        cardView.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        isUpdating = true;
        originalItemName = item.getItemName();
        addItemButton.setText("Update Item");
    }

    private void submitUpdate() {
        String name = itemName.getText().toString().trim();
        String price = itemPrice.getText().toString().trim();
        String prepTime = itemPrepTime.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        boolean isAvailable = availabilitySwitch.isChecked(); // Get switch state

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", selectedImageBase64 != null ? selectedImageBase64 : "");
        itemData.put("Available", isAvailable); // Use switch state

        WriteBatch batch = db.batch();

        if (!originalItemName.equals(name)) {
            DocumentReference oldDocRef = db.collection("FoodPlaces").document(restaurantId)
                    .collection("Menu").document("DefaultMenu").collection("Items").document(originalItemName);
            batch.delete(oldDocRef);
        }

        DocumentReference newDocRef = db.collection("FoodPlaces").document(restaurantId)
                .collection("Menu").document("DefaultMenu").collection("Items").document(name);
        batch.set(newDocRef, itemData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item updated!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                    clearInputs();
                    cardView.setVisibility(View.GONE);
                    backButton.setVisibility(View.GONE);
                    addItemButton.setText("Add Item");
                    isUpdating = false;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteItem(String itemName) {
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(itemName)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item deleted!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void checkAndRequestPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) {
            inputStream.close();
        }
        return bitmap;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // 80% quality to reduce size
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    private void clearInputs() {
        itemName.setText("");
        itemPrice.setText("");
        itemPrepTime.setText("");
        itemDescription.setText("");
        itemImg.setText("");
        selectedImageBase64 = null;
        availabilitySwitch.setChecked(true);
    }
}