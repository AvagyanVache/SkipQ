package com.example.skipq.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.YourOrderAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.example.skipq.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class YourOrderFragment extends Fragment {
    private static final String ARG_ORDER = "order";

    private RecyclerView recyclerView;
    private YourOrderAdaptor yourOrderAdapter;
    private TextView totalPriceTextView, orderCountdownTextView;
    private CountDownTimer countDownTimer;
    private int prepTimeMinutes;
    private ListenerRegistration ordersListener;

    private ArrayList<MenuDomain> cartItems;
    private double totalPrice;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private boolean isOrderCanceled = false;
    private TextView backButton;
    private long timeRemaining = 0;
    private ListenerRegistration orderListener;
    private String lastApprovalStatus = null; // Track the previous status
    private boolean isFirstSnapshot = true;   // Flag for initial snapshot

    public static YourOrderFragment newInstance(String restaurantName, String orderId, double totalPrice,
                                                int totalPrepTime, ArrayList<MenuDomain> items, YourOrderMainDomain order,
                                                String status, boolean isNewOrder) {
        YourOrderFragment fragment = new YourOrderFragment();
        Bundle args = new Bundle();
        args.putString("restaurantName", restaurantName);
        args.putString("orderId", orderId);
        args.putDouble("totalPrice", totalPrice);
        args.putInt("totalPrepTime", totalPrepTime);
        args.putParcelableArrayList("items", items);
        args.putString("status", status);
        args.putParcelable("order", order);
        args.putBoolean("isNewOrder", isNewOrder);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        setRetainInstance(true);
        Bundle args = getArguments();
        if (args != null) {
            String orderId = args.getString("orderId");
            if (orderId != null) {
                listenForOrderAcceptance(orderId); // Start listening immediately
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_your_order, container, false);
        recyclerView = view.findViewById(R.id.your_order_recyclerview);
        totalPriceTextView = view.findViewById(R.id.orderTotal);
        orderCountdownTextView = view.findViewById(R.id.OrderCountdown);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        backButton = view.findViewById(R.id.backButton);

        // Ensure countdown is null initially
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        Bundle args = getArguments();
        if (args != null) {
            String orderId = args.getString("orderId");
            YourOrderMainDomain order = args.getParcelable("order");

            if (order != null) {
                cartItems = order.getItems();
                Log.d("YourOrderFragment", "Initial cartItems: " + new Gson().toJson(cartItems));
                yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                recyclerView.setAdapter(yourOrderAdapter);
                totalPriceTextView.setText(String.format("%.2f֏", order.getTotalPrice()));
                orderCountdownTextView.setText("Waiting for acceptance...");

                boolean isNewOrder = args.getBoolean("isNewOrder", false);
                if (isNewOrder) {
                    confirmOrder(order);
                }
            } else {
                Log.e("YourOrderFragment", "Order is null");
            }

            if (orderId != null) {
                fetchOrderDetails(orderId);
            } else {
                Log.e("YourOrderFragment", "Order ID is null");
            }
        } else {
            Log.e("YourOrderFragment", "Arguments are null");
        }

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        return view;
    }

    private void fetchOrderDetails(String orderId) {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String restaurantId = documentSnapshot.getString("restaurantId");
                        List<Map<String, Object>> orderItems = (List<Map<String, Object>>) documentSnapshot.get("items");
                        Timestamp endTime = documentSnapshot.getTimestamp("endTime");
                        String status = documentSnapshot.getString("status");
                        String approvalStatus = documentSnapshot.getString("approvalStatus");
                        Double totalPrice = documentSnapshot.getDouble("totalPrice");

                        // Set initial lastApprovalStatus if not set yet
                        if (lastApprovalStatus == null) {
                            lastApprovalStatus = approvalStatus;
                        }

                        if (restaurantId != null && orderItems != null) {
                            fetchMenuItemsForOrder(restaurantId, orderItems);
                            totalPriceTextView.setText(String.format("%.2f֏", totalPrice != null ? totalPrice : 0.0));

                            if (endTime != null) {
                                long endTimeMillis = endTime.toDate().getTime();
                                long currentTimeMillis = System.currentTimeMillis();
                                timeRemaining = endTimeMillis - currentTimeMillis;

                                if ("done".equals(status)) {
                                    orderCountdownTextView.setText("00:00");
                                } else {
                                    orderCountdownTextView.setText("Waiting for acceptance..."); // Default state
                                }
                            } else {
                                orderCountdownTextView.setText("Time unavailable");
                            }
                        } else {
                            Log.e("FirestoreDebug", "RestaurantId or items missing in order document");
                            if (cartItems == null) {
                                cartItems = new ArrayList<>();
                            }
                            yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                            recyclerView.setAdapter(yourOrderAdapter);
                        }
                    } else {
                        Log.e("FirestoreDebug", "Order document does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to fetch order: " + e.getMessage()));
    }

    private void fetchMenuItemsForOrder(String restaurantId, List<Map<String, Object>> orderItems) {
        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .get()
                .addOnSuccessListener(menuSnapshots -> {
                    if (menuSnapshots.isEmpty()) {
                        Log.w("FirestoreDebug", "No menu found for restaurant: " + restaurantId);
                        return;
                    }

                    QueryDocumentSnapshot menuDoc = null;
                    for (QueryDocumentSnapshot doc : menuSnapshots) {
                        menuDoc = doc;
                        break;
                    }

                    if (menuDoc == null) {
                        Log.w("FirestoreDebug", "No valid menu document found for restaurant: " + restaurantId);
                        return;
                    }

                    Log.d("FirestoreDebug", "Fetching menu from: FoodPlaces/" + restaurantId + "/Menu/" + menuDoc.getId());

                    db.collection("FoodPlaces")
                            .document(restaurantId)
                            .collection("Menu")
                            .document(menuDoc.getId())
                            .collection("Items")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                ArrayList<MenuDomain> menuList = new ArrayList<>();
                                Map<String, DocumentSnapshot> menuItemsMap = new HashMap<>();

                                for (DocumentSnapshot document : queryDocumentSnapshots) {
                                    String menuItemName = document.getString("Item Name");
                                    if (menuItemName != null) {
                                        menuItemsMap.put(menuItemName.trim().toLowerCase(), document);
                                        Log.d("FirestoreDebug", "Menu item: " + menuItemName);
                                    }
                                }

                                for (Map<String, Object> orderItem : orderItems) {
                                    String orderItemName = (String) orderItem.get("name");
                                    if (orderItemName == null || orderItemName.trim().isEmpty()) {
                                        Log.w("FirestoreDebug", "Skipping order item with null or empty name");
                                        continue;
                                    }

                                    String normalizedOrderItemName = orderItemName.trim().toLowerCase();
                                    DocumentSnapshot menuDocSnapshot = menuItemsMap.get(normalizedOrderItemName);

                                    String itemDescription = "No description available";
                                    String itemImg = "";
                                    String itemPrice = "0";
                                    int prepTime = 0;
                                    int itemCount = 1;

                                    if (orderItem.containsKey("item count")) {
                                        itemCount = ((Long) orderItem.get("item count")).intValue();
                                        Log.d("FirestoreDebug", "Item: " + orderItemName + ", Count: " + itemCount);
                                    } else {
                                        Log.w("FirestoreDebug", "No item count found for: " + orderItemName);
                                    }

                                    if (menuDocSnapshot != null && menuDocSnapshot.exists()) {
                                        itemDescription = menuDocSnapshot.getString("Item Description") != null ?
                                                menuDocSnapshot.getString("Item Description") : itemDescription;
                                        itemImg = menuDocSnapshot.getString("Item Img") != null ?
                                                menuDocSnapshot.getString("Item Img") : itemImg;
                                        itemPrice = menuDocSnapshot.getString("Item Price") != null ?
                                                menuDocSnapshot.getString("Item Price") : itemPrice;
                                        prepTime = menuDocSnapshot.contains("Prep Time") ?
                                                menuDocSnapshot.getLong("Prep Time").intValue() : 0;
                                        Log.d("FirestoreDebug", "Matched: " + orderItemName);
                                    } else {
                                        Log.w("FirestoreDebug", "No menu match for: " + orderItemName);
                                    }

                                    menuList.add(new MenuDomain(orderItemName, itemDescription, itemImg, itemPrice, prepTime, itemCount));
                                }

                                if (!menuList.isEmpty()) {
                                    cartItems = menuList;
                                    yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                                    recyclerView.setAdapter(yourOrderAdapter);
                                    yourOrderAdapter.notifyDataSetChanged();
                                    Log.d("FirestoreDebug", "Updated UI with " + menuList.size() + " items");
                                } else {
                                    Log.w("FirestoreDebug", "No items to display");
                                }
                            })
                            .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to fetch menu items: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to fetch menu documents: " + e.getMessage()));
    }

    private void listenForOrderAcceptance(String orderId) {
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Listen failed: " + e.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String approvalStatus = snapshot.getString("approvalStatus");
                        Timestamp endTime = snapshot.getTimestamp("endTime");
                        Timestamp startTime = snapshot.getTimestamp("startTime");
                        String status = snapshot.getString("status");
                        Long totalPrepTime = snapshot.getLong("totalPrepTime");

                        long snapshotTimeMillis = System.currentTimeMillis();
                        Log.d("YourOrderFragment", "Snapshot received at: " + new java.util.Date(snapshotTimeMillis) +
                                ", approvalStatus: " + approvalStatus +
                                ", status: " + status +
                                ", startTime: " + (startTime != null ? startTime.toDate() : "null") +
                                ", endTime: " + (endTime != null ? endTime.toDate() : "null") +
                                ", totalPrepTime: " + totalPrepTime + " minutes");

                        if (endTime != null) {
                            long endTimeMillis = endTime.toDate().getTime();
                            long currentTimeMillis = System.currentTimeMillis();
                            timeRemaining = endTimeMillis - currentTimeMillis;

                            Log.d("YourOrderFragment", "Calculated - currentTime: " + new java.util.Date(currentTimeMillis) +
                                    ", timeRemaining: " + (timeRemaining / 1000) + " seconds");

                            if ("done".equals(status) || timeRemaining <= 0) {
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                    countDownTimer = null;
                                }
                                orderCountdownTextView.setText("00:00");
                            } else if ("accepted".equals(approvalStatus) && timeRemaining > 0) {
                                if (countDownTimer == null) {
                                    Log.d("YourOrderFragment", "Starting countdown with " + (timeRemaining / 1000) + " seconds");
                                    orderCountdownTextView.setText("Order accepted, preparing...");
                                    startCountdown(timeRemaining);
                                }
                            } else {
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                    countDownTimer = null;
                                }
                                orderCountdownTextView.setText("Waiting for acceptance...");
                            }
                        } else {
                            orderCountdownTextView.setText("Waiting for acceptance...");
                            Log.w("YourOrderFragment", "endTime is null");
                        }
                    }
                });
    }


    private void startCountdown(long remainingTimeInMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(remainingTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isOrderCanceled || !isAdded()) {
                    cancel();
                    return;
                }
                timeRemaining = millisUntilFinished;
                orderCountdownTextView.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60));
            }

            @Override
            public void onFinish() {
                if (!isAdded() || getView() == null) {
                    return;
                }
                orderCountdownTextView.setText("00:00");
                TextView countdownLabel = getView().findViewById(R.id.countdownLabel);
                if (countdownLabel != null) {
                    countdownLabel.setText("Ready! Go pick up your order now!");
                }
            }
        }.start();
    }

    private void confirmOrder(YourOrderMainDomain order) {
        if (order == null) {
            Log.e("YourOrderFragment", "Order data is null, cannot confirm order.");
            return;
        }

        Log.d("FirestoreDebug", "confirmOrder() called");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        long currentTimeMillis = System.currentTimeMillis();
        long totalPrepTimeMillis = order.getTotalPrepTime() * 60 * 1000;
        long endTimeMillis = currentTimeMillis + totalPrepTimeMillis;

        Timestamp startTime = new Timestamp(currentTimeMillis / 1000, (int) ((currentTimeMillis % 1000) * 1_000_000));
        Timestamp endTime = new Timestamp(endTimeMillis / 1000, (int) ((endTimeMillis % 1000) * 1_000_000));

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("userId", userId);
        orderData.put("restaurantId", order.getRestaurantId());
        orderData.put("totalPrice", order.getTotalPrice());
        orderData.put("totalPrepTime", order.getTotalPrepTime());
        orderData.put("startTime", startTime);
        orderData.put("endTime", endTime);
        orderData.put("approvalStatus", "pending"); // Initial status
        orderData.put("status", "pending");

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (MenuDomain item : order.getItems()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getItemName());
            itemData.put("price", Double.parseDouble(item.getItemPrice()));
            itemData.put("prepTime", item.getPrepTime());
            itemData.put("photo", item.getItemImg());
            itemData.put("description", item.getItemDescription());
            itemsList.add(itemData);
        }
        orderData.put("items", itemsList);

        db.collection("orders").document(order.getOrderId())
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreDebug", "Order stored successfully with pending status");
                    // Verify initial status
                    db.collection("orders").document(order.getOrderId()).get()
                            .addOnSuccessListener(doc -> Log.d("YourOrderFragment",
                                    "Initial status after save: " + doc.getString("approvalStatus")));
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to store order: " + e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}