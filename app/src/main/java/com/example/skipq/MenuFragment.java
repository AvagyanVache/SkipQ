package com.example.skipq;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skipq.Adaptor.MenuAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class MenuFragment extends Fragment {
    private RecyclerView recyclerViewMenu;
    private MenuAdaptor menuAdaptor;
    private ArrayList<MenuDomain> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        recyclerViewMenu = view.findViewById(R.id.recycleViewMenu);
        recyclerViewMenu.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        // Get restaurant ID from arguments
        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
            fetchMenuItems();
        }

        menuAdaptor = new MenuAdaptor(requireContext(), menuList);
        recyclerViewMenu.setAdapter(menuAdaptor);

        return view;
    }

    private void fetchMenuItems() {
        if (restaurantId == null) {
            Log.e("MenuFragment", "Restaurant ID is null");
            Toast.makeText(getContext(), "Error loading menu", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .get()
                .addOnSuccessListener(menuSnapshots -> {
                    for (QueryDocumentSnapshot menuDoc : menuSnapshots) {
                        String menuId = menuDoc.getId(); // e.g., "KFC_Menu"

                        db.collection("FoodPlaces")
                                .document(restaurantId)
                                .collection("Menu")
                                .document(menuId)
                                .collection("Items")
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    menuList.clear();
                                    for (QueryDocumentSnapshot itemDoc : itemSnapshots) {
                                        MenuDomain menuItem = new MenuDomain();
                                        menuItem.setItemName(itemDoc.getString("Item Name"));
                                        menuItem.setItemDescription(itemDoc.getString("Item Description"));
                                        menuItem.setItemPrice(itemDoc.getString("Item Price"));
                                        menuItem.setItemImg(itemDoc.getString("Item Img"));

                                        menuList.add(menuItem);
                                    }
                                    menuAdaptor.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching menu items", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching menu", e);
                    Toast.makeText(getContext(), "Failed to load menu", Toast.LENGTH_SHORT).show();
                });
    }
}
