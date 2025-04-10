package com.example.skipq.Fragment;

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

import com.example.skipq.Adaptor.OrderManagementAdapter;
import com.example.skipq.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderManagementFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private OrderManagementAdapter orderAdapter;
    private List<Map<String, Object>> orders;
    private FirebaseFirestore db;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_management, container, false);

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);

        db = FirebaseFirestore.getInstance();
        restaurantId = getArguments().getString("restaurantId");

        orders = new ArrayList<>();
        orderAdapter = new OrderManagementAdapter(orders, this::acceptOrder, this::declineOrder);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersRecyclerView.setAdapter(orderAdapter);

        loadPendingOrders();

        return view;
    }

    private void loadPendingOrders() {
        db.collection("orders")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("approvalStatus", "pendingApproval")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    orders.clear();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> order = doc.getData();
                            order.put("orderId", doc.getId());
                            fetchRestaurantDetails((String) order.get("restaurantId"), order);
                            orders.add(order);
                        }
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void fetchRestaurantDetails(String restaurantId, Map<String, Object> order) {
        db.collection("FoodPlaces").document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        order.put("restaurantName", documentSnapshot.getString("name"));
                        order.put("restaurantImage", documentSnapshot.getString("imageUrl"));
                        orderAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    order.put("restaurantName", "Unknown");
                    order.put("restaurantImage", "");
                    orderAdapter.notifyDataSetChanged();
                });
    }

    private void acceptOrder(String orderId) {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long totalPrepTimeMinutes = documentSnapshot.getLong("totalPrepTime"); // in minutes

                        if (totalPrepTimeMinutes != null) {
                            // Set startTime to current time at acceptance
                            long currentTimeMillis = System.currentTimeMillis();
                            Timestamp startTime = new Timestamp(currentTimeMillis / 1000, (int) ((currentTimeMillis % 1000) * 1_000_000));

                            // Calculate endTime from new startTime
                            long totalPrepTimeMillis = totalPrepTimeMinutes * 60 * 1000; // Convert minutes to milliseconds
                            long endTimeMillis = currentTimeMillis + totalPrepTimeMillis;
                            Timestamp endTime = new Timestamp(endTimeMillis / 1000, (int) ((endTimeMillis % 1000) * 1_000_000));

                            Log.d("OrderManagement", "Accepting order - orderId: " + orderId +
                                    ", totalPrepTime: " + totalPrepTimeMinutes + " minutes" +
                                    ", startTime: " + startTime.toDate() +
                                    ", endTime: " + endTime.toDate() +
                                    ", currentTimeMillis: " + currentTimeMillis);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("approvalStatus", "accepted");
                            updates.put("startTime", startTime);
                            updates.put("endTime", endTime);

                            db.collection("orders").document(orderId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("OrderManagement", "Order accepted at: " + new java.util.Date(System.currentTimeMillis()));
                                        Toast.makeText(getContext(), "Order accepted!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to accept order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(getContext(), "Missing totalPrepTime", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    private void declineOrder(String orderId) {
        db.collection("orders").document(orderId)
                .update("approvalStatus", "declined")
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Order declined!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to decline order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}