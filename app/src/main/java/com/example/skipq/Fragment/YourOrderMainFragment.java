package com.example.skipq.Fragment;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

        yourOrdersAdapter = new YourOrderMainAdaptor(requireContext(), groupedOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(yourOrdersAdapter);

        loadOrdersFromFirestore(true);
        startOrderCountdownWatcher();
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("FirestoreError", "User is not logged in");
            return;
        }
        userId = user.getUid();
        Log.d("YourOrderMainFragment", "Current userId: " + userId);

        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        Timestamp currentTimestamp = new Timestamp(currentTimeSeconds, 0);
        Log.d("YourOrderMainFragment", "Current time (seconds): " + currentTimeSeconds +
                ", Timestamp: " + currentTimestamp.toDate().toString());

        Query query = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("approvalStatus", "accepted");

        if (isCurrentOrders) {
            query = query.whereGreaterThan("endTime", currentTimestamp); // Current orders
        } else {
            query = query.whereLessThanOrEqualTo("endTime", currentTimestamp); // Past orders
        }

        query.orderBy("endTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    Log.d("YourOrderMainFragment", "Snapshot size: " + (snapshots != null ? snapshots.size() : 0));
                    groupedOrders.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        CountDownLatch latch = new CountDownLatch(snapshots.size());

                        for (QueryDocumentSnapshot document : snapshots) {
                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            order.setOrderId(document.getId());
                            order.setStartTime(document.getTimestamp("startTime"));
                            order.setEndTime(document.getTimestamp("endTime"));

                            String restaurantId = document.getString("restaurantId");
                            if (restaurantId != null) {
                                firestore.collection("FoodPlaces").document(restaurantId)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                String name = doc.getString("name");
                                                String imageUrl = doc.getString("imageUrl");
                                                order.setRestaurant(new RestaurantDomain(name != null ? name : "Unknown", imageUrl != null ? imageUrl : ""));
                                            } else {
                                                order.setRestaurant(new RestaurantDomain("Unknown", ""));
                                            }
                                            latch.countDown();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirestoreError", "Error fetching restaurant: " + e.getMessage());
                                            order.setRestaurant(new RestaurantDomain("Unknown", ""));
                                            latch.countDown();
                                        });
                            } else {
                                order.setRestaurant(new RestaurantDomain("Unknown", ""));
                                latch.countDown();
                            }
                            groupedOrders.add(order);
                        }

                        // Wait for all restaurant fetches to complete
                        new Thread(() -> {
                            try {
                                latch.await(); // Block until all fetches are done
                                requireActivity().runOnUiThread(() -> {
                                    Log.d("YourOrderMainFragment", "Grouped orders size: " + groupedOrders.size());
                                    yourOrdersAdapter.notifyDataSetChanged();
                                    updateEmptyStateVisibility();
                                });
                            } catch (InterruptedException e) {
                                Log.e("YourOrderMainFragment", "Latch interrupted: " + e.getMessage());
                            }
                        }).start();
                    } else {
                        Log.d("YourOrderMainFragment", "No orders found for userId: " + userId);
                        yourOrdersAdapter.notifyDataSetChanged();
                        updateEmptyStateVisibility();
                    }
                });
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

                if (hasChanged) {
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
                .addOnSuccessListener(aVoid -> Log.d("FirestoreUpdate", "Order " + orderId + " updated to " + newStatus))
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
                        Log.e("CartFragment", "Listen failed", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                        String base64Image = documentSnapshot.getString("profileImage");
                        loadProfileImage(base64Image, profileIcon);
                    }
                });
    }

    private void loadProfileImage(String base64Image, ImageView imageView) {
        if (base64Image != null && !base64Image.isEmpty() && isAdded()) {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Glide.with(this)
                    .load(decodedByte)
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

    @Override
    public void onResume() {
        super.onResume();
        loadOrdersFromFirestore(true);
    }
}