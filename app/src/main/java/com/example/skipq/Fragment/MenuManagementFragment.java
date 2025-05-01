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
import android.media.ExifInterface;
import android.graphics.Matrix;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private Uri selectedImageUri;
    private FirebaseStorage storage;// Store Base64 string for selected image
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

        selectedImageUri = null;
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
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
                selectedImageUri = uri;
                itemImg.setText("Image selected");
                Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
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
    private Uri compressImage(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        // Correct orientation using EXIF data
        ExifInterface exif = new ExifInterface(requireContext().getContentResolver().openInputStream(uri));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Resize to max 1024x1024
        int maxSize = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale < 1) {
            width = (int) (width * scale);
            height = (int) (height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        // Compress to JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        // Save to temporary file
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

        if (selectedImageUri != null) {
            try {
                Uri compressedUri = compressImage(selectedImageUri); // Compress image
                uploadImageAndSaveItem(name, price, prepTime, description, compressedUri, isAvailable);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to compress image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            saveItemToFirestore(name, price, prepTime, description, "", isAvailable);
        }
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
        selectedImageUri = null; // Reset URI
        itemImg.setText(item.getItemImg() != null && !item.getItemImg().isEmpty() ? "Image selected" : "");
        availabilitySwitch.setChecked(item.isAvailable());

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
        boolean isAvailable = availabilitySwitch.isChecked();

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            try {
                Uri compressedUri = compressImage(selectedImageUri);
                uploadImageAndUpdateItem(name, price, prepTime, description, compressedUri, isAvailable);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to compress image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            String existingImageUrl = menuItems.stream()
                    .filter(item -> item.getItemName().equals(originalItemName))
                    .findFirst()
                    .map(MenuDomain::getItemImg)
                    .orElse("");
            updateItemInFirestore(name, price, prepTime, description, existingImageUrl, isAvailable);
        }
    }
    private void uploadImageAndUpdateItem(String name, String price, String prepTime, String description, Uri imageUri, boolean isAvailable) {
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
        StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + sanitizedName + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateItemInFirestore(name, price, prepTime, description, imageUrl, isAvailable);
                }))
                .addOnFailureListener(e -> {
                    Log.e("MenuManagementFragment", "Failed to upload image", e);
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Log.e("MenuManagementFragment", "Failed to update item", e);
                    Toast.makeText(getContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteItem(String itemName) {
        String sanitizedName = itemName.replaceAll("[^a-zA-Z0-9]", "_");
        StorageReference imageRef = storage.getReference().child("menu_images/" + restaurantId + "/" + sanitizedName + ".jpg");

        imageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("MenuManagementFragment", "Image deleted for item: " + itemName))
                .addOnFailureListener(e -> Log.e("MenuManagementFragment", "Failed to delete image: " + e.getMessage()));

        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(itemName)
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
    private void checkAndRequestPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }



    private void clearInputs() {
        itemName.setText("");
        itemPrice.setText("");
        itemPrepTime.setText("");
        itemDescription.setText("");
        itemImg.setText("");
        selectedImageUri = null; // Changed from selectedImageBase64
        availabilitySwitch.setChecked(true);
    }
}