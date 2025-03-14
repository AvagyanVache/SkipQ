package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.example.skipq.Adaptor.YourOrderMainAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.google.common.reflect.TypeToken;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YourOrderMainFragment extends Fragment {
    private RecyclerView recyclerView;
    private YourOrderMainAdaptor yourOrdersAdapter;
    private TextView emptyOrderText, goShoppingText, currentOrdersText, orderHistoryText;
    private ArrayList<YourOrderMainDomain> groupedOrders;
    private FirebaseFirestore firestore;
    private String userId;
    private String filterStatus;


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


        firestore = FirebaseFirestore.getInstance();
        groupedOrders = new ArrayList<>();

        yourOrdersAdapter = new YourOrderMainAdaptor(requireContext(), groupedOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(yourOrdersAdapter);

        loadOrdersFromFirestore();
        updateEmptyStateVisibility();

        currentOrdersText.setOnClickListener(v -> {
            loadOrdersFromFirestore();
            updateTabSelection(true);
        });

        orderHistoryText.setOnClickListener(v -> {
            loadOrdersFromFirestore();
            updateTabSelection(false);
        });

        loadOrdersFromFirestore();
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
    /*
    private void loadOrdersFromFirestore(String filterStatus) {
        this.filterStatus = filterStatus;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Log.e("FirestoreError", "User is not logged in");
            return;
        }

        firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    groupedOrders.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            Timestamp startTime = document.getTimestamp("startTime");

                            if (startTime == null) {
                                Log.e("FirestoreError", "startTime is null for order: " + document.getId());
                                continue;  // Skip this order to prevent crashes
                            }

                            order.setStartTime(startTime);
                            order.setOrderId(document.getId());

                            long currentTime = System.currentTimeMillis() / 1000;
                            long orderStartTime = startTime.getSeconds();
                            long prepTime = order.getTotalPrepTime();

                            String status = (currentTime < orderStartTime + prepTime) ? "pending" : "done";
                            order.setStatus(status);

                            if (this.filterStatus == null || this.filterStatus.equals(status)) {
                                groupedOrders.add(order);
                            }

                            yourOrdersAdapter.notifyDataSetChanged();
                            String restaurantId = document.getString("restaurantId");
                            if (restaurantId != null) {
                                fetchRestaurantDetails(restaurantId, order);
                            }
                        }
                    }
                    updateEmptyStateVisibility();
                });
    }


     */


    private void loadOrdersFromFirestore() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Log.e("FirestoreError", "User is not logged in");
            return;
        }

        firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                        return;
                    }

                    groupedOrders.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                            order.setStartTime(document.getTimestamp("startTime"));
                            order.setOrderId(document.getId());

                            String restaurantId = document.getString("restaurantId");
                            if (restaurantId != null) {
                                fetchRestaurantDetails(restaurantId, order);
                            }
                        }
                    } else {
                        updateEmptyStateVisibility();
                    }
                });
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


    private void fetchRestaurantDetails(String restaurantId, YourOrderMainDomain order) {
        Log.d("FetchRestaurant", "Fetching details for: " + restaurantId);

        firestore.collection("FoodPlaces")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getId();
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        RestaurantDomain restaurant = new RestaurantDomain(name, imageUrl);
                        Log.d("FetchRestaurant", "Fetched: " + restaurant.getName());

                        order.setRestaurant(restaurant);

                        groupedOrders.add(order);
                        yourOrdersAdapter.notifyDataSetChanged();
                        updateEmptyStateVisibility();
                    } else {
                        Log.e("FirestoreError", "Restaurant not found for ID: " + restaurantId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching restaurant: " + e.getMessage());
                });
    }

    /* private void loadOrdersFromFirestore() {
         FirebaseAuth auth = FirebaseAuth.getInstance();
         FirebaseUser user = auth.getCurrentUser();
         if (user == null) {
             Log.e("FirestoreError", "User is not logged in");
             return;
         }
         userId = user.getUid();

         firestore.collection("orders")
                 .whereEqualTo("userId", userId)
                 .addSnapshotListener((snapshots, error) -> {
                     if (error != null) {
                         Log.e("FirestoreError", "Failed to load orders: " + error.getMessage());
                         return;
                     }

                     groupedOrders.clear();

                     if (snapshots != null) {
                         for (QueryDocumentSnapshot document : snapshots) {
                             Log.d("Firestore Debug", "Checking document: " + document.getId());

                             // Validate restaurantId
                             if (!document.contains("restaurantId") || !(document.get("restaurantId") instanceof String)) {
                                 Log.e("FirestoreError", "Invalid or missing restaurantId in document: " + document.getId());
                                 continue; // Skip this document
                             }

                             YourOrderMainDomain order = document.toObject(YourOrderMainDomain.class);
                             String restaurantId = document.getString("restaurantId");

                             // Fetch restaurant details using the restaurantId
                             fetchRestaurantById(restaurantId, restaurant -> {
                                 order.setRestaurant(restaurant);

                                 // Process items
                                 List<Map<String, Object>> itemsData = (List<Map<String, Object>>) document.get("items");
                                 if (itemsData != null) {
                                     ArrayList<MenuDomain> items = new ArrayList<>();
                                     for (Map<String, Object> itemData : itemsData) {
                                         MenuDomain item = new MenuDomain();
                                         item.setItemName((String) itemData.get("name"));
                                         item.setItemPrice(String.valueOf(itemData.get("price")));
                                         item.setPrepTime(((Long) itemData.get("prepTime")).intValue());
                                         items.add(item);
                                     }
                                     order.setItems(items);
                                 }

                                 // Add order to the list
                                 if (!groupedOrders.contains(order)) {
                                     groupedOrders.add(order);
                                     yourOrdersAdapter.notifyDataSetChanged();
                                     updateEmptyStateVisibility();
                                 }
                             });
                         }
                     }
                 });
     }

     */
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
        loadOrdersFromFirestore();

    }

    /*private ArrayList<YourOrderMainDomain> loadAndGroupOrders() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        ArrayList<MenuDomain> allItems = new Gson().fromJson(cartItemsJson, new TypeToken<ArrayList<MenuDomain>>() {}.getType());
        if (allItems == null || allItems.isEmpty()) {
            Log.e("YourOrderMainFragment", "No items found in SharedPreferences!");
            return new ArrayList<>();
        }

        ArrayList<YourOrderMainDomain> ordersList = new ArrayList<>();
        Map<String, YourOrderMainDomain> restaurantMap = new HashMap<>();

        for (MenuDomain item : allItems) {
            if (item.getRestaurant() == null) {
                Log.e("YourOrderMainFragment", "Item has no restaurant: " + item.getItemName());
                String restaurantId = item.getRestaurantId();
                if (restaurantId != null && !restaurantId.isEmpty()) {
                    fetchRestaurantById(restaurantId, restaurant -> {
                        if (restaurant != null) {
                            item.setRestaurant(restaurant);
                        } else {
                            Log.e("YourOrderMainFragment", "Failed to fetch restaurant for ID: " + restaurantId);
                            item.setRestaurant(new RestaurantDomain("Unknown Restaurant", "https://example.com/default_image.png"));
                        }

                                           });
                } else {
                    Log.e("YourOrderMainFragment", "Item has no restaurant ID: " + item.getItemName());
                    item.setRestaurant(new RestaurantDomain("Unknown Restaurant", "https://example.com/default_image.png"));
                }

        }

        String restaurantKey = item.getRestaurant().getName();
            if (!restaurantMap.containsKey(restaurantKey)) {
                restaurantMap.put(restaurantKey, new YourOrderMainDomain(
                        "generatedOrderId",
                        item.getRestaurant(),
                        0.0,
                        0,
                        new ArrayList<>(),
                        new Timestamp(System.currentTimeMillis() / 1000, 0)
                ));
            }
            YourOrderMainDomain order = restaurantMap.get(restaurantKey);
            if (order != null) {
                order.getItems().add(item);
                order.setTotalPrice(order.getTotalPrice() + Double.parseDouble(item.getItemPrice()));
                order.setTotalPrepTime(order.getTotalPrepTime() + item.getPrepTime());
            }
            Log.d("YourOrderMainFragment", "Processing item: " + item.getItemName() + " for restaurant: " + restaurantKey);
        }
        Log.d("YourOrderMainFragment", "Grouped orders count: " + restaurantMap.size());
        return new ArrayList<>(restaurantMap.values());
    }

     */



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

    private void openYourOrderFragment(YourOrderMainDomain order) {
        YourOrderFragment fragment = YourOrderFragment.newInstance(
                order.getRestaurant().getName(),
                order.getOrderId(),
                order.getTotalPrice(),
                order.getTotalPrepTime(),
                order.getItems(),
                order,
                order.getStatus()
        );

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }
}
