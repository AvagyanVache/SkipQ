package com.example.skipq.Adaptor;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.util.List;
import java.util.function.Consumer;

public class MenuManagementAdapter extends RecyclerView.Adapter<MenuManagementAdapter.ViewHolder> {
    private Context context;
    private List<MenuDomain> items;
    private Consumer<MenuDomain> onUpdate;
    private Consumer<String> onDelete;
    private int selectedPosition = -1;

    public MenuManagementAdapter(Context context, List<MenuDomain> items, Consumer<MenuDomain> onUpdate, Consumer<String> onDelete) {
        this.context = context;
        this.items = items;
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    public void updateItems(List<MenuDomain> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain item = items.get(position);

        String fullDescription = item.getItemDescription() != null ? item.getItemDescription() : "";
        String shortDescription = shortenDescription(fullDescription);
        holder.itemDescription.setText(shortDescription);
        holder.itemName.setText(item.getItemName() != null ? item.getItemName() : "N/A");
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            Log.e("MenuManagementAdapter", "Invalid price format for item: " + item.getItemName(), e);
        }
        holder.itemPrice.setText(String.format("֏ %.2f", price));
        holder.itemPrepTime.setText(item.getPrepTime() >= 0 ? String.format("%d min", item.getPrepTime()) : "N/A");
        holder.itemAvailability.setText((item.isAvailable() ? "Available" : "Unavailable"));

        if (item.getItemImg() != null && !item.getItemImg().isEmpty() && item.getItemImg().startsWith("http")) {
            Glide.with(context)
                    .load(item.getItemImg())
                    .centerCrop()
                    .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.white);
        }
        holder.itemImage.setClipToOutline(true);
        holder.itemImage.setBackgroundResource(R.drawable.rounded_corners);
        if (position == selectedPosition) {
            holder.updateButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.updateButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position == selectedPosition ? -1 : position;
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition);
            } else {
                showItemDetailsDialog(item);
            }
        });

        holder.updateButton.setOnClickListener(v -> {
            onUpdate.accept(item);
            resetSelection();
        });

        holder.deleteButton.setOnClickListener(v -> {
            onDelete.accept(item.getItemName());
            resetSelection();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void resetSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
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
        itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "No description");
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            Log.e("MenuManagementAdapter", "Invalid price format for item: " + item.getItemName(), e);
        }
        itemPrice.setText(String.format("֏ %.2f", price));
        itemPrepTime.setText(item.getPrepTime() >= 0 ? String.format("%d min", item.getPrepTime()) : "N/A");

        if (item.getItemImg() != null && !item.getItemImg().isEmpty() && item.getItemImg().startsWith("http")) {
            Glide.with(context)
                    .load(item.getItemImg())
                    .placeholder(R.drawable.white)
                    .centerCrop()
                    .error(R.drawable.white)
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemPrice, itemPrepTime, itemDescription, itemAvailability;
        ImageView updateButton, deleteButton, itemImage;

        ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.item_name_text);
            itemPrice = view.findViewById(R.id.item_price_text);
            itemPrepTime = view.findViewById(R.id.item_prep_time_text);
            itemDescription = view.findViewById(R.id.item_description_text);
            itemAvailability = view.findViewById(R.id.item_availability_text);
            updateButton = view.findViewById(R.id.EditItem);
            deleteButton = view.findViewById(R.id.DeleteItem);
            itemImage = view.findViewById(R.id.ItemPhoto);
        }
    }
}