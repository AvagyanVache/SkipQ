package com.example.skipq;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.YourOrderAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.google.common.reflect.TypeToken;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Type;
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
    private TextView CancelButton;
    private boolean isOrderCanceled = false;
    private TextView backButton;
    private long timeRemaining = 0;
    private ListenerRegistration orderListener;

    public static YourOrderFragment newInstance(String restaurantName, String orderId, double totalPrice, int totalPrepTime, ArrayList<MenuDomain> items, YourOrderMainDomain order, String status, boolean isNewOrder) {
        YourOrderFragment fragment = new YourOrderFragment();
        Bundle args = new Bundle();
        args.putString("restaurantName", restaurantName);
        args.putString("orderId", orderId);
        args.putDouble("totalPrice", totalPrice);
        args.putInt("totalPrepTime", totalPrepTime);
        args.putParcelableArrayList("items", items); // Ensure items is ArrayList for Parcelable
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_your_order, container, false);
        recyclerView = view.findViewById(R.id.your_order_recyclerview);
        totalPriceTextView = view.findViewById(R.id.orderTotal);
        orderCountdownTextView = view.findViewById(R.id.OrderCountdown);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        CancelButton = view.findViewById(R.id.CancelOrder);
        backButton = view.findViewById(R.id.backButton);


        Bundle args = getArguments();
        if (args != null) {
            String orderId = args.getString("orderId");
            YourOrderMainDomain order = args.getParcelable("order");
            String status = args.getString("status");

            if (order != null) {
                cartItems = order.getItems();
                Log.d("YourOrderFragment", "Initial cartItems: " + new Gson().toJson(cartItems));
                yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                recyclerView.setAdapter(yourOrderAdapter);
                totalPriceTextView.setText(String.format("%.2f֏", order.getTotalPrice()));

                Timestamp endTime = order.getEndTime();
                if (endTime != null) {
                    long endTimeMillis = endTime.toDate().getTime();
                    long currentTimeMillis = System.currentTimeMillis();
                    timeRemaining = endTimeMillis - currentTimeMillis;
                    if ("done".equals(status)) {
                        orderCountdownTextView.setText("00:00");
                        CancelButton.setVisibility(View.GONE);
                    } else if ("pending".equals(status) && timeRemaining > 0) {
                        if (countDownTimer == null) {
                            startCountdown(timeRemaining);
                        }
                        CancelButton.setVisibility(View.VISIBLE);
                    } else {
                        orderCountdownTextView.setText("00:00");
                        CancelButton.setVisibility(View.GONE);
                    }
                } else {
                    orderCountdownTextView.setText("Order status unavailable");
                    CancelButton.setVisibility(View.GONE);
                }

                boolean isNewOrder = args.getBoolean("isNewOrder", false);
                if (isNewOrder) {
                    confirmOrder(order);
                }
            } else {
                Log.e("YourOrderFragment", "Order is null");
            }

            // Fetch updated data from Firestore if orderId is provided
            if (orderId != null) {
                fetchOrderDetails(orderId);
            } else {
                Log.e("YourOrderFragment", "Order ID is null");
            }
        } else {
            Log.e("YourOrderFragment", "Arguments are null");
        }


        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        CancelButton.setOnClickListener(v -> cancelOrder());

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
                        Double totalPrice = documentSnapshot.getDouble("totalPrice");

                        if (restaurantId != null && orderItems != null) {
                            fetchMenuItemsForOrder(restaurantId, orderItems);
                            totalPriceTextView.setText(String.format("%.2f֏", totalPrice != null ? totalPrice : 0.0));

                            if (endTime != null) {
                                long endTimeMillis = endTime.toDate().getTime();
                                long currentTimeMillis = System.currentTimeMillis();
                                timeRemaining = endTimeMillis - currentTimeMillis;

                                if ("done".equals(status)) {
                                    orderCountdownTextView.setText("00:00");
                                    CancelButton.setVisibility(View.GONE);
                                } else if ("pending".equals(status) && timeRemaining > 0) {
                                    startCountdown(timeRemaining);
                                    CancelButton.setVisibility(View.VISIBLE);
                                } else {
                                    orderCountdownTextView.setText("00:00");
                                    CancelButton.setVisibility(View.GONE);
                                }
                            } else {
                                orderCountdownTextView.setText("Time unavailable");
                                CancelButton.setVisibility(View.GONE);
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

                                    if (menuDocSnapshot != null && menuDocSnapshot.exists()) {
                                        itemDescription = menuDocSnapshot.getString("Item Description") != null ? menuDocSnapshot.getString("Item Description") : itemDescription;
                                        itemImg = menuDocSnapshot.getString("Item Img") != null ? menuDocSnapshot.getString("Item Img") : itemImg;
                                        itemPrice = menuDocSnapshot.getString("Item Price") != null ? menuDocSnapshot.getString("Item Price") : itemPrice;
                                        prepTime = menuDocSnapshot.contains("Prep Time") ? menuDocSnapshot.getLong("Prep Time").intValue() : 0;
                                        Log.d("FirestoreDebug", "Matched: " + orderItemName);
                                    } else {
                                        Log.w("FirestoreDebug", "No menu match for: " + orderItemName);
                                    }

                                    menuList.add(new MenuDomain(orderItemName, itemDescription, itemImg, itemPrice, prepTime));
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
                    return; // Avoid accessing views if fragment is detached
                }
                orderCountdownTextView.setText("00:00");
                TextView countdownLabel = getView().findViewById(R.id.countdownLabel);
                if (countdownLabel != null) {
                    countdownLabel.setText("Ready! Go pick up your order now!");
                }
                CancelButton.setVisibility(View.GONE);
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

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (MenuDomain item : order.getItems()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getItemName());
            itemData.put("price", Double.parseDouble(item.getItemPrice()));
            itemData.put("prepTime", item.getPrepTime());
            itemData.put("photo", item.getItemImg());
            itemData.put("description", item.getItemDescription()); // Add description to order data
            itemsList.add(itemData);
        }
        orderData.put("items", itemsList);

        db.collection("orders").document(order.getOrderId())
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreDebug", "Order stored successfully with correct timestamp.");
                    startCountdown(totalPrepTimeMillis);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to store order: " + e.getMessage());
                });
    }

    private void cancelOrder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_cancel_order, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();

        TextView backButton = dialogView.findViewById(R.id.cancelOrderDialog);
        TextView cancelButton = dialogView.findViewById(R.id.backButtonDialog);

        backButton.setOnClickListener(v -> {
            alertDialog.dismiss();
            if (timeRemaining > 0) {
                startCountdown(timeRemaining / 1000);
            }
        });

        cancelButton.setOnClickListener(v -> {
            alertDialog.dismiss();
            deleteOrderFromFirestore();
        });

        alertDialog.show();
        isOrderCanceled = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void deleteOrderFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderId = getArguments().getString("orderId");
        if (orderId == null) {
            Log.e("YourOrderFragment", "Order ID is null");
            Toast.makeText(requireContext(), "Order ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Order successfully deleted");

                    if (cartItems != null) {
                        cartItems.clear();
                    }
                    if (yourOrderAdapter != null) {
                        yourOrderAdapter.notifyDataSetChanged();
                    }

                    totalPrice = 0;
                    timeRemaining = 0;
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }

                    totalPriceTextView.setText("0֏");
                    orderCountdownTextView.setText("Order Canceled");

                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
                    sharedPreferences.edit().clear().apply();

                    Toast.makeText(requireContext(), "Order Canceled", Toast.LENGTH_SHORT).show();

                    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.frame_layout, new YourOrderMainFragment());
                    transaction.addToBackStack(null);
                    transaction.commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", "Failed to delete order: " + e.getMessage());
                });
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (countDownTimer != null && isOrderCanceled) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}