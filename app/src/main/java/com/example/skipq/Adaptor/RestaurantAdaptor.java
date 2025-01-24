package com.example.skipq.Adaptor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.R;

import java.util.ArrayList;

public class RestaurantAdaptor extends RecyclerView.Adapter<RestaurantAdaptor.ViewHolder> {

    private final ArrayList<RestaurantDomain> restaurantDomains;
    private final Context context;

    public RestaurantAdaptor(Context context, ArrayList<RestaurantDomain> restaurantDomains) {
        this.restaurantDomains = restaurantDomains;
        this.context = context;
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
        holder.restaurantImage.setImageResource(restaurant.getImage());
    }

    @Override
    public int getItemCount() {
        return restaurantDomains.size();
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
