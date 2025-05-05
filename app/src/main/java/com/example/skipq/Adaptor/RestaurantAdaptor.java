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
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        String sanitizedName = restaurant.getName().replaceAll("[^a-zA-Z0-9]", "_");

        holder.restaurantName.setText(restaurant.getName());

        String imageUrl = restaurant.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .error(R.drawable.white)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("RestaurantAdaptor", "Failed to load logo from URL: " + imageUrl, e);
                            // Fallback to Storage
                            loadLogoFromStorage(sanitizedName, holder.restaurantImage, restaurant.getName());
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.restaurantImage);
        } else {
            Log.w("RestaurantAdaptor", "imageUrl is null or empty for restaurant: " + restaurant.getName());
            loadLogoFromStorage(sanitizedName, holder.restaurantImage, restaurant.getName());
        }
        holder.restaurantImage.setClipToOutline(true);
        holder.restaurantImage.setBackgroundResource(R.drawable.rounded_corners);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(restaurant));
    }

    private void loadLogoFromStorage(String sanitizedName, ImageView imageView, String restaurantName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference logoRef = storage.getReference().child("restaurant_logos/" + sanitizedName + "_logo.jpg");
        logoRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    Glide.with(context)
                            .load(logoUrl)
                            .centerCrop()
                            .error(R.drawable.white)
                            .into(imageView);

                    FirebaseFirestore.getInstance()
                            .collection("FoodPlaces").document(restaurantName)
                            .update("logoUrl", logoUrl)
                            .addOnSuccessListener(aVoid -> Log.d("RestaurantAdaptor", "Updated logoUrl in Firestore"))
                            .addOnFailureListener(e -> Log.e("RestaurantAdaptor", "Failed to update logoUrl", e));
                })
                .addOnFailureListener(e -> {
                    Log.e("RestaurantAdaptor", "Failed to load logo from Storage: " + sanitizedName, e);
                    Glide.with(context)
                            .load(R.drawable.white)
                            .centerCrop()
                            .into(imageView);
                });
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
