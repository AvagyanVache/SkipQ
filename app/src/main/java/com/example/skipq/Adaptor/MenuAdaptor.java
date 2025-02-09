package com.example.skipq.Adaptor;

import android.content.Context;
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
import com.example.skipq.MainActivity;
import com.example.skipq.R;

import java.text.MessageFormat;
import java.util.ArrayList;

public class MenuAdaptor extends RecyclerView.Adapter<MenuAdaptor.ViewHolder> {

    private Context context;
    private ArrayList<MenuDomain> menuList;
    private OnAddToCartListener onAddToCartListener;

    public interface OnAddToCartListener {
        void onAddToCart(MenuDomain menuItem);
        void onItemAdded(MenuDomain item);
    }
    public void updateList(ArrayList<MenuDomain> newList) {
        this.menuList.clear();
        this.menuList.addAll(newList);
        notifyDataSetChanged();
    }


    public MenuAdaptor(Context context, ArrayList<MenuDomain> menuList, OnAddToCartListener onAddToCartListener) {
        this.context = context;
        this.menuList = menuList;
        this.onAddToCartListener = onAddToCartListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_restaurant_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain menuItem = menuList.get(position);

        holder.menuItemTitle.setText(menuItem.getItemName());
        holder.menuItemDescription.setText(menuItem.getItemDescription());
        holder.menuItemPrice.setText(MessageFormat.format("֏ {0}", menuItem.getItemPrice()));

        Glide.with(context)
                .load(menuItem.getItemImg())
                .into(holder.menuItemPhoto);

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
        return menuList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView menuItemTitle;
        ImageView menuItemPhoto;
        TextView menuItemDescription;
        TextView menuItemPrice;
        TextView itemCount;
        ImageView plusButton, minusButton;
        View addToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
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
