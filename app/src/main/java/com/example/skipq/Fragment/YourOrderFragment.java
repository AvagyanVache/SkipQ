package com.example.skipq.Fragment;

import android.os.Bundle;
import android.os.CountDownTimer;
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
                listenForOrderAcceptance(orderId);
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

    private void listenForOrderAcceptance(String orderId) {
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Listen failed: " + e.getMessage());
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        Log.d("FirestoreDebug", "Snapshot null or does not exist");
                        orderCountdownTextView.setText("Order not found");
                        return;
                    }

                    String approvalStatus = snapshot.getString("approvalStatus");
                    String status = snapshot.getString("status");
                    Timestamp endTime = snapshot.getTimestamp("endTime");
                    Long totalPrepTime = snapshot.getLong("totalPrepTime");

                    Log.d("FirestoreDebug", "Order snapshot: approvalStatus=" + approvalStatus +
                            ", status=" + status + ", endTime=" + endTime + ", totalPrepTime=" + totalPrepTime);

                    // Cancel any existing timer to prevent overlap
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }

                    // Handle completed order
                    if ("done".equals(status)) {
                        orderCountdownTextView.setText("00:00");
                        TextView countdownLabel = getView().findViewById(R.id.countdownLabel);
                        if (countdownLabel != null) {
                            countdownLabel.setText("Ready! Go pick up your order now!");
                        }
                        lastApprovalStatus = approvalStatus;
                        return;
                    }

                    // If not accepted, show waiting message
                    if (!"accepted".equals(approvalStatus)) {
                        orderCountdownTextView.setText("Waiting for acceptance...");
                        lastApprovalStatus = approvalStatus;
                        return;
                    }

                    // If accepted but missing endTime or totalPrepTime, show error state
                    if (endTime == null || totalPrepTime == null) {
                        orderCountdownTextView.setText("Error: Order data incomplete");
                        Log.e("FirestoreDebug", "Missing endTime or totalPrepTime for accepted order");
                        lastApprovalStatus = approvalStatus;
                        return;
                    }

                    // Calculate remaining time using server time
                    db.collection("serverTime").document("current")
                            .set(new HashMap<String, Object>() {{
                                put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                            }})
                            .addOnSuccessListener(aVoid -> {
                                db.collection("serverTime").document("current")
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            Timestamp serverTime = doc.getTimestamp("timestamp");
                                            if (serverTime == null || !"accepted".equals(approvalStatus)) {
                                                orderCountdownTextView.setText("Waiting for acceptance...");
                                                Log.d("FirestoreDebug", "Server time null or status changed");
                                                return;
                                            }

                                            long endTimeMillis = endTime.toDate().getTime();
                                            long serverTimeMillis = serverTime.toDate().getTime();
                                            long timeRemaining = endTimeMillis - serverTimeMillis;

                                            // Fallback to totalPrepTime if timeRemaining is invalid
                                            long expectedTimeRemaining = totalPrepTime * 60 * 1000;
                                            if (timeRemaining <= 0 || timeRemaining > expectedTimeRemaining + 60000) {
                                                timeRemaining = expectedTimeRemaining;
                                                Log.d("FirestoreDebug", "Adjusted timeRemaining to: " + timeRemaining);
                                            }

                                            if (timeRemaining > 0) {
                                                orderCountdownTextView.setText("Order accepted, preparing...");
                                                startCountdown(timeRemaining);
                                                Log.d("FirestoreDebug", "Countdown started with timeRemaining: " + timeRemaining + "ms");
                                            } else {
                                                orderCountdownTextView.setText("00:00");
                                                TextView countdownLabel = getView().findViewById(R.id.countdownLabel);
                                                if (countdownLabel != null) {
                                                    countdownLabel.setText("Ready! Go pick up your order now!");
                                                }
                                                Log.d("FirestoreDebug", "Order ready, no countdown needed");
                                            }
                                            lastApprovalStatus = approvalStatus;
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e("FirestoreError", "Failed to fetch server time: " + e2.getMessage());
                                            orderCountdownTextView.setText("Error: Unable to fetch time");
                                            lastApprovalStatus = approvalStatus;
                                        });
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e("FirestoreError", "Failed to set server time: " + e2.getMessage());
                                orderCountdownTextView.setText("Error: Unable to fetch time");
                                lastApprovalStatus = approvalStatus;
                            });
                });
    }
    private void fetchOrderDetails(String orderId) {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String restaurantId = documentSnapshot.getString("restaurantId");
                        List<Map<String, Object>> orderItems = (List<Map<String, Object>>) documentSnapshot.get("items");
                        String approvalStatus = documentSnapshot.getString("approvalStatus");
                        Double totalPrice = documentSnapshot.getDouble("totalPrice");

                        if (lastApprovalStatus == null) {
                            lastApprovalStatus = approvalStatus;
                        }

                        if (restaurantId != null && orderItems != null) {
                            fetchMenuItemsForOrder(restaurantId, orderItems);
                            totalPriceTextView.setText(String.format("%.2f֏", totalPrice != null ? totalPrice : 0.0));
                            orderCountdownTextView.setText("Waiting for acceptance...");
                        } else {
                            Log.e("FirestoreDebug", "RestaurantId or items missing in order document");
                            if (cartItems == null) {
                                cartItems = new ArrayList<>();
                            }
                            yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                            recyclerView.setAdapter(yourOrderAdapter);
                            orderCountdownTextView.setText("Waiting for acceptance...");
                        }
                    } else {
                        Log.e("FirestoreDebug", "Order document does not exist");
                        orderCountdownTextView.setText("Waiting for acceptance...");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to fetch order: " + e.getMessage());
                    orderCountdownTextView.setText("Waiting for acceptance...");
                });
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

    private void startCountdown(long remainingTimeInMillis) {
        if (!isAdded() || getView() == null) {
            Log.w("YourOrderFragment", "Fragment not attached, skipping countdown");
            return;
        }

        if (remainingTimeInMillis <= 0) {
            orderCountdownTextView.setText("00:00");
            TextView countdownLabel = getView().findViewById(R.id.countdownLabel);
            if (countdownLabel != null) {
                countdownLabel.setText("Ready! Go pick up your order now!");
            }
            return;
        }

        countDownTimer = new CountDownTimer(remainingTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isOrderCanceled || !isAdded()) {
                    cancel();
                    return;
                }
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                orderCountdownTextView.setText(String.format("%02d:%02d", minutes, seconds));
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

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("userId", userId);
        orderData.put("restaurantId", order.getRestaurantId());
        orderData.put("totalPrice", order.getTotalPrice());
        orderData.put("totalPrepTime", order.getTotalPrepTime());
        orderData.put("startTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
        orderData.put("endTime", null);
        orderData.put("approvalStatus", "pendingApproval");
        orderData.put("status", "pending");

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (MenuDomain item : order.getItems()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getItemName());
            itemData.put("price", Double.parseDouble(item.getItemPrice()));
            itemData.put("prepTime", item.getPrepTime());
            itemData.put("photo", item.getItemImg());
            itemData.put("description", item.getItemDescription());
            itemData.put("item count", item.getItemCount());
            itemsList.add(itemData);
        }
        orderData.put("items", itemsList);

        db.runTransaction(transaction -> {
            transaction.set(db.collection("orders").document(order.getOrderId()), orderData);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("FirestoreDebug", "Order stored successfully with pendingApproval status");
        }).addOnFailureListener(e -> {
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