package com.example.skipq.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.Activity.HomeActivity;
import com.example.skipq.Adaptor.YourOrderMainAdaptor;
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.example.skipq.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class YourOrderMainFragment extends Fragment {
    private RecyclerView recyclerView;
    private YourOrderMainAdaptor yourOrdersAdapter;
    private TextView emptyOrderText, goShoppingText, currentOrdersText, orderHistoryText;
    private ArrayList<YourOrderMainDomain> groupedOrders;
    private FirebaseFirestore firestore;
    private String userId;
    private ImageView profileIcon;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;
    private ListenerRegistration pendingApprovalListener;
    private ListenerRegistration declinedOrdersListener;
    private AlertDialog waitingDialog;
    private TextView waitingTimerText;
    private Handler timerHandler;
    private long startTime;
    private final Set<String> shownDeclineDialogs = new HashSet<>();
    private String currentPendingOrderId;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.your_order_main_fragment, container, false);
        setRetainInstance(true);
        recyclerView = view.findViewById(R.id.YourOrderMainRecycleView);
        emptyOrderText = view.findViewById(R.id.OrderEmpty);
        goShoppingText = view.findViewById(R.id.GoShopping);
        currentOrdersText = view.findViewById(R.id.CurrentOrders);
        orderHistoryText = view.findViewById(R.id.OrderHistory);
        profileIcon = view.findViewById(R.id.profileIcon);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            setupProfileListener(currentUser);
        }

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        firestore = FirebaseFirestore.getInstance();
        groupedOrders = new ArrayList<>();
        timerHandler = new Handler();

        yourOrdersAdapter = new YourOrderMainAdaptor(requireContext(), groupedOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(yourOrdersAdapter);

        loadOrdersFromFirestore(true);

        startOrderCountdownWatcher();
        checkForPendingApprovalOrders();
        checkForDeclinedOrders();
        updateEmptyStateVisibility();

        currentOrdersText.setOnClickListener(v -> {
            loadOrdersFromFirestore(true);
            updateTabSelection(true);
        });

        orderHistoryText.setOnClickListener(v -> {
            loadOrdersFromFirestore(false);
            updateTabSelection(false);
        });

        goShoppingText.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.putExtra("FRAGMENT_TO_LOAD", "HOME");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        yourOrdersAdapter.setOnItemClickListener(position -> {
            if (!groupedOrders.isEmpty() && position >= 0 && position < groupedOrders.size()) {
                YourOrderMainDomain selectedOrder = groupedOrders.get(position);
                openYourOrderFragment(selectedOrder);
            } else {
                Log.e("YourOrderMainFragment", "Invalid position or empty list");
            }
        });

        return view;
    }

    private void loadOrdersFromFirestore(boolean isCurrentOrders) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e("FirestoreError", "User is not logged in");
            return;
        }
        userId = user.getUid();

        long currentTime = System.currentTimeMillis() / 1000;
        Timestamp currentTimestamp = new Timestamp(currentTime, 0);

        Query query = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("approvalStatus", "accepted"); // Only accepted orders

        if (isCurrentOrders) {
            query = query.whereGreaterThan("endTime", currentTimestamp); // Pending accepted orders
        } else {
            query = query.whereLessThanOrEqualTo("endTime", currentTimestamp); // Done accepted orders
        }

        query.orderBy("endTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    groupedOrders.clear(); // Clear previous orders

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Map<String, YourOrderMainDomain> uniqueOrders = new HashMap<>();
                        int totalOrders = snapshots.size();
                        final int[] fetchedCount = {0};

                        for (QueryDocumentSnapshot document : snapshots) {
                            String orderId = document.getId();
                            if (uniqueOrders.containsKey(orderId)) continue;

                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            order.setStartTime(document.getTimestamp("startTime"));
                            order.setEndTime(document.getTimestamp("endTime"));
                            order.setOrderId(document.getId());

                            long endTimeSeconds = order.getEndTime() != null ? order.getEndTime().getSeconds() : 0;
                            String status = (currentTime < endTimeSeconds) ? "pending" : "done";
                            order.setStatus(status);

                            uniqueOrders.put(orderId, order);

                            String restaurantId = document.getString("restaurantId");
                            if (restaurantId != null) {
                                fetchRestaurantDetails(restaurantId, order, () -> {
                                    fetchedCount[0]++;
                                    if (fetchedCount[0] == totalOrders) {
                                        groupedOrders.clear();
                                        groupedOrders.addAll(uniqueOrders.values());
                                        yourOrdersAdapter.notifyDataSetChanged();
                                        updateEmptyStateVisibility();
                                    }
                                });
                            } else {
                                order.setRestaurant(new RestaurantDomain("Unknown", ""));
                                fetchedCount[0]++;
                                if (fetchedCount[0] == totalOrders) {
                                    groupedOrders.clear();
                                    groupedOrders.addAll(uniqueOrders.values());
                                    yourOrdersAdapter.notifyDataSetChanged();
                                    updateEmptyStateVisibility();
                                }
                            }
                        }
                    } else {
                        yourOrdersAdapter.notifyDataSetChanged();
                        updateEmptyStateVisibility();
                    }
                });
    }

    private void checkForPendingApprovalOrders() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Query pendingQuery = firestore.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("approvalStatus", "pendingApproval");

        pendingApprovalListener = pendingQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e("FirestoreError", "Failed to check pending approval orders: " + error.getMessage());
                return;
            }

            if (snapshots != null && !snapshots.isEmpty() && isAdded()) {
                for (QueryDocumentSnapshot document : snapshots) {
                    String orderId = document.getId();
                    Log.d("YourOrderMainFragment", "Pending order detected: " + orderId);
                    showWaitingDialog(orderId);
                    break; // Show waiting dialog for the first pending order
                }
            } else if (waitingDialog != null && waitingDialog.isShowing()) {
                waitingDialog.dismiss();
                Log.d("YourOrderMainFragment", "No pending orders, dismissing waiting dialog");
            }
        });
    }

    private void checkForDeclinedOrders() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Query declinedQuery = firestore.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("approvalStatus", "declined");

        declinedOrdersListener = declinedQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e("FirestoreError", "Failed to check declined orders: " + error.getMessage());
                return;
            }

            if (snapshots != null && !snapshots.isEmpty() && isAdded()) {
                for (QueryDocumentSnapshot document : snapshots) {
                    String orderId = document.getId();
                    // Only process the decline if it matches the current pending order
                    if (!orderId.equals(currentPendingOrderId)) {
                        Log.d("YourOrderMainFragment", "Ignoring decline for non-current order: " + orderId);
                        continue;
                    }
                    // Skip if we've already shown a dialog for this order
                    if (shownDeclineDialogs.contains(orderId)) {
                        Log.d("YourOrderMainFragment", "Skipping duplicate decline dialog for order: " + orderId);
                        continue;
                    }

                    String declineReason = document.getString("declineReason");
                    if (declineReason == null || declineReason.isEmpty()) {
                        declineReason = "No reason provided";
                    }
                    Log.d("YourOrderMainFragment", "Declined order detected: " + orderId + ", reason: " + declineReason);

                    shownDeclineDialogs.add(orderId); // Mark this order as shown
                    showDeclineDialog(orderId, declineReason);
                    currentPendingOrderId = null; // Clear current order after decline
                    break; // Show only one decline dialog per snapshot
                }
            }
        });
    }

    private void showWaitingDialog(String orderId) {
        if (!isAdded()) {
            Log.w("YourOrderMainFragment", "Fragment not attached, skipping dialog");
            return;
        }

        if (waitingDialog != null && waitingDialog.isShowing()) {
            Log.d("YourOrderMainFragment", "Waiting dialog already showing for order: " + orderId);
            return;
        }

        currentPendingOrderId = orderId; // Track the current order
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_waiting_order, null);
        builder.setView(dialogView);

        waitingTimerText = dialogView.findViewById(R.id.waiting_timer_text);
        TextView cancelButton = dialogView.findViewById(R.id.CancelOrder);

        builder.setCancelable(false);
        waitingDialog = builder.create();
        waitingDialog.show();
        Log.d("YourOrderMainFragment", "Showing waiting dialog for order: " + orderId);

        startTime = System.currentTimeMillis();
        updateWaitingTimer();

        loadOrdersFromFirestore(true);
        updateTabSelection(true);

        cancelButton.setOnClickListener(v -> {
            cancelOrder(orderId);
            waitingDialog.dismiss();
            currentPendingOrderId = null;
            Log.d("YourOrderMainFragment", "Waiting dialog canceled for order: " + orderId);
        });

        // Monitor this specific order for status changes
        firestore.collection("orders").document(orderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Error listening for order status: " + e.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists() && isAdded()) {
                        String approvalStatus = snapshot.getString("approvalStatus");
                        Log.d("YourOrderMainFragment", "Order " + orderId + " status: " + approvalStatus);
                        if ("accepted".equals(approvalStatus)) {
                            if (waitingDialog != null && waitingDialog.isShowing()) {
                                waitingDialog.dismiss();
                                loadOrdersFromFirestore(true);
                                yourOrdersAdapter.notifyDataSetChanged();
                                Log.d("YourOrderMainFragment", "Waiting dialog dismissed, order accepted: " + orderId);
                            }
                            currentPendingOrderId = null;
                        } else if ("declined".equals(approvalStatus) && orderId.equals(currentPendingOrderId)) {
                            String declineReason = snapshot.getString("declineReason");
                            if (declineReason == null || declineReason.isEmpty()) {
                                declineReason = "No reason provided";
                            }
                            Log.d("YourOrderMainFragment", "Declined order detected: " + orderId + ", reason: " + declineReason);
                            if (waitingDialog != null && waitingDialog.isShowing()) {
                                waitingDialog.dismiss();
                            }
                            showDeclineDialog(orderId, declineReason);
                            currentPendingOrderId = null;
                        }
                    } else if (snapshot != null && !snapshot.exists() && waitingDialog != null && waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                        Log.d("YourOrderMainFragment", "Waiting dialog dismissed, order deleted: " + orderId);
                        currentPendingOrderId = null;
                    }
                });
    }

    private void showDeclineDialog(String orderId, String declineReason) {
        if (!isAdded()) {
            Log.w("YourOrderMainFragment", "Fragment not attached, skipping decline dialog");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Order Declined");
        builder.setMessage("Sorry, your order was declined because: " + declineReason);
        builder.setPositiveButton("OK", (dialog, which) -> {
            loadOrdersFromFirestore(true);
            dialog.dismiss();
            Log.d("YourOrderMainFragment", "Decline dialog dismissed for order: " + orderId);
        });
        builder.setCancelable(false);
        AlertDialog declineDialog = builder.create();
        declineDialog.show();
    }

    private void updateWaitingTimer() {
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (waitingDialog != null && waitingDialog.isShowing() && isAdded()) { // Check if fragment is attached
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long seconds = elapsedTime / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    waitingTimerText.setText(String.format("Please wait until the foodplace accepts your order\nWaiting: %02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void cancelOrder(String orderId) {
        firestore.collection("orders").document(orderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreUpdate", "Order " + orderId + " canceled");
                    if (isAdded()) loadOrdersFromFirestore(true); // Only refresh if attached
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to cancel order: " + e.getMessage()));
    }

    private void startOrderCountdownWatcher() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis() / 1000;
                boolean hasChanged = false;

                for (YourOrderMainDomain order : new ArrayList<>(groupedOrders)) {
                    long endTimeSeconds = order.getEndTime() != null ? order.getEndTime().getSeconds() : 0;
                    if ("pending".equals(order.getStatus()) && currentTime >= endTimeSeconds) {
                        order.setStatus("done");
                        updateOrderStatus(order.getOrderId(), "done");
                        groupedOrders.remove(order);
                        hasChanged = true;
                    }
                }

                if (hasChanged && isAdded()) { // Only update UI if attached
                    yourOrdersAdapter.notifyDataSetChanged();
                    updateEmptyStateVisibility();
                }

                handler.postDelayed(this, 5000);
            }
        };
        handler.post(runnable);
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        firestore.collection("orders").document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreUpdate", "Order " + orderId + " updated to " + newStatus);
                    if (isAdded()) loadOrdersFromFirestore(newStatus.equals("pending")); // Only refresh if attached
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to update order status: " + e.getMessage()));
    }

    private void updateTabSelection(boolean isCurrentOrdersSelected) {
        if (isCurrentOrdersSelected) {
            currentOrdersText.setPaintFlags(currentOrdersText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            orderHistoryText.setPaintFlags(orderHistoryText.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        } else {
            orderHistoryText.setPaintFlags(orderHistoryText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            currentOrdersText.setPaintFlags(currentOrdersText.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    private void fetchRestaurantDetails(String restaurantId, YourOrderMainDomain order, Runnable onComplete) {
        firestore.collection("FoodPlaces")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        if (name != null) {
                            order.setRestaurant(new RestaurantDomain(name, imageUrl));
                        } else {
                            order.setRestaurant(new RestaurantDomain("Unknown", imageUrl != null ? imageUrl : ""));
                        }
                    } else {
                        order.setRestaurant(new RestaurantDomain("Unknown", ""));
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching restaurant: " + e.getMessage());
                    order.setRestaurant(new RestaurantDomain("Unknown", ""));
                    onComplete.run();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrdersFromFirestore(true);
        checkForPendingApprovalOrders();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingApprovalListener != null) {
            pendingApprovalListener.remove();
            pendingApprovalListener = null;
        }
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
        timerHandler.removeCallbacksAndMessages(null);
        currentPendingOrderId = null;
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

    private void setupProfileListener(FirebaseUser firebaseUser) {
        profileListener = db.collection("users").document(firebaseUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("YourOrderMainFragment", "Listen failed", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                        loadProfileImage(profilePictureUrl, profileIcon);
                    }
                });
    }

    private void loadProfileImage(String profilePictureUrl, ImageView imageView) {
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty() && isAdded()) {
            Glide.with(this)
                    .load(profilePictureUrl)
                    .transform(new CircleCrop())
                    .into(imageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.profile_picture)
                    .transform(new CircleCrop())
                    .into(imageView);
        }
    }

    private void openYourOrderFragment(YourOrderMainDomain order) {
        YourOrderFragment fragment = YourOrderFragment.newInstance(
                order.getRestaurant().getName(),
                order.getOrderId(),
                order.getTotalPrice(),
                order.getTotalPrepTime(),
                order.getItems(),
                order,
                order.getStatus(),
                false
        );
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }
}