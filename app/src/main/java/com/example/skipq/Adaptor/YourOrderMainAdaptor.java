package com.example.skipq.Adaptor;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
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
        // Scale UI elements based on screen width
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int desiredWidth = (int) (screenWidth * 0.65); // 65% of screen width
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(desiredWidth, RecyclerView.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((screenWidth - desiredWidth) / 2, 16, (screenWidth - desiredWidth) / 2, 16); // Center horizontally
        view.setLayoutParams(layoutParams);
        float density = displayMetrics.density;
        float scaleFactor = screenWidth / (360 * density); // Reference width: 360dp (typical phone)

        // Scale mainLayout (ConstraintLayout)
        ConstraintLayout mainLayout = view.findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            int basePaddingPx = (int) (4 * density); // Base padding: 4dp
            int scaledPaddingPx = (int) (basePaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            mainLayout.setPadding(scaledPaddingPx, scaledPaddingPx, scaledPaddingPx, scaledPaddingPx);
        }

        // Scale background ImageView (imageView)
        ImageView backgroundImage = view.findViewById(R.id.imageView);
        if (backgroundImage != null) {
            int baseHeightPx = (int) (94 * density); // Base height: 94dp
            int scaledHeightPx = (int) (baseHeightPx * Math.min(scaleFactor, 1.5)); // Cap at 1.5x
            int baseMarginPx = (int) (4 * density); // Base margin: 4dp (top, bottom)
            int scaledMarginPx = (int) (baseMarginPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) backgroundImage.getLayoutParams();
            params.height = scaledHeightPx;
            params.topMargin = scaledMarginPx;
            params.bottomMargin = scaledMarginPx;
            backgroundImage.setLayoutParams(params);
        }

        // Scale restaurant photo ImageView (YourOrderRestaurantPhoto)
        ImageView restaurantPhoto = view.findViewById(R.id.YourOrderRestaurantPhoto);
        if (restaurantPhoto != null) {
            int baseWidthPx = (int) (127 * density); // Base width: 127dp
            int scaledWidthPx = (int) (baseWidthPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseHeightPx = (int) (87 * density); // Base height: 87dp
            int scaledHeightPx = (int) (baseHeightPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseMarginPx = (int) (5 * density); // Base margin: 5dp
            int scaledMarginPx = (int) (baseMarginPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) restaurantPhoto.getLayoutParams();
            params.width = scaledWidthPx;
            params.height = scaledHeightPx;
            params.leftMargin = scaledMarginPx;
            params.topMargin = scaledMarginPx;
            params.rightMargin = scaledMarginPx;
            params.bottomMargin = scaledMarginPx;
            restaurantPhoto.setLayoutParams(params);
        }

        // Scale restaurant title TextView (YourOrderRestaurantTitle)
        TextView restaurantTitle = view.findViewById(R.id.YourOrderRestaurantTitle);
        if (restaurantTitle != null) {
            float baseTextSizeSp = 15; // Base size: 15sp
            float scaledTextSizeSp = baseTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
            int baseMarginTopPx = (int) (16 * density); // Base margin: 16dp
            int scaledMarginTopPx = (int) (baseMarginTopPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseMarginEndPx = (int) (16 * density); // Base margin: 16dp
            int scaledMarginEndPx = (int) (baseMarginEndPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            restaurantTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSizeSp);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) restaurantTitle.getLayoutParams();
            params.topMargin = scaledMarginTopPx;
            params.rightMargin = scaledMarginEndPx;
            restaurantTitle.setLayoutParams(params);
        }

        // Scale info LinearLayout (infoLayout)
        LinearLayout infoLayout = view.findViewById(R.id.infoLayout);
        if (infoLayout != null) {
            int basePaddingPx = (int) (8 * density); // Base padding: 8dp
            int scaledPaddingPx = (int) (basePaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseMarginTopPx = (int) (8 * density); // Base margin: 8dp
            int scaledMarginTopPx = (int) (baseMarginTopPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            infoLayout.setPadding(scaledPaddingPx, scaledPaddingPx, scaledPaddingPx, scaledPaddingPx);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) infoLayout.getLayoutParams();
            params.topMargin = scaledMarginTopPx;
            infoLayout.setLayoutParams(params);
        }

        // Scale order price TextView (RestaurantOrderPrice)
        TextView orderPrice = view.findViewById(R.id.RestaurantOrderPrice);
        if (orderPrice != null) {
            float baseTextSizeSp = 14; // Base size: 14sp
            float scaledTextSizeSp = baseTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
            int baseMarginStartPx = (int) (16 * density); // Base margin: 16dp
            int scaledMarginStartPx = (int) (baseMarginStartPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            orderPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSizeSp);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) orderPrice.getLayoutParams();
            params.leftMargin = scaledMarginStartPx;
            orderPrice.setLayoutParams(params);
        }

        // Scale timer ImageView (imageView8)
        ImageView timerImage = view.findViewById(R.id.imageView8);
        if (timerImage != null) {
            int baseWidthPx = (int) (20 * density); // Base width: 20dp
            int scaledWidthPx = (int) (baseWidthPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseHeightPx = (int) (17 * density); // Base height: 17dp
            int scaledHeightPx = (int) (baseHeightPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseMarginStartPx = (int) (16 * density); // Base margin: 16dp
            int scaledMarginStartPx = (int) (baseMarginStartPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) timerImage.getLayoutParams();
            params.width = scaledWidthPx;
            params.height = scaledHeightPx;
            params.leftMargin = scaledMarginStartPx;
            timerImage.setLayoutParams(params);
        }

        // Scale prep time TextView (PrepTime)
        TextView prepTime = view.findViewById(R.id.PrepTime);
        if (prepTime != null) {
            float baseTextSizeSp = 14; // Base size: 14sp
            float scaledTextSizeSp = baseTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
            int baseMarginStartPx = (int) (8 * density); // Base margin: 8dp
            int scaledMarginStartPx = (int) (baseMarginStartPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            prepTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSizeSp);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) prepTime.getLayoutParams();
            params.leftMargin = scaledMarginStartPx;
            prepTime.setLayoutParams(params);
        }
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