package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
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
import com.example.skipq.Adaptor.YourOrderMainAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.example.skipq.YourOrderFragment;
import com.google.common.reflect.TypeToken;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;


public class YourOrderMainFragment extends Fragment {
    private RecyclerView recyclerView;
    private YourOrderMainAdaptor yourOrdersAdapter;
    private TextView emptyOrderText, goShoppingText, currentOrdersText, orderHistoryText;
    private ArrayList<YourOrderMainDomain> groupedOrders;
    private FirebaseFirestore firestore;
    private String userId;
    private ImageView profileIcon;
    private String filterStatus;
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
            Intent intent = new Intent(getActivity(), HomeActivity.class);intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent);
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

        loadOrdersFromFirestore(true);
        updateTabSelection(true);
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
                .whereEqualTo("userId", userId);

        if (isCurrentOrders) {
            query = query.whereGreaterThan("endTime", currentTimestamp); // Pending orders
        } else {
            query = query.whereLessThanOrEqualTo("endTime", currentTimestamp); // Done orders
        }

        query.orderBy("endTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    groupedOrders.clear(); // Clear previous orders

                    if (snapshots != null && !snapshots.isEmpty()) {
                        int totalOrders = snapshots.size();
                        final int[] fetchedCount = {0}; // Track how many restaurant details are fetched

                        for (QueryDocumentSnapshot document : snapshots) {
                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            order.setStartTime(document.getTimestamp("startTime"));
                            order.setEndTime(document.getTimestamp("endTime"));
                            order.setOrderId(document.getId());

                            long endTimeSeconds = order.getEndTime() != null ? order.getEndTime().getSeconds() : 0;
                            String status = (currentTime < endTimeSeconds) ? "pending" : "done";
                            order.setStatus(status);

                            if ((isCurrentOrders && "pending".equals(status)) || (!isCurrentOrders && "done".equals(status))) {
                                String restaurantId = document.getString("restaurantId");
                                if (restaurantId != null) {
                                    fetchRestaurantDetails(restaurantId, order, () -> {
                                        groupedOrders.add(order); // Add only after restaurant is fetched
                                        fetchedCount[0]++;
                                        if (fetchedCount[0] == totalOrders) {
                                            yourOrdersAdapter.notifyDataSetChanged(); // Notify only when all data is ready
                                            updateEmptyStateVisibility();
                                        }
                                    });
                                } else {
                                    Log.e("FirestoreError", "RestaurantId is null for order: " + order.getOrderId());
                                    order.setRestaurant(new RestaurantDomain("Unknown", "")); // Fallback
                                    groupedOrders.add(order);
                                    fetchedCount[0]++;
                                    if (fetchedCount[0] == totalOrders) {
                                        yourOrdersAdapter.notifyDataSetChanged();
                                        updateEmptyStateVisibility();
                                    }
                                }
                            }
                        }
                    } else {
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

                for (YourOrderMainDomain order : new ArrayList<>(groupedOrders)) { // Use copy to avoid concurrent modification
                    long endTimeSeconds = order.getEndTime() != null ? order.getEndTime().getSeconds() : 0; // Use stored endTime
                    if ("pending".equals(order.getStatus()) && currentTime >= endTimeSeconds) {
                        order.setStatus("done");
                        updateOrderStatus(order.getOrderId(), "done"); // Update in Firestore, no endTime overwrite
                        groupedOrders.remove(order); // Remove from current list
                        hasChanged = true;
                    }
                }

                if (hasChanged) {
                    yourOrdersAdapter.notifyDataSetChanged();
                    updateEmptyStateVisibility();
                }

                handler.postDelayed(this, 5000); // Check every 5 seconds
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
                    loadOrdersFromFirestore(newStatus.equals("pending"));
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
                    onComplete.run(); // Call the callback when done
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching restaurant: " + e.getMessage());
                    order.setRestaurant(new RestaurantDomain("Unknown", "")); // Fallback
                    onComplete.run();
                });
    }


    private void saveOrderToFirestore(YourOrderMainDomain order) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("restaurantId", order.getRestaurantId());
        orderData.put("totalPrice", order.getTotalPrice());
        orderData.put("totalPrepTime", order.getTotalPrepTime());
        orderData.put("items", order.getItems());
        orderData.put("startTime", Timestamp.now());
        db.collection("orders")
                .document(order.getOrderId())
                .set(orderData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Order successfully saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving order", e));
    }


    @Override
    public void onResume() {
        super.onResume();
        loadOrdersFromFirestore(true);

    }


    public interface OnRestaurantFetchedListener {
        void onFetched(RestaurantDomain restaurant);
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
        Log.d("YourOrderMainFragment", "Passing order to fragment: " + new Gson().toJson(order));
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }
}