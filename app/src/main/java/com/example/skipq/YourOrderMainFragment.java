package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Adaptor.YourOrderMainAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class YourOrderMainFragment extends Fragment {
    private RecyclerView recyclerView;
    private YourOrderMainAdaptor yourOrdersAdapter;
    private TextView emptyOrderText, goShoppingText;
    private ArrayList<YourOrderMainDomain> groupedOrders;
    private FirebaseFirestore firestore;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.your_order_main_fragment, container, false);

        recyclerView = view.findViewById(R.id.YourOrderMainRecycleView);
        emptyOrderText = view.findViewById(R.id.OrderEmpty);
        goShoppingText = view.findViewById(R.id.GoShopping);

        firestore = FirebaseFirestore.getInstance();
        groupedOrders = new ArrayList<>();
        groupedOrders = loadAndGroupOrders();


        yourOrdersAdapter = new YourOrderMainAdaptor(requireContext(), groupedOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(yourOrdersAdapter);

        updateEmptyStateVisibility();
        loadOrdersFromFirestore();


        goShoppingText.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);intent.putExtra("FRAGMENT_TO_LOAD", "HOME");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent);
        });

        yourOrdersAdapter.setOnItemClickListener(position -> {
            YourOrderMainDomain selectedOrder = groupedOrders.get(position);
            openYourOrderFragment(selectedOrder);
        });

        return view;
    }
    private void loadOrdersFromFirestore() {
        firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    groupedOrders.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            order.setStartTime(document.getLong("startTime"));
                            groupedOrders.add(order);
                        }
                    }
                    yourOrdersAdapter.notifyDataSetChanged();
                    updateEmptyStateVisibility();
                });
    }
    private void saveOrderToFirestore(YourOrderMainDomain order) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("restaurant", order.getRestaurant());
        orderData.put("totalPrice", order.getTotalPrice());
        orderData.put("totalPrepTime", order.getTotalPrepTime());
        orderData.put("items", order.getItems());
        orderData.put("startTime", System.currentTimeMillis()); // Save order time

        db.collection("orders")
                .document(order.getOrderId())
                .set(orderData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Order successfully saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving order", e));
    }


    @Override
    public void onResume() {
        super.onResume();
        ArrayList<YourOrderMainDomain> updatedOrders = loadAndGroupOrders();

        if (!updatedOrders.isEmpty()) {
            groupedOrders.clear();
            groupedOrders.addAll(updatedOrders);
            yourOrdersAdapter.notifyDataSetChanged();
            updateEmptyStateVisibility();
        } else {
            Log.e("YourOrderMainFragment", "No orders to display in RecyclerView.");
        }
    }

    private ArrayList<YourOrderMainDomain> loadAndGroupOrders() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        ArrayList<MenuDomain> allItems = new Gson().fromJson(cartItemsJson, new TypeToken<ArrayList<MenuDomain>>() {}.getType());


        if (allItems == null || allItems.isEmpty()) {
            Log.e("YourOrderMainFragment", "No items found in SharedPreferences!");
            return new ArrayList<>();
        }
        ArrayList<YourOrderMainDomain> ordersList = new ArrayList<>();
        Map<String, YourOrderMainDomain> restaurantMap = new HashMap<>();

        for (MenuDomain item : allItems) {
            if (item.getRestaurant() == null) {
                Log.e("YourOrderMainFragment", "Item has no restaurant: " + item.getItemName());
                item.setRestaurant(new RestaurantDomain("Unknown Restaurant", ""));

            }
            String restaurantKey = item.getRestaurant().getName();
            if (!restaurantMap.containsKey(restaurantKey)) {
                restaurantMap.put(restaurantKey, new YourOrderMainDomain(
                        "generatedOrderId",
                        item.getRestaurant(),
                        0.0,
                        0,
                        new ArrayList<>(),
                        System.currentTimeMillis()
                ));
            }
            YourOrderMainDomain order = restaurantMap.get(restaurantKey);
            if (order != null) {
                order.getItems().add(item);
                order.setTotalPrice(order.getTotalPrice() + Double.parseDouble(item.getItemPrice()));
                order.setTotalPrepTime(order.getTotalPrepTime() + item.getPrepTime());
            }


            Log.d("YourOrderMainFragment", "Processing item: " + item.getItemName() + " for restaurant: " + restaurantKey);

            if (!restaurantMap.containsKey(restaurantKey)) {
                restaurantMap.put(restaurantKey, new YourOrderMainDomain(
                        "generatedOrderId",
                        item.getRestaurant(),
                        0.0,
                        0,
                        new ArrayList<>(),
                        System.currentTimeMillis()
                ));
            }
            return new ArrayList<>(restaurantMap.values());
        }

        Log.d("YourOrderMainFragment", "Grouped orders count: " + restaurantMap.size());
        return new ArrayList<>(restaurantMap.values());
    }


    private void updateEmptyStateVisibility() {
        if (groupedOrders.isEmpty()) {
            emptyOrderText.setVisibility(View.VISIBLE);
            goShoppingText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyOrderText.setVisibility(View.GONE);
            goShoppingText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void openYourOrderFragment(YourOrderMainDomain order) {
        YourOrderFragment fragment = YourOrderFragment.newInstance(
                order.getRestaurant().getName(),
                order.getTotalPrice(),
                order.getTotalPrepTime(),
                order.getItems(),
                order
        );

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }

}
