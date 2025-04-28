package com.example.skipq.Adaptor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
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

public class MenuAdaptor extends RecyclerView.Adapter<MenuAdaptor.ViewHolder> {
    private Context context;
    private ArrayList<MenuDomain> menuList;
    private ArrayList<MenuDomain> filteredMenuList;
    private OnAddToCartListener onAddToCartListener;

    public interface OnAddToCartListener {
        void onAddToCart(MenuDomain menuItem);
        void onItemAdded(MenuDomain item);
    }

    public MenuAdaptor(Context context, ArrayList<MenuDomain> menuList, OnAddToCartListener onAddToCartListener) {
        this.context = context;
        this.menuList = menuList != null ? new ArrayList<>(menuList) : new ArrayList<>();
        this.onAddToCartListener = onAddToCartListener;
        this.filteredMenuList = new ArrayList<>();
        filterItems();
        Log.d("MenuAdaptor", "Initialized with " + this.menuList.size() + " items, " + filteredMenuList.size() + " filtered");
    }

    private void filterItems() {
        filteredMenuList.clear();
        for (MenuDomain item : menuList) {
            if (item != null && item.isAvailable()) {
                filteredMenuList.add(item);
                Log.d("MenuAdaptor", "Added to filtered: " + item.getItemName());
            } else {
                Log.d("MenuAdaptor", "Skipped item: " + (item != null ? item.getItemName() : "null") + ", Available: " + (item != null ? item.isAvailable() : "null"));
            }
        }
        if (filteredMenuList.isEmpty()) {
            Log.w("MenuAdaptor", "Filtered list is empty");
        }
    }

    public void updateList(ArrayList<MenuDomain> newList) {
        this.menuList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        filterItems();
        notifyDataSetChanged();
        Log.d("MenuAdaptor", "Updated list: " + menuList.size() + " items, " + filteredMenuList.size() + " filtered");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_restaurant_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain menuItem = filteredMenuList.get(position);

        holder.menuItemTitle.setText(menuItem.getItemName() != null ? menuItem.getItemName() : "");
        holder.menuItemDescription.setText(menuItem.getItemDescription() != null ? menuItem.getItemDescription() : "");
        holder.menuItemPrice.setText(MessageFormat.format("֏ {0}", menuItem.getItemPrice() != null ? menuItem.getItemPrice() : "0"));

        String imageData = menuItem.getItemImg();
        if (imageData != null && !imageData.isEmpty()) {
            if (imageData.startsWith("http")) {
                Glide.with(context)
                        .load(imageData)
                        .placeholder(R.drawable.white)
                        .into(holder.menuItemPhoto);
                holder.menuItemPhoto.setVisibility(View.VISIBLE);
            } else {
                try {
                    byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    holder.menuItemPhoto.setImageBitmap(bitmap);
                    holder.menuItemPhoto.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e("MenuAdaptor", "Failed to decode Base64 image for item: " + menuItem.getItemName(), e);
                    holder.menuItemPhoto.setImageResource(R.drawable.white);
                    holder.menuItemPhoto.setVisibility(View.VISIBLE);
                }
            }
        } else {
            holder.menuItemPhoto.setImageResource(R.drawable.white);
            holder.menuItemPhoto.setVisibility(View.VISIBLE);
        }

        holder.prepTime.setText(MessageFormat.format("{0} min", menuItem.getPrepTime()));
        holder.itemCount.setText(String.valueOf(menuItem.getItemCount()));
        holder.addToCart.setOnClickListener(v -> {
            if (menuItem.getItemCount() > 0) {
                onAddToCartListener.onAddToCart(menuItem);
            }
        });

        holder.plusButton.setOnClickListener(v -> {
            menuItem.setItemCount(menuItem.getItemCount() + 1);
            holder.itemCount.setText(String.valueOf(menuItem.getItemCount()));
            notifyItemChanged(position);
            onAddToCartListener.onItemAdded(menuItem);
        });

        holder.minusButton.setOnClickListener(v -> {
            if (menuItem.getItemCount() > 0) {
                menuItem.setItemCount(menuItem.getItemCount() - 1);
                holder.itemCount.setText(String.valueOf(menuItem.getItemCount()));
                notifyItemChanged(position);
                onAddToCartListener.onItemAdded(menuItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredMenuList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView menuItemTitle, menuItemDescription, menuItemPrice, itemCount, prepTime;
        ImageView menuItemPhoto, plusButton, minusButton;
        View addToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            prepTime = itemView.findViewById(R.id.PrepTime);
            menuItemTitle = itemView.findViewById(R.id.MenuItemTitle);
            menuItemPhoto = itemView.findViewById(R.id.MenuItemPhoto);
            menuItemDescription = itemView.findViewById(R.id.MenuItemDescription);
            menuItemPrice = itemView.findViewById(R.id.MenuItemPrice);
            itemCount = itemView.findViewById(R.id.ItemCount);
            plusButton = itemView.findViewById(R.id.ItemPlus);
            minusButton = itemView.findViewById(R.id.ItemMinus);
            addToCart = itemView.findViewById(R.id.AddToCart);
        }
    }
}