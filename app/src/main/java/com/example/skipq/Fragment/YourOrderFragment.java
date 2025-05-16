package com.example.skipq.Fragment;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
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
  //  private ImageView qrCodeImageView;
    private boolean isOrderCanceled = false;
    private ImageView backButton;
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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
      //  qrCodeImageView = view.findViewById(R.id.qrCodeImage);
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
                orderCountdownTextView.setText("00:00");

                generateQRCode(orderId);

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
    private void generateQRCode(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            Log.e("YourOrderFragment", "Order ID is null or empty, cannot generate QR code");
            return;
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(orderId, BarcodeFormat.QR_CODE, 500, 500);
            Bitmap qrBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);
            for (int x = 0; x < 500; x++) {
                for (int y = 0; y < 500; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

//            qrCodeImageView.setImageBitmap(qrBitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] qrBytes = baos.toByteArray();
            String qrBase64 = Base64.encodeToString(qrBytes, Base64.NO_WRAP);

            db.collection("orders").document(orderId)
                    .update("qrCode", qrBase64)
                    .addOnSuccessListener(aVoid -> Log.d("YourOrderFragment", "QR code saved to Firestore"))
                    .addOnFailureListener(e -> Log.e("YourOrderFragment", "Failed to save QR code: " + e.getMessage()));

        } catch (WriterException e) {
            Log.e("YourOrderFragment", "Failed to generate QR code: " + e.getMessage());
        //    qrCodeImageView.setImageResource(android.R.drawable.ic_dialog_alert);
        }
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
                            countdownLabel.setText("Ready!\nGo pick up your order now!");
                        }
                        lastApprovalStatus = approvalStatus;
                        return;
                    }

                    // If not accepted, show waiting message
                    if (!"accepted".equals(approvalStatus)) {
                        orderCountdownTextView.setText("00:00");
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
                                                orderCountdownTextView.setText("00:00");
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
                                                    countdownLabel.setText("Ready!\nGo pick up your order now!");
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
                        String qrCodeBase64 = documentSnapshot.getString("qrCode");

                        if (lastApprovalStatus == null) {
                            lastApprovalStatus = approvalStatus;
                        }
                        if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(qrCodeBase64, Base64.DEFAULT);
                                Bitmap qrBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                //qrCodeImageView.setImageBitmap(qrBitmap);
                                Log.d("YourOrderFragment", "QR code loaded from Firestore for orderId: " + orderId);
                            } catch (Exception e) {
                                Log.e("YourOrderFragment", "Failed to decode QR code: " + e.getMessage());
                            //    qrCodeImageView.setImageResource(android.R.drawable.ic_dialog_alert);
                            }

                        } else {
                            Log.w("YourOrderFragment", "No QR code found for orderId: " + orderId);
                            //qrCodeImageView.setImageResource(android.R.drawable.ic_dialog_alert);
                        }
                        if (restaurantId != null && orderItems != null) {
                            fetchMenuItemsForOrder(restaurantId, orderItems);
                            totalPriceTextView.setText(String.format("%.2f֏", totalPrice != null ? totalPrice : 0.0));
                            orderCountdownTextView.setText("00:00");
                        } else {
                            Log.e("FirestoreDebug", "RestaurantId or items missing in order document");
                            if (cartItems == null) {
                                cartItems = new ArrayList<>();
                            }
                            yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                            recyclerView.setAdapter(yourOrderAdapter);
                            orderCountdownTextView.setText("00:00");
                        }
                    } else {
                        Log.e("FirestoreDebug", "Order document does not exist");
                        orderCountdownTextView.setText("00:00");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to fetch order: " + e.getMessage());
                    orderCountdownTextView.setText("00:00");
                });
    }
    private void fetchMenuItemsForOrder(String restaurantId, List<Map<String, Object>> orderItems) {
        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .document("DefaultMenu")
                .collection("Items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<MenuDomain> menuList = new ArrayList<>();
                    Map<String, DocumentSnapshot> menuItemsMap = new HashMap<>();

                    // Build a map of menu items for efficient lookup
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Boolean isAvailable = document.getBoolean("Available");
                        // Include only available items (or where Available is null)
                        if (isAvailable == null || isAvailable) {
                            String menuItemName = document.getString("Item Name");
                            if (menuItemName != null) {
                                menuItemsMap.put(menuItemName.trim().toLowerCase(), document);
                                Log.d("FirestoreDebug", "Menu item cached: " + menuItemName);
                            }
                        } else {
                            Log.d("FirestoreDebug", "Skipped unavailable item: " + document.getString("Item Name"));
                        }
                    }

                    // Process each order item
                    for (Map<String, Object> orderItem : orderItems) {
                        String orderItemName = (String) orderItem.get("name");
                        if (orderItemName == null || orderItemName.trim().isEmpty()) {
                            Log.w("FirestoreDebug", "Skipping order item with null or empty name");
                            continue;
                        }

                        String normalizedOrderItemName = orderItemName.trim().toLowerCase();
                        DocumentSnapshot menuDocSnapshot = menuItemsMap.get(normalizedOrderItemName);

                        MenuDomain menuItem = new MenuDomain();
                        int itemCount = 1;

                        // Set item count from order data
                        if (orderItem.containsKey("item count")) {
                            try {
                                itemCount = ((Long) orderItem.get("item count")).intValue();
                            } catch (ClassCastException e) {
                                Log.w("FirestoreDebug", "Invalid item count format for: " + orderItemName);
                            }
                        }

                        if (menuDocSnapshot != null && menuDocSnapshot.exists()) {
                            // Populate MenuDomain with Firestore data
                            menuItem.setItemName(menuDocSnapshot.getString("Item Name"));
                            menuItem.setItemDescription(menuDocSnapshot.getString("Item Description") != null ?
                                    menuDocSnapshot.getString("Item Description") : "No description");
                            menuItem.setItemPrice(menuDocSnapshot.getString("Item Price") != null ?
                                    menuDocSnapshot.getString("Item Price") : "0");
                            menuItem.setItemImg(menuDocSnapshot.getString("Item Img") != null ?
                                    menuDocSnapshot.getString("Item Img") : "");
                            menuItem.setPrepTime(menuDocSnapshot.contains("Prep Time") ?
                                    menuDocSnapshot.getLong("Prep Time").intValue() : 0);
                            menuItem.setRestaurantId(restaurantId);
                            menuItem.setAvailable(true); // Explicitly set as available
                        } else {
                            // Fallback if no matching menu item is found
                            Log.w("FirestoreDebug", "No menu match for: " + orderItemName);
                            menuItem.setItemName(orderItemName);
                            menuItem.setItemDescription("No description available");
                            menuItem.setItemPrice(orderItem.containsKey("price") ?
                                    String.valueOf(orderItem.get("price")) : "0");
                            menuItem.setItemImg(orderItem.containsKey("photo") ?
                                    (String) orderItem.get("photo") : "");
                            menuItem.setPrepTime(orderItem.containsKey("prepTime") ?
                                    ((Long) orderItem.get("prepTime")).intValue() : 0);
                            menuItem.setRestaurantId(restaurantId);
                            menuItem.setAvailable(true);
                        }

                        menuItem.setItemCount(itemCount);
                        menuList.add(menuItem);
                        Log.d("FirestoreDebug", "Added item to order: " + orderItemName + ", Count: " + itemCount);
                    }

                    if (!menuList.isEmpty()) {
                        cartItems = menuList;
                        yourOrderAdapter = new YourOrderAdaptor(requireContext(), cartItems);
                        recyclerView.setAdapter(yourOrderAdapter);
                        yourOrderAdapter.notifyDataSetChanged();
                        Log.d("FirestoreDebug", "Updated UI with " + menuList.size() + " items");
                    } else {
                        Log.w("FirestoreDebug", "No items to display");
                        orderCountdownTextView.setText("No items found in order");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to fetch menu items: " + e.getMessage());
                    orderCountdownTextView.setText("Error loading order items");
                });
    }
    private String generateQRCodeBase64(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            Log.e("YourOrderFragment", "Order ID is null or empty, cannot generate QR code");
            return null;
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(orderId, BarcodeFormat.QR_CODE, 500, 500);
            Bitmap qrBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);
            for (int x = 0; x < 500; x++) {
                for (int y = 0; y < 500; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Convert bitmap to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] qrBytes = baos.toByteArray();
            String qrCodeBase64 = Base64.encodeToString(qrBytes, Base64.DEFAULT);
            Log.d("YourOrderFragment", "QR code Base64 generated for orderId: " + orderId);
            return qrCodeBase64;
        } catch (WriterException e) {
            Log.e("YourOrderFragment", "Failed to generate QR code for orderId: " + orderId + ", error: " + e.getMessage());
            return null;
        }
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
                countdownLabel.setText("Ready!\nGo pick up your order now!");
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
                    countdownLabel.setText("Ready!\nGo pick up your order now!");
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

        String qrCodeBase64 = generateQRCodeBase64(order.getOrderId());
        if (qrCodeBase64 != null) {
            orderData.put("qrCode", qrCodeBase64);
            Log.d("YourOrderFragment", "QR code generated and added to order data for orderId: " + order.getOrderId());
        } else {
            Log.e("YourOrderFragment", "Failed to generate QR code for orderId: " + order.getOrderId());
        }

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