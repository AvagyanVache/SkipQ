package com.example.skipq.Fragment;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

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
import com.example.skipq.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManagementFragment extends Fragment {

    private RecyclerView menuRecyclerView;
    private MenuManagementAdapter menuAdapter;
    private List<MenuDomain> menuItems;
    private EditText itemName, itemPrice, itemPrepTime, itemDescription;
    private Button addItemButton;
    private CardView cardView;
    private FirebaseFirestore db;
    private String restaurantId;
    private TextView itemImg, backButton;
    private boolean isUpdating = false;
    private String originalItemName;
    private Uri selectedImageUri;
    private FirebaseStorage storage;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Switch availabilitySwitch;
    private String originalDocumentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_management, container, false);

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

        selectedImageUri = null;
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        restaurantId = getArguments().getString("restaurantId");

        menuItems = new ArrayList<>();
        menuAdapter = new MenuManagementAdapter(getContext(), menuItems, this::updateItem, this::deleteItem);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecyclerView.setAdapter(menuAdapter);

        backButton.setVisibility(View.GONE);

        loadMenuItems();

        addItemButton.setOnClickListener(v -> {
            if (cardView.getVisibility() != View.VISIBLE) {
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
            cardView.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            addItemButton.setText("Add Item");
            clearInputs();
            isUpdating = false;
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                itemImg.setText("Image selected");
                Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        });

        itemImg.setOnClickListener(v -> checkAndRequestPermission());
        return view;
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

    private Uri compressImage(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        int maxSize = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale < 1) {
            width = (int) (width * scale);
            height = (int) (height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        File tempFile = File.createTempFile("compressed", ".jpg", requireContext().getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(data);
        fos.close();

        return Uri.fromFile(tempFile);
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
                        item.setPrepTime(doc.getLong("Prep Time") != null ? doc.getLong("Prep Time").intValue() : 0);
                        item.setItemDescription(doc.getString("Item Description"));
                        item.setItemImg(doc.getString("Item Img") != null ? doc.getString("Item Img") : "");
                        Boolean available = doc.getBoolean("Available");
                        item.setAvailable(available != null ? available : true);
                        item.setDocumentId(doc.getId()); // Store the document ID
                        menuItems.add(item);
                    }
                    menuAdapter.updateItems(menuItems);
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

        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(sanitizedName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(getContext(), "Item name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        if (selectedImageUri != null) {
                            try {
                                Uri compressedUri = compressImage(selectedImageUri);
                                uploadImageAndSaveItem(name, price, prepTime, description, compressedUri, isAvailable);
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Failed to compress image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            saveItemToFirestore(name, price, prepTime, description, "", isAvailable);
                        }
                    }
                });
    }

    private void uploadImageAndSaveItem(String name, String price, String prepTime, String description, Uri imageUri, boolean isAvailable) {
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + sanitizedName + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveItemToFirestore(name, price, prepTime, description, imageUrl, isAvailable);
                }))
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to upload image", e);
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveItemToFirestore(name, price, prepTime, description, "", isAvailable);
                });
    }

    private void saveItemToFirestore(String name, String price, String prepTime, String description, String imageUrl, boolean isAvailable) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", imageUrl);
        itemData.put("Available", isAvailable);

        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(sanitizedName)
                .set(itemData, SetOptions.merge())
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
        selectedImageUri = null;
        itemImg.setText(item.getItemImg() != null && !item.getItemImg().isEmpty() ? "Image loaded" : "No image");
        availabilitySwitch.setChecked(item.isAvailable());

        cardView.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        isUpdating = true;
        originalItemName = item.getItemName();
        originalDocumentId = item.getDocumentId(); // Store the original document ID
        addItemButton.setText("Update Item");
    }

    private void submitUpdate() {
        String name = itemName.getText().toString().trim();
        String price = itemPrice.getText().toString().trim();
        String prepTime = itemPrepTime.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        boolean isAvailable = availabilitySwitch.isChecked();

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the new name conflicts with another existing item (excluding the original item)
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(sanitizedName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && !sanitizedName.equals(originalDocumentId)) {
                        Toast.makeText(getContext(), "Item name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        if (selectedImageUri != null) {
                            try {
                                Uri compressedUri = compressImage(selectedImageUri);
                                uploadImageAndUpdateItem(name, price, prepTime, description, compressedUri, isAvailable);
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Failed to compress image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Proceed with update using existing image
                                String existingImageUrl = menuItems.stream()
                                        .filter(item -> item.getDocumentId().equals(originalDocumentId))
                                        .findFirst()
                                        .map(MenuDomain::getItemImg)
                                        .orElse("");
                                updateItemInFirestore(name, price, prepTime, description, existingImageUrl, isAvailable);
                            }
                        } else {
                            String existingImageUrl = menuItems.stream()
                                    .filter(item -> item.getDocumentId().equals(originalDocumentId))
                                    .findFirst()
                                    .map(MenuDomain::getItemImg)
                                    .orElse("");
                            updateItemInFirestore(name, price, prepTime, description, existingImageUrl, isAvailable);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to check item name", e);
                    Toast.makeText(getContext(), "Failed to validate item name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageAndUpdateItem(String name, String price, String prepTime, String description, Uri imageUri, boolean isAvailable) {
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + sanitizedName + ".jpg");

        // Delete the old image if it exists
        String oldSanitizedName = originalDocumentId; // Use the original document ID
        StorageReference oldImageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + oldSanitizedName + ".jpg");
        oldImageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("MenuManagementFragment", "Old image deleted: " + oldSanitizedName))
                .addOnFailureListener(e -> Log.w("MenuManagementFragment", "Failed to delete old image (may not exist): " + e.getMessage()));

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateItemInFirestore(name, price, prepTime, description, imageUrl, isAvailable);
                }))
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to upload image", e);
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    String existingImageUrl = menuItems.stream()
                            .filter(item -> item.getDocumentId().equals(originalDocumentId))
                            .findFirst()
                            .map(MenuDomain::getItemImg)
                            .orElse("");
                    updateItemInFirestore(name, price, prepTime, description, existingImageUrl, isAvailable);
                });
    }
    private void updateItemInFirestore(String name, String price, String prepTime, String description, String imageUrl, boolean isAvailable) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", imageUrl);
        itemData.put("Available", isAvailable);

        WriteBatch batch = db.batch();
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");

        // If the document ID has changed, delete the old document
        if (!sanitizedName.equals(originalDocumentId)) {
            batch.delete(
                    db.collection("FoodPlaces").document(restaurantId)
                            .collection("Menu").document("DefaultMenu").collection("Items").document(originalDocumentId)
            );
        }

        // Set the new/updated document
        batch.set(
                db.collection("FoodPlaces").document(restaurantId)
                        .collection("Menu").document("DefaultMenu").collection("Items").document(sanitizedName),
                itemData,
                SetOptions.merge()
        );

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
                    Log.e("MenuManagementFragment", "Failed to update item", e);
                    Toast.makeText(getContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteItem(String itemName) {
        // Find the document ID for the item
        String documentId = menuItems.stream()
                .filter(item -> item.getItemName().equals(itemName))
                .findFirst()
                .map(MenuDomain::getDocumentId)
                .orElse(null);

        if (documentId == null) {
            Toast.makeText(getContext(), "Item not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete the associated image
        StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + documentId + ".jpg");
        imageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("MenuManagementFragment", "Image deleted for item: " + itemName))
                .addOnFailureListener(e -> Log.w("MenuManagementFragment", "Failed to delete image (may not exist): " + e.getMessage()));

        // Delete the Firestore document
        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item deleted!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                })
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to delete item", e);
                    Toast.makeText(getContext(), "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearInputs() {
        itemName.setText("");
        itemPrice.setText("");
        itemPrepTime.setText("");
        itemDescription.setText("");
        itemImg.setText("");
        selectedImageUri = null;
        availabilitySwitch.setChecked(true);
    }
}