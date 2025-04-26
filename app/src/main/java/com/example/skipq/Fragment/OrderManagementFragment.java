package com.example.skipq.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.DocumentSnapshot;
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
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(db.collection("orders").document(orderId));
            if (!snapshot.exists()) {
                throw new IllegalStateException("Order does not exist");
            }

            Long totalPrepTimeMinutes = snapshot.getLong("totalPrepTime");
            if (totalPrepTimeMinutes == null) {
                throw new IllegalStateException("Total prep time is missing");
            }

            // Set approvalStatus and startTime in the transaction
            Map<String, Object> updates = new HashMap<>();
            updates.put("approvalStatus", "accepted");
            updates.put("startTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
            // Do not set endTime here, as startTime is not yet resolved
            transaction.update(db.collection("orders").document(orderId), updates);
            return totalPrepTimeMinutes; // Pass totalPrepTimeMinutes to the success handler
        }).addOnSuccessListener(totalPrepTimeMinutes -> {
            // Fetch the updated order document to get the resolved startTime
            db.collection("orders").document(orderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Timestamp startTime = documentSnapshot.getTimestamp("startTime");
                            if (startTime != null) {
                                // Calculate endTime based on startTime + totalPrepTime
                                long endTimeSeconds = startTime.getSeconds() + (totalPrepTimeMinutes * 60);
                                Timestamp endTime = new Timestamp(endTimeSeconds, 0);

                                // Update the order with the calculated endTime
                                db.collection("orders").document(orderId)
                                        .update("endTime", endTime)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Order accepted!", Toast.LENGTH_SHORT).show();
                                            Log.d("OrderManagement", "Order " + orderId + " accepted and endTime set");
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Failed to set end time: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("OrderManagement", "Failed to set end time: " + e.getMessage());
                                        });
                            } else {
                                Toast.makeText(getContext(), "Failed to retrieve start time", Toast.LENGTH_SHORT).show();
                                Log.e("OrderManagement", "startTime is null for order " + orderId);
                            }
                        } else {
                            Toast.makeText(getContext(), "Order not found", Toast.LENGTH_SHORT).show();
                            Log.e("OrderManagement", "Order document does not exist for " + orderId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to fetch order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("OrderManagement", "Failed to fetch order: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to accept order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("OrderManagement", "Failed to accept order: " + e.getMessage());
        });
    }

    private void declineOrder(String orderId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Reason for Declining Order");

        String[] declineReasons = {
                "Out of stock",
                "Kitchen overload",
                "Order not feasible",
                "Other"
        };

        builder.setItems(declineReasons, (dialog, which) -> {
            String selectedReason = declineReasons[which];
            Log.d("OrderManagement", "Declining order " + orderId + " with reason: " + selectedReason);

            Map<String, Object> updates = new HashMap<>();
            updates.put("approvalStatus", "declined");
            updates.put("declineReason", selectedReason);

            db.collection("orders").document(orderId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("OrderManagement", "Order " + orderId + " updated to declined with reason: " + selectedReason);
                        new Handler().postDelayed(() -> {
                            db.collection("orders").document(orderId)
                                    .delete()
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(getContext(), "Order declined and removed!", Toast.LENGTH_SHORT).show();
                                        Log.d("OrderManagement", "Order " + orderId + " deleted successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to delete order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("OrderManagement", "Failed to delete order: " + e.getMessage());
                                    });
                        }, 2000);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to decline order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("OrderManagement", "Failed to decline order: " + e.getMessage());
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}