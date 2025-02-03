package com.example.skipq.Adaptor;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.ItemDetailActivity;
import com.example.skipq.R;

import java.text.MessageFormat;
import java.util.ArrayList;

public class MenuAdaptor extends RecyclerView.Adapter<MenuAdaptor.ViewHolder> {
    Context context;
    ArrayList<MenuDomain> arrayListMenu;

    public MenuAdaptor(Context context, ArrayList<MenuDomain> arrayListMenu) {
        this.arrayListMenu = arrayListMenu;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_restaurant_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuDomain menuItem = arrayListMenu.get(position);

        holder.MenuItemTitle.setText(menuItem.getItemName());
        holder.MenuItemDescription.setText(menuItem.getItemDescription());
        holder.MenuItemPrice.setText(MessageFormat.format("$ {0}", menuItem.getItemPrice()));

        Glide.with(context)
                .load(menuItem.getItemImg())
                .into(holder.MenuItemPhoto);

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Clicked on " + menuItem.getItemName(), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra("Item Name", menuItem.getItemName());
            intent.putExtra("Item Description", menuItem.getItemDescription());
            intent.putExtra("Item Price", menuItem.getItemPrice());
            intent.putExtra("Item Img", menuItem.getItemImg());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return arrayListMenu.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView MenuItemTitle;
        ImageView MenuItemPhoto;
        TextView MenuItemDescription;
        TextView MenuItemPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            MenuItemTitle = itemView.findViewById(R.id.MenuItemTitle);
            MenuItemPhoto = itemView.findViewById(R.id.MenuItemPhoto);
            MenuItemDescription = itemView.findViewById(R.id.MenuItemDescription);
            MenuItemPrice = itemView.findViewById(R.id.MenuItemPrice);
        }

    }
}
