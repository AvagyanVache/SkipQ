package com.example.skipq.Adaptor;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.R;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OrderManagementAdapter extends RecyclerView.Adapter<OrderManagementAdapter.ViewHolder> {
    private List<Map<String, Object>> orders;
    private Consumer<String> onAccept;
    private Consumer<String> onDecline;

    public OrderManagementAdapter(List<Map<String, Object>> orders, Consumer<String> onAccept, Consumer<String> onDecline) {
        this.orders = orders;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> order = orders.get(position);
        String orderId = (String) order.get("orderId");

        // Log the order data to debug
        Log.d("OrderManagementAdapter", "Order data: " + order.toString());

        // Extract order details with type-safe handling
        String restaurantName = (String) order.get("restaurantName");
        Object priceObj = order.get("totalPrice");
        Double totalPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : null;
        Object prepTimeObj = order.get("totalPrepTime");
        Integer totalPrepTime = prepTimeObj instanceof Number ? ((Number) prepTimeObj).intValue() : null;
        String restaurantImage = (String) order.get("restaurantImage");

        holder.restaurantName.setText(restaurantName != null ? restaurantName : "Unknown Restaurant");
        holder.orderPrice.setText(totalPrice != null ? String.format("%.2f֏", totalPrice) : "0.00֏");
        holder.prepTime.setText(totalPrepTime != null ? totalPrepTime + " mins" : "0 mins");

        // Load restaurant image if available
        if (restaurantImage != null && !restaurantImage.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(restaurantImage)
                    .into(holder.restaurantPhoto);
        }

        // Accept button
        holder.acceptOrder.setOnClickListener(v -> onAccept.accept(orderId));

        // Decline button
        holder.declineOrder.setOnClickListener(v -> onDecline.accept(orderId));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName, orderPrice, prepTime;
        ImageView restaurantPhoto, acceptOrder, declineOrder;

        ViewHolder(View view) {
            super(view);
            restaurantName = view.findViewById(R.id.YourOrderRestaurantTitle);
            orderPrice = view.findViewById(R.id.RestaurantOrderPrice);
            prepTime = view.findViewById(R.id.PrepTime);
            restaurantPhoto = view.findViewById(R.id.YourOrderRestaurantPhoto);
            acceptOrder = view.findViewById(R.id.AcceptOrder);
            declineOrder = view.findViewById(R.id.DeclineOrder);
        }
    }
}