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
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.R;

import java.util.ArrayList;

public class RestaurantAdaptor extends RecyclerView.Adapter<RestaurantAdaptor.ViewHolder> {

    private final ArrayList<RestaurantDomain> restaurantDomains;
    private final Context context;
    private final OnItemClickListener listener;
    private ArrayList<RestaurantDomain> restaurantList;


    public interface OnItemClickListener {
        void onItemClick(RestaurantDomain restaurant);
    }

    public RestaurantAdaptor(Context context, ArrayList<RestaurantDomain> restaurantDomains, OnItemClickListener listener) {
        this.restaurantDomains = restaurantDomains;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.viewholder_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RestaurantDomain restaurant = restaurantDomains.get(position);

        holder.restaurantName.setText(restaurant.getName());


        holder.itemView.setOnClickListener(v -> listener.onItemClick(restaurant));
        Glide.with(context)
                .load(restaurant.getImageUrl())
                .into(holder.restaurantImage);
    }

    @Override
    public int getItemCount() {
        return restaurantDomains.size();
    }
    public void updateList(ArrayList<RestaurantDomain> newList) {
        this.restaurantDomains.clear();
        this.restaurantDomains.addAll(newList);
        notifyDataSetChanged();
    }




    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName;
        ImageView restaurantImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.RestaurantTextView);
            restaurantImage = itemView.findViewById(R.id.RestaurantImageView);
        }
    }
}
