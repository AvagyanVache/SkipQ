package com.example.skipq;

import android.util.Log;

import com.example.skipq.Adaptor.CartAdaptor;
import com.example.skipq.Domain.MenuDomain;
import java.util.ArrayList;

public class CartManager {

    private static CartManager instance;
    private ArrayList<MenuDomain> cartList;
    private CartAdaptor.OnCartUpdatedListener listener;


    private CartManager() {
        cartList = new ArrayList<>();
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

    public void addToCart(MenuDomain item) {
        boolean itemExists = false;

        for (MenuDomain menuItem : cartList) {
            if (menuItem.getItemName().equals(item.getItemName())) {
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            cartList.add(item);
            notifyCartUpdated();
        }
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
            listener.onCartUpdated(getTotalPrice(), getTotalPrepTime());
        }

    }
}
