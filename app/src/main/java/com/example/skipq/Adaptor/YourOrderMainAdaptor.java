package com.example.skipq.Adaptor;

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
import com.example.skipq.Domain.YourOrderMainDomain;
import com.example.skipq.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class YourOrderMainAdaptor extends RecyclerView.Adapter<YourOrderMainAdaptor.ViewHolder> {
    private Context context;
    private ArrayList<YourOrderMainDomain> orderList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public YourOrderMainAdaptor(Context context, ArrayList<YourOrderMainDomain> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.your_order_viewholder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YourOrderMainDomain order = orderList.get(position);
        holder.restaurantName.setText(order.getRestaurant().getName() != null ? order.getRestaurant().getName() : "N/A");
        holder.orderPrice.setText(String.format("֏ %.2f", order.getTotalPrice()));
        holder.totalPrepTime.setText(order.getTotalPrepTime() >= 0 ? String.format("%d min", order.getTotalPrepTime()) : "N/A");

        String restaurantName = order.getRestaurant().getName();
        if (restaurantName != null && !restaurantName.isEmpty()) {
            String imagePath = "restaurant_logos/" + restaurantName.replaceAll("[^a-zA-Z0-9]", "_") + "_logo.jpg";
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);
            storageReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(context)
                                .load(uri.toString())
                                .placeholder(R.drawable.white)
                                .centerCrop()
                                .error(R.drawable.white)
                                .into(holder.restaurantImage);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("YourOrderMainAdaptor", "Failed to load restaurant logo: " + e.getMessage());
                        holder.restaurantImage.setImageResource(R.drawable.white);
                    });
        } else {
            holder.restaurantImage.setImageResource(R.drawable.white);
        }
        holder.restaurantImage.setClipToOutline(true);
        holder.restaurantImage.setBackgroundResource(R.drawable.rounded_corners);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName, orderPrice, totalPrepTime;
        ImageView restaurantImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.YourOrderRestaurantTitle);
            orderPrice = itemView.findViewById(R.id.RestaurantOrderPrice);
            restaurantImage = itemView.findViewById(R.id.YourOrderRestaurantPhoto);
            totalPrepTime = itemView.findViewById(R.id.PrepTime);
        }
    }
}