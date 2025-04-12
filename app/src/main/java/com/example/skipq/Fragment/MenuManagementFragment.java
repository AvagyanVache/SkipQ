package com.example.skipq.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.MenuManagementAdapter;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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
    private TextView itemImg;
    private boolean isUpdating = false; // Flag to track add vs update mode
    private String originalItemName; // Store original name for updating Firestore

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

        db = FirebaseFirestore.getInstance();
        restaurantId = getArguments().getString("restaurantId");

        // Setup menu management
        menuItems = new ArrayList<>();
        menuAdapter = new MenuManagementAdapter(menuItems, this::updateItem, this::deleteItem);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuRecyclerView.setAdapter(menuAdapter);

        // Load data
        loadMenuItems();

        // Add/Update item button listener
        addItemButton.setOnClickListener(v -> {
            if (cardView.getVisibility() == View.GONE) {
                // Show CardView for adding a new item
                cardView.setVisibility(View.VISIBLE);
                isUpdating = false;
                addItemButton.setText("Submit Item");
                clearInputs();
            } else {
                // Submit based on mode (add or update)
                if (isUpdating) {
                    submitUpdate();
                } else {
                    addItem();
                }
            }
        });

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
                        item.setItemImg(doc.getString("Item Img"));
                        menuItems.add(item);
                    }
                    menuAdapter.notifyDataSetChanged();
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
        String img = itemImg.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", img.isEmpty() ? "" : img);

        db.collection("FoodPlaces").document(restaurantId).collection("Menu")
                .document("DefaultMenu").collection("Items").document(name)
                .set(itemData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item added!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                    clearInputs();
                    cardView.setVisibility(View.GONE);
                    addItemButton.setText("Add Item");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateItem(MenuDomain item) {
        // Populate input fields with item data
        itemName.setText(item.getItemName());
        itemPrice.setText(item.getItemPrice());
        itemPrepTime.setText(String.valueOf(item.getPrepTime()));
        itemDescription.setText(item.getItemDescription());
        itemImg.setText(item.getItemImg());

        // Show CardView and set update mode
        cardView.setVisibility(View.VISIBLE);
        isUpdating = true;
        originalItemName = item.getItemName(); // Store original name for Firestore update
        addItemButton.setText("Update Item");
    }

    private void submitUpdate() {
        String name = itemName.getText().toString().trim();
        String price = itemPrice.getText().toString().trim();
        String prepTime = itemPrepTime.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        String img = itemImg.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(getContext(), "Name, price, and prep time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("Item Name", name);
        itemData.put("Item Price", price);
        itemData.put("Prep Time", Integer.parseInt(prepTime));
        itemData.put("Item Description", description.isEmpty() ? "" : description);
        itemData.put("Item Img", img.isEmpty() ? "" : img);

        // Start a batch to ensure atomic updates
        WriteBatch batch = db.batch();

        // If the name changed, delete the old document
        if (!originalItemName.equals(name)) {
            DocumentReference oldDocRef = db.collection("FoodPlaces").document(restaurantId)
                    .collection("Menu").document("DefaultMenu").collection("Items").document(originalItemName);
            batch.delete(oldDocRef);
        }

        // Write the updated item to the new document ID (name)
        DocumentReference newDocRef = db.collection("FoodPlaces").document(restaurantId)
                .collection("Menu").document("DefaultMenu").collection("Items").document(name);
        batch.set(newDocRef, itemData);

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item updated!", Toast.LENGTH_SHORT).show();
                    loadMenuItems();
                    clearInputs();
                    cardView.setVisibility(View.GONE);
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

    private void clearInputs() {
        itemName.setText("");
        itemPrice.setText("");
        itemPrepTime.setText("");
        itemDescription.setText("");
        itemImg.setText("");
    }
}