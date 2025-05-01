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
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

import java.text.MessageFormat;
import java.util.ArrayList;

public class YourOrderAdaptor extends RecyclerView.Adapter<YourOrderAdaptor.ViewHolder> {
    private Context context;
    private ArrayList<MenuDomain> orderList;

    public YourOrderAdaptor(Context context, ArrayList<MenuDomain> orderList) {
        this.context = context;
        this.orderList = orderList;
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
        holder.itemName.setText(item.getItemName());
        holder.itemPrepTime.setText(MessageFormat.format("{0} min", item.getPrepTime()));

        holder.itemDescription.setText(item.getItemDescription());
        holder.itemCount.setText(String.valueOf(item.getItemCount()));
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        holder.itemPrice.setText(String.format("%.2f֏", price));
        if (item.getItemImg() != null && !item.getItemImg().isEmpty()) {
            Glide.with(context)
                    .load(item.getItemImg())
                    .into(holder.cartItemImage);
        } else {
            holder.cartItemImage.setImageResource(R.drawable.white);
        }
    }




    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemPrice, itemDescription, itemPrepTime, itemCount;;
        ImageView cartItemImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cartItemImage = itemView.findViewById(R.id.CartItemPhoto);
            itemPrepTime = itemView.findViewById(R.id.PrepTime);
            itemDescription= itemView.findViewById(R.id.CartItemDescription);
            itemCount = itemView.findViewById(R.id.CartItemCount);
            itemName = itemView.findViewById(R.id.CartItemTitle);
            itemPrice = itemView.findViewById(R.id.CartItemPrice);
        }
    }

}

