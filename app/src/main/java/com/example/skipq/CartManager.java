package com.example.skipq;

import android.util.Log;

import com.example.skipq.Adaptor.CartAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CartManager {

    private static CartManager instance;
    private ArrayList<MenuDomain> cartList;
    private CartAdaptor.OnCartUpdatedListener listener;
    private String selectedAddress; // Added to store selected address
    private LatLng selectedLatLng; // Added to store address coordinates

    private CartManager() {
        cartList = new ArrayList<>();
        selectedAddress = null;
        selectedLatLng = null;
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void setListener(CartAdaptor.OnCartUpdatedListener listener) {
        this.listener = listener;
    }

    public ArrayList<MenuDomain> getCartList() {
        return cartList;
    }

    public void setSelectedAddress(String address, LatLng latLng) {
        this.selectedAddress = address;
        this.selectedLatLng = latLng;
        Log.d("CartManager", "Selected address set to: " + address);
        notifyCartUpdated();
    }

    public String getSelectedAddress() {
        return selectedAddress;
    }

    public LatLng getSelectedLatLng() {
        return selectedLatLng;
    }

    public void addToCart(MenuDomain item) {
        if (item.getRestaurant() == null) {
            Log.e("CartManager", "Item has no restaurant: " + item.getItemName());
            String restaurantId = item.getRestaurantId();

            if (restaurantId != null && !restaurantId.isEmpty()) {
                Log.d("CartManager", "Fetching restaurant for ID: " + restaurantId);
                fetchRestaurantById(restaurantId, new RestaurantCallback() {
                    @Override
                    public void onRestaurantFetched(RestaurantDomain restaurant) {
                        item.setRestaurant(restaurant);
                        Log.d("CartManager", "Restaurant fetched: " + restaurant.getName());
                        addItemToCart(item);
                    }
                });
            } else {
                Log.e("CartManager", "Item has no restaurant ID: " + item.getItemName());
                item.setRestaurant(new RestaurantDomain("Unknown Restaurant", "https://example.com/default_image.png"));
                addItemToCart(item);
            }
        } else {
            Log.d("CartManager", "Item has restaurant: " + item.getRestaurant().getName());
            addItemToCart(item);
        }
    }

    private void addItemToCart(MenuDomain item) {
        boolean itemExists = false;
        for (MenuDomain menuItem : cartList) {
            if (menuItem.getItemName().equals(item.getItemName())) {
                itemExists = true;
                menuItem.setItemCount(menuItem.getItemCount() + item.getItemCount());
                break;
            }
        }
        if (!itemExists) {
            cartList.add(item);
        }
        notifyCartUpdated();
    }

    public void fetchRestaurantById(String restaurantId, final RestaurantCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("FoodPlaces")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        RestaurantDomain restaurant = new RestaurantDomain(name, imageUrl);
                        callback.onRestaurantFetched(restaurant);
                    } else {
                        callback.onRestaurantFetched(new RestaurantDomain("Unknown Restaurant", ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "Failed to fetch restaurant: " + e.getMessage());
                    callback.onRestaurantFetched(new RestaurantDomain("Unknown Restaurant", ""));
                });
    }

    public interface RestaurantCallback {
        void onRestaurantFetched(RestaurantDomain restaurant);
    }

    public void updateItem(MenuDomain item) {
        for (MenuDomain menuItem : cartList) {
            if (menuItem.getItemName().equals(item.getItemName())) {
                menuItem.setItemCount(item.getItemCount());
                if (menuItem.getItemCount() <= 0) {
                    removeFromCart(item);
                }
                notifyCartUpdated();
                return;
            }
        }
    }

    public void removeFromCart(MenuDomain item) {
        for (int i = 0; i < cartList.size(); i++) {
            if (cartList.get(i).getItemName().equals(item.getItemName())) {
                cartList.remove(i);
                notifyCartUpdated();
                break;
            }
        }
    }

    public void clearCart() {
        cartList.clear();
        selectedAddress = null; // Clear selected address when cart is cleared
        selectedLatLng = null;
        notifyCartUpdated();
    }

    public double getTotalPrice() {
        double total = 0;
        for (MenuDomain item : cartList) {
            String priceStr = item.getItemPrice();
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                total += Double.parseDouble(priceStr) * item.getItemCount();
            }
        }
        return total;
    }

    public int getTotalPrepTime() {
        int totalPrepTime = 0;
        for (MenuDomain item : cartList) {
            totalPrepTime += item.getPrepTime() * item.getItemCount();
        }
        Log.d("CartManager", "Calculated Total Prep Time: " + totalPrepTime);
        return totalPrepTime;
    }

    private void notifyCartUpdated() {
        if (listener != null) {
            double totalPrice = getTotalPrice();
            int totalPrepTime = getTotalPrepTime();
            Log.d("CartManager", "Notifying Cart Updated - Total Price: " + totalPrice + ", Total Prep Time: " + totalPrepTime);
            listener.onCartUpdated(totalPrice, totalPrepTime);
        }
    }
}