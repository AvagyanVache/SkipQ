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
    private ImageView profileIcon;
    private TextView CancelButton;
    private boolean isOrderCanceled = false;
    private TextView backButton;
    private long timeRemaining = 0;
    private ListenerRegistration orderListener;

    public static YourOrderFragment newInstance(String restaurantName,String orderId, double totalPrice, int totalPrepTime, ArrayList<MenuDomain> items, YourOrderMainDomain order, String status) {
        YourOrderFragment fragment = new YourOrderFragment();
        Bundle args = new Bundle();
        args.putString("restaurantName", restaurantName);
        args.putDouble("totalPrice", totalPrice);
        args.putInt("totalPrepTime", totalPrepTime);
        args.putParcelableArrayList("items", items);
        args.putString("status", status);
        args.putParcelable("order", order);
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
        profileIcon = view.findViewById(R.id.profileIcon);
        CancelButton = view.findViewById(R.id.CancelOrder);
        backButton = view.findViewById(R.id.backButton);



        String status = getArguments().getString("status");

        Bundle args = getArguments();
        if (args != null) {
            ArrayList<MenuDomain> items = args.getParcelableArrayList("items");
            if (items != null) {
                cartItems = items;
                yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                recyclerView.setAdapter(yourOrderAdapter);
            } else {
                Log.e("YourOrderFragment", "Items list is null");
            }
        } else {
            Log.e("YourOrderFragment", "Arguments are null");
        }

        loadOrderData();
        confirmOrder();
        fetchLatestOrder();

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        CancelButton.setOnClickListener(v -> cancelOrder());

        return view;
    }

    private void loadOrderData() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String cartItemsJson = sharedPreferences.getString("cartItems", null);
        String restaurantId = sharedPreferences.getString("restaurantId", null);
        totalPrice = sharedPreferences.getFloat("totalPrice", 0);
        prepTimeMinutes = sharedPreferences.getInt("prepTime", 0);

        if (cartItemsJson == null || restaurantId == null) {
            Log.e("YourOrderFragment", "Order data is null");
            return;
        }

        Type listType = new TypeToken<ArrayList<MenuDomain>>() {}.getType();
        cartItems = gson.fromJson(cartItemsJson, listType);

        if (cartItems == null || cartItems.isEmpty()) {
            Log.e("YourOrderFragment", "Cart items are empty");
            return;
        }

        if (cartItems != null && !cartItems.isEmpty()) {
            yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
            recyclerView.setAdapter(yourOrderAdapter);
        } else {
            Log.e("YourOrderFragment", "Cart is empty, cannot attach adapter");
        }

        totalPriceTextView.setText(String.format("%.2f֏", totalPrice));

        fetchOrderFromFirestore(restaurantId);
    }


    private void fetchOrderFromFirestore(String orderId) {
        db.collection("orders").document(orderId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to fetch order: " + error.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        Timestamp endTime = documentSnapshot.getTimestamp("endTime");

                        if (endTime != null) {
                            long endTimeMillis = endTime.toDate().getTime();
                            long currentTimeMillis = System.currentTimeMillis();
                            long timeRemaining = endTimeMillis - currentTimeMillis;

                            if (timeRemaining > 0) {
                                startCountdown(timeRemaining);
                            } else {
                                orderCountdownTextView.setText("Ready! Go pick up your order now!");
                            }
                        }
                    } else {
                        Log.e("FirestoreError", "Order document does not exist");
                    }
                });
    }



    private void fetchLatestOrder() {
        db.collection("orders")
                .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot latestOrder = queryDocumentSnapshots.getDocuments().get(0);

                        Timestamp endTime = latestOrder.getTimestamp("endTime");
                        if (endTime == null) {
                            Log.e("FirestoreDebug", "endTime is null in Firestore");
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        long timeRemaining = endTime.toDate().getTime() - currentTime;

                        if (timeRemaining > 0) {
                            startCountdown(timeRemaining);
                        } else {
                            orderCountdownTextView.setText("Ready! Go pick up your order now!");
                        }
                    } else {
                        Log.e("FirestoreDebug", "No orders found for user.");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to fetch latest order: " + e.getMessage()));
    }

    private void startCountdown(long remainingTimeInSeconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(remainingTimeInSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isOrderCanceled) {
                    cancel();
                    orderCountdownTextView.setText("");
                    return;
                }

                timeRemaining = millisUntilFinished;
                orderCountdownTextView.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60));
            }

            @Override
            public void onFinish() {
                orderCountdownTextView.setText("00:00");
                TextView countdownLabel = requireView().findViewById(R.id.countdownLabel);
                countdownLabel.setText("Ready! Go pick up your order now!");
            }
        }.start();
    }


    private void confirmOrder() {
        YourOrderMainDomain order = getArguments().getParcelable("order");
        if (order == null) {
            Log.e("YourOrderFragment", "Order data is null, cannot confirm order.");
            return;
        }

        Log.d("FirestoreDebug", "confirmOrder() called");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        long currentTimeMillis = System.currentTimeMillis(); // Current time in milliseconds
        long totalPrepTimeMillis = order.getTotalPrepTime() * 60 * 1000;
        long endTimeMillis = currentTimeMillis + totalPrepTimeMillis; // Add totalPrepTime to current time

        Timestamp startTime = new Timestamp(currentTimeMillis / 1000, (int) ((currentTimeMillis % 1000) * 1_000_000)); // Current time as startTime
        Timestamp endTime = new Timestamp(endTimeMillis / 1000, (int) ((endTimeMillis % 1000) * 1_000_000));

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("userId", userId);
        orderData.put("restaurantId", order.getRestaurantId());
        orderData.put("totalPrice", order.getTotalPrice());
        orderData.put("totalPrepTime", order.getTotalPrepTime());
        orderData.put("startTime", Timestamp.now());

        orderData.put("endTime", endTime);

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (MenuDomain item : order.getItems()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getItemName());
            itemData.put("price", Double.parseDouble(item.getItemPrice()));

            String priceString = item.getItemPrice();
            if (priceString != null && !priceString.trim().isEmpty()) {
                itemData.put("price", Double.parseDouble(priceString.trim()));
            } else {
                itemData.put("price", 0.0);
            }
            itemData.put("price", Double.parseDouble(priceString.trim()));
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


    /*
        private void cancelOrder() {

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_cancel_order, null);
            builder.setView(dialogView);

            AlertDialog alertDialog = builder.create();

            TextView backButton = dialogView.findViewById(R.id.cancelOrderDialog);
            TextView cancelButton = dialogView.findViewById(R.id.backButtonDialog);

            backButton.setOnClickListener(v -> alertDialog.dismiss());

            cancelButton.setOnClickListener(v -> {
                alertDialog.dismiss();
                clearOrderFromFirestore();
            });

            alertDialog.show();
            isOrderCanceled = true;

            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            Bundle args = getArguments();
            if (args == null) {
                Log.e("YourOrderFragment", "Arguments are null");
                return;
            }

            String orderId = args.getString("orderId");
            if (orderId == null) {
                Log.e("YourOrderFragment", "Order ID is null");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("orders").document(orderId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (cartItems != null) {
                            cartItems.clear();
                        }

                        if (yourOrderAdapter != null) {
                            yourOrderAdapter.notifyDataSetChanged();
                        }

                        totalPrice = 0.0;
                        totalPriceTextView.setText("0֏");

                        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
                        sharedPreferences.edit()
                                .clear()
                                .apply();

                        Toast.makeText(requireContext(), "Order Canceled", Toast.LENGTH_SHORT).show();
                        orderCountdownTextView.setText("Order Canceled");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show();
                        Log.e("FirestoreError", "Failed to delete order: " + e.getMessage());
                    });
        }

     */
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