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
    private OnItemClickListener itemClickListener;
    public interface OnAddToCartListener {
        void onAddToCart(MenuDomain menuItem);
        void onItemAdded(MenuDomain item);
    }
    public interface OnItemClickListener {
        void onItemClick(MenuDomain item);
    }

    public MenuAdaptor(Context context, ArrayList<MenuDomain> menuList, OnAddToCartListener onAddToCartListener,OnItemClickListener itemClickListener) {
        this.context = context;
        this.menuList = menuList != null ? new ArrayList<>(menuList) : new ArrayList<>();
        this.onAddToCartListener = onAddToCartListener;
        this.filteredMenuList = new ArrayList<>();
        this.itemClickListener = itemClickListener;
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
        // Shorten description to 2-3 words
        String fullDescription = menuItem.getItemDescription() != null ? menuItem.getItemDescription() : "";
        String shortDescription = shortenDescription(fullDescription);
        holder.menuItemDescription.setText(shortDescription);

        double price = 0.0;
        try {
            price = Double.parseDouble(menuItem.getItemPrice() != null ? menuItem.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            Log.e("MenuAdaptor", "Invalid price format for item: " + menuItem.getItemName(), e);
        }
        holder.menuItemPrice.setText(String.format("֏ %.2f", price));
        holder.prepTime.setText(MessageFormat.format("{0} min", menuItem.getPrepTime()));
//        holder.itemCount.setText(String.format("%02d", menuItem.getItemCount()));
        // Load image from Firebase Storage URL
        if (menuItem.getItemImg() != null && !menuItem.getItemImg().isEmpty() && menuItem.getItemImg().startsWith("http")) {
            Glide.with(context)
                    .load(menuItem.getItemImg())
                    .centerCrop()
                    .into(holder.menuItemPhoto);
            holder.menuItemPhoto.setVisibility(View.VISIBLE);
        } else {
            holder.menuItemPhoto.setVisibility(View.VISIBLE);
        }

        holder.menuItemPhoto.setClipToOutline(true);
        holder.menuItemPhoto.setBackgroundResource(R.drawable.rounded_corners);

      //  holder.plusButton.setVisibility(View.VISIBLE);
       // holder.minusButton.setVisibility(View.VISIBLE);

        /*holder.addToCart.setOnClickListener(v -> {
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


         */
        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(menuItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredMenuList.size();
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
         //   itemCount = itemView.findViewById(R.id.ItemCount);
          //  plusButton = itemView.findViewById(R.id.ItemPlus);
           // minusButton = itemView.findViewById(R.id.ItemMinus);
          //  addToCart = itemView.findViewById(R.id.AddToCart);
        }
    }
}