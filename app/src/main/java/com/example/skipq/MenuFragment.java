package com.example.skipq;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.MenuAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MenuFragment extends Fragment {

    private RecyclerView recyclerView;
    private MenuAdaptor menuAdaptor;
    private ArrayList<MenuDomain> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        FirebaseApp.initializeApp(requireContext());
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
        }

        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("MenuFragment", "restaurantId is null or empty");
        } else {
            Log.d("MenuFragment", "restaurantId: " + restaurantId);
            fetchMenuItems();
        }

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycleViewMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        menuAdaptor = new MenuAdaptor(getContext(), menuList);
        recyclerView.setAdapter(menuAdaptor);

        return view;
    }

    private void fetchMenuItems() {
        // Log Firestore path for debugging
        String path = "FoodPlaces/" + restaurantId + "/Menu/" + restaurantId + " Menu/Items";
        Log.d("MenuFragment", "Fetching data from Firestore path: " + path);

        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .document(restaurantId + " Menu")
                .collection("Items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        menuList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("MenuFragment", "Document data: " + document.getData());

                            MenuDomain menuItem = new MenuDomain();
                            menuItem.setItemName(document.getString("Item Name"));
                            menuItem.setItemDescription(document.getString("Item Description"));
                            menuItem.setItemPrice(document.getString("Item Price"));
                            menuItem.setItemImg(document.getString("Item Img"));

                            // Log fetched data for debugging
                            Log.d("MenuFragment", "Fetched menu item: " + menuItem.getItemName());

                            menuList.add(menuItem);
                        }
                        menuAdaptor.notifyDataSetChanged();
                    } else {
                        Log.e("MenuFragment", "Error getting menu items: ", task.getException());
                    }
                });
    }
}