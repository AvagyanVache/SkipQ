package com.example.skipq;

import com.example.skipq.Domain.MenuDomain;
import java.util.ArrayList;

public class CartManager {

    private static CartManager instance;
    private ArrayList<MenuDomain> cartList;

    private CartManager() {
        cartList = new ArrayList<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
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
        }
    }


    public void updateItem(MenuDomain item) {
        for (MenuDomain menuItem : cartList) {
            if (menuItem.getItemName().equals(item.getItemName())) {
                menuItem.setItemCount(item.getItemCount());
                if (menuItem.getItemCount() <= 0) {
                    removeFromCart(item);
                }
                return;
            }
        }
    }

    public void removeFromCart(MenuDomain item) {
        for (int i = 0; i < cartList.size(); i++) {
            if (cartList.get(i).getItemName().equals(item.getItemName())) {
                cartList.remove(i);
                break;
            }
        }
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

}
