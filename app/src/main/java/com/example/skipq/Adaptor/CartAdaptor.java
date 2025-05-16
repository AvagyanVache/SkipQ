package com.example.skipq.Adaptor;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.CartManager;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.text.MessageFormat;
import java.util.ArrayList;

public class CartAdaptor extends RecyclerView.Adapter<CartAdaptor.ViewHolder> {

    private Context context;
    private ArrayList<MenuDomain> cartList;
    private OnCartUpdatedListener listener;

    public CartAdaptor(Context context, ArrayList<MenuDomain> cartList, OnCartUpdatedListener listener) {
        this.context = context;
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_cart_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain cartItem = cartList.get(position);

        String fullDescription = cartItem.getItemDescription() != null ? cartItem.getItemDescription() : "";
        String shortDescription = shortenDescription(fullDescription);
        holder.cartItemDescription.setText(shortDescription);
        holder.cartItemName.setText(cartItem.getItemName());
        holder.cartItemCount.setText(String.format("%02d", cartItem.getItemCount()));
        holder.cartItemPrice.setText(String.format("%.2f֏", Double.parseDouble(cartItem.getItemPrice())));
        holder.cartItemPrepTime.setText(MessageFormat.format("{0} min", cartItem.getPrepTime()));

        if (cartItem.getItemImg() != null && !cartItem.getItemImg().isEmpty()) {
            Glide.with(context)
                    .load(cartItem.getItemImg())
                    .centerCrop()
                    .into(holder.cartItemImage);
        } else {
            holder.cartItemImage.setImageResource(R.drawable.white); // Fallback
        }
        holder.cartItemImage.setClipToOutline(true);
        holder.cartItemImage.setBackgroundResource(R.drawable.rounded_corners);


        holder.addButton.setVisibility(View.VISIBLE);
        holder.minusButton.setVisibility(View.VISIBLE);
        holder.addButton.setOnClickListener(v -> {
            cartItem.setItemCount(cartItem.getItemCount() + 1);
            CartManager.getInstance().updateItem(cartItem);
            notifyItemChanged(position);
            updateTotal();
        });

        holder.minusButton.setOnClickListener(v -> {
            if (cartItem.getItemCount() > 1) {
                cartItem.setItemCount(cartItem.getItemCount() - 1);
                CartManager.getInstance().updateItem(cartItem);
                notifyItemChanged(position);
            } else {
                removeItemFromCart(position);
            }
            updateTotal();
        });
        holder.itemView.setOnClickListener(v -> showItemDetailsDialog(cartItem));
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    private void removeItemFromCart(int position) {
        try {
            if (position >= 0 && position < cartList.size()) {
                MenuDomain cartItem = cartList.get(position);
                CartManager.getInstance().removeFromCart(cartItem);
                cartList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cartList.size());
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("CartAdaptor", "Attempted to remove item at invalid position: " + position, e);
        }
    }

    private void updateTotal() {
        double total = 0;
        int totalPrepTime = 0;

        for (MenuDomain item : cartList) {
            total += Double.parseDouble(item.getItemPrice()) * item.getItemCount();
            totalPrepTime += item.getPrepTime() * item.getItemCount();
        }

        listener.onCartUpdated(total, totalPrepTime);


    int averagePrepTime = (cartList.size() > 0) ? totalPrepTime / cartList.size() : 0;
        listener.onCartUpdated(total,averagePrepTime);

    }
    private String shortenDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "No description";
        }
        if (description.length() <= 10) {
            return description;
        }
        return description.substring(0, 10) + "...";
    }

    private void showItemDetailsDialog(MenuDomain item) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_menu_item_details, null);

        ImageView itemImage = dialogView.findViewById(R.id.item_image);
        TextView itemName = dialogView.findViewById(R.id.item_name);
        TextView itemDescription = dialogView.findViewById(R.id.item_description);
        TextView itemPrice = dialogView.findViewById(R.id.item_price);
        TextView itemPrepTime = dialogView.findViewById(R.id.item_prep_time);

        itemName.setText(item.getItemName() != null ? item.getItemName() : "N/A");
        itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "No description"); // Full description
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            Log.e("CartAdaptor", "Invalid price format for item: " + item.getItemName(), e);
        }
        itemPrice.setText(String.format("֏ %.2f", price));
        itemPrepTime.setText(String.format("%d min", item.getPrepTime()));

        if (item.getItemImg() != null && !item.getItemImg().isEmpty() && item.getItemImg().startsWith("http")) {
            Glide.with(context)
                    .load(item.getItemImg())
                    .centerCrop()
                    .into(itemImage);
        } else {
            itemImage.setImageResource(R.drawable.white);
        }
        itemImage.setClipToOutline(true);
        itemImage.setBackgroundResource(R.drawable.rounded_corners);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Item Details")
                .setView(dialogView)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .create();
        dialog.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cartItemName, cartItemCount, cartItemPrice, cartItemPrepTime,cartItemDescription;
        ImageView cartItemImage, addButton, minusButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cartItemDescription= itemView.findViewById(R.id.CartItemDescription);
            cartItemPrepTime = itemView.findViewById(R.id.PrepTime);
            cartItemName = itemView.findViewById(R.id.CartItemTitle);
            cartItemCount = itemView.findViewById(R.id.CartItemCount);
            cartItemPrice = itemView.findViewById(R.id.CartItemPrice);
            cartItemImage = itemView.findViewById(R.id.CartItemPhoto);
            addButton = itemView.findViewById(R.id.CartItemPlus);
            minusButton = itemView.findViewById(R.id.CartItemMinus);
        }
    }

    public interface OnCartUpdatedListener {
        void onCartUpdated(double total, int averagePrepTime);

    }
}
