package com.example.skipq.Adaptor;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.text.MessageFormat;
import java.util.ArrayList;

public class YourOrderAdaptor extends RecyclerView.Adapter<YourOrderAdaptor.ViewHolder> {
    private Context context;
    private ArrayList<MenuDomain> orderList;

    public YourOrderAdaptor(Context context, ArrayList<MenuDomain> orderList) {
        this.context = context;
        this.orderList = orderList != null ? new ArrayList<>(orderList) : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_yourorder_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain item = orderList.get(position);
        holder.itemName.setText(item.getItemName() != null ? item.getItemName() : "N/A");

        // Shorten description to 2-3 words
        String fullDescription = item.getItemDescription() != null ? item.getItemDescription() : "";
        String shortDescription = shortenDescription(fullDescription);
        holder.itemDescription.setText(shortDescription);

        holder.itemPrepTime.setText(MessageFormat.format("{0} min", item.getPrepTime()));
        holder.itemCount.setText(String.valueOf(item.getItemCount()));
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        holder.itemPrice.setText(String.format("%.2f֏", price));

        if (item.getItemImg() != null && !item.getItemImg().isEmpty() && item.getItemImg().startsWith("http")) {
            Glide.with(context)
                    .load(item.getItemImg())
                    .centerCrop()
                    .into(holder.cartItemImage);
        } else {
            holder.cartItemImage.setImageResource(R.drawable.white);
        }

        holder.cartItemImage.setClipToOutline(true);
        holder.cartItemImage.setBackgroundResource(R.drawable.rounded_corners);
        holder.itemView.setOnClickListener(v -> showItemDetailsDialog(item));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    private String shortenDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "No description";
        }
        if (description.length() <= 15) {
            return description;
        }
        return description.substring(0, 15) + "...";
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
        itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "No description");
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        itemPrice.setText(String.format("֏ %.2f", price));
        itemPrepTime.setText(item.getPrepTime() > 0 ? String.format("%d min", item.getPrepTime()) : "Not specified");

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
        TextView itemName, itemPrice, itemDescription, itemPrepTime, itemCount;
        ImageView cartItemImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cartItemImage = itemView.findViewById(R.id.CartItemPhoto);
            itemPrepTime = itemView.findViewById(R.id.PrepTime);
            itemDescription = itemView.findViewById(R.id.CartItemDescription);
            itemCount = itemView.findViewById(R.id.CartItemCount);
            itemName = itemView.findViewById(R.id.CartItemTitle);
            itemPrice = itemView.findViewById(R.id.CartItemPrice);
        }
    }
}