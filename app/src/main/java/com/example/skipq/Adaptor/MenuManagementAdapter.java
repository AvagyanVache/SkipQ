package com.example.skipq.Adaptor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.util.List;
import java.util.function.Consumer;

public class MenuManagementAdapter extends RecyclerView.Adapter<MenuManagementAdapter.ViewHolder> {
    private List<MenuDomain> items;
    private Consumer<MenuDomain> onUpdate;
    private Consumer<String> onDelete;

    public MenuManagementAdapter(List<MenuDomain> items, Consumer<MenuDomain> onUpdate, Consumer<String> onDelete) {
        this.items = items;
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
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

        // Safely set texted with null checks
        if (holder.itemName != null) holder.itemName.setText(item.getItemName() != null ? item.getItemName() : "");
        if (holder.itemPrice != null) holder.itemPrice.setText(item.getItemPrice() != null ? item.getItemPrice() : "");
        if (holder.itemPrepTime != null) holder.itemPrepTime.setText(item.getPrepTime() >= 0 ? String.valueOf(item.getPrepTime()) : "");
        if (holder.itemDescription != null) holder.itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "");

        // Update button click listener (assumes update happens elsewhere, e.g., in a dialog)
        if (holder.updateButton != null) {
            holder.updateButton.setOnClickListener(v -> onUpdate.accept(item)); // Pass the item as-is
        }

        // Delete button click listener
        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> onDelete.accept(item.getItemName()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemPrice, itemPrepTime, itemDescription;
        ImageView updateButton, deleteButton;

        ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.item_name_text);
            itemPrice = view.findViewById(R.id.item_price_text);
            itemPrepTime = view.findViewById(R.id.item_prep_time_text);
            itemDescription = view.findViewById(R.id.item_description_text);
            updateButton = view.findViewById(R.id.EditItem);
            deleteButton = view.findViewById(R.id.DeleteItem);
        }
    }
}