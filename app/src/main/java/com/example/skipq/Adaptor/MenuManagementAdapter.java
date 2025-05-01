package com.example.skipq.Adaptor;

import com.bumptech.glide.Glide;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MenuManagementAdapter extends RecyclerView.Adapter<MenuManagementAdapter.ViewHolder> {
    private List<MenuDomain> items;
    private Consumer<MenuDomain> onUpdate;
    private Consumer<String> onDelete;
    private int selectedPosition = -1;

    public MenuManagementAdapter(List<MenuDomain> items, Consumer<MenuDomain> onUpdate, Consumer<String> onDelete) {
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

        // Safely set text with null checks
        if (holder.itemName != null) holder.itemName.setText(item.getItemName() != null ? item.getItemName() : "");
        if (holder.itemPrice != null) holder.itemPrice.setText(item.getItemPrice() != null ? item.getItemPrice() : "");
        if (holder.itemPrepTime != null) holder.itemPrepTime.setText(item.getPrepTime() >= 0 ? String.valueOf(item.getPrepTime()) : "");
        if (holder.itemDescription != null) holder.itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "");
        holder.itemAvailability.setText("Availability: " + (item.isAvailable() ? "Available" : "Unavailable"));        String base64Image = item.getItemImg();
        if (holder.itemImage != null) {
            if (item.getItemImg() != null && !item.getItemImg().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getItemImg())
                        .into(holder.itemImage);
            } else {
                holder.itemImage.setImageDrawable(null); // Or set a placeholder
            }
        }

        // Set visibility of Edit and Delete buttons based on selected position
        if (position == selectedPosition) {
            if (holder.updateButton != null) holder.updateButton.setVisibility(View.VISIBLE);
            if (holder.deleteButton != null) holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            if (holder.updateButton != null) holder.updateButton.setVisibility(View.GONE);
            if (holder.deleteButton != null) holder.deleteButton.setVisibility(View.GONE);
        }

        // Handle item click to toggle visibility
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            int currentPosition = holder.getAdapterPosition();

            if (currentPosition == selectedPosition) {
                selectedPosition = -1;
            } else {
                selectedPosition = currentPosition;
            }

            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition);
            }
        });

        // Update button click listener
        if (holder.updateButton != null) {
            holder.updateButton.setOnClickListener(v -> {
                onUpdate.accept(item);
                resetSelection();
            });
        }

        // Delete button click listener
        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                onDelete.accept(item.getItemName());
                resetSelection();
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Reset selected position and refresh the adapter
    public void resetSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
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