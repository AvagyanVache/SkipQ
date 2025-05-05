package com.example.skipq.Adaptor;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        String restaurantName = (String) order.get("restaurantName");

        holder.restaurantName.setText(restaurantName != null ? restaurantName : "Unknown Restaurant");

        if (restaurantName != null && !restaurantName.isEmpty()) {
            String imagePath = "restaurant_logos/" + restaurantName.replaceAll("[^a-zA-Z0-9]", "_") + "_logo.jpg";
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);
            storageReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(holder.itemView.getContext())
                                .load(uri.toString())
                                .placeholder(R.drawable.white)
                                .error(R.drawable.white)
                                .centerCrop()
                                .into(holder.restaurantPhoto);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OrderManagementAdapter", "Failed to load restaurant logo: " + e.getMessage());
                        holder.restaurantPhoto.setImageResource(R.drawable.white);
                    });
        } else {
            holder.restaurantPhoto.setImageResource(R.drawable.white);
        }
        holder.restaurantPhoto.setClipToOutline(true);
        holder.restaurantPhoto.setBackgroundResource(R.drawable.rounded_corners);
        holder.acceptOrder.setOnClickListener(v -> onAccept.accept(orderId));
        holder.declineOrder.setOnClickListener(v -> onDecline.accept(orderId));
        holder.itemView.setOnClickListener(v -> showOrderDetailsDialog(holder.itemView.getContext(), order));
    }

    private void showOrderDetailsDialog(Context context, Map<String, Object> order) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_order_details, null);

        TextView totalPriceView = dialogView.findViewById(R.id.totalPrice);
        TextView prepTimeView = dialogView.findViewById(R.id.prepTime);
        TextView orderTypeView = dialogView.findViewById(R.id.orderType);
        TextView customerCountView = dialogView.findViewById(R.id.customerCount);
        TextView itemsListView = dialogView.findViewById(R.id.itemsList);

        Object priceObj = order.get("totalPrice");
        Double totalPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0;
        Object prepTimeObj = order.get("totalPrepTime");
        Integer totalPrepTime = prepTimeObj instanceof Number ? ((Number) prepTimeObj).intValue() : 0;
        String orderType = (String) order.get("orderType");
        Object customerCountObj = order.get("customerCount");
        Integer customerCount = customerCountObj instanceof Number ? ((Number) customerCountObj).intValue() : 0;
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");

        totalPriceView.setText(String.format("Total Price: ֏ %.2f", totalPrice));
        prepTimeView.setText(String.format("Prep Time: %d min", totalPrepTime));
        orderTypeView.setText(orderType != null ? "Order Type: " + orderType : "Order Type: Unknown");

        if (orderType != null && orderType.equals("Eat In") && customerCount > 0) {
            customerCountView.setText("Customers: " + customerCount);
            customerCountView.setVisibility(View.VISIBLE);
        } else {
            customerCountView.setVisibility(View.GONE);
        }

        if (items != null && !items.isEmpty()) {
            StringBuilder itemsText = new StringBuilder();
            for (Map<String, Object> item : items) {
                String name = (String) item.get("name");
                Object priceObjItem = item.get("price");
                Double price = priceObjItem instanceof Number ? ((Number) priceObjItem).doubleValue() : 0.0;
                Object countObj = item.get("itemCount");
                Integer count = countObj instanceof Number ? ((Number) countObj).intValue() : 1;
                String description = (String) item.get("description");
                String image = (String) item.get("image");
                Object prepTimeItemObj = item.get("prepTime");
                Integer prepTime = prepTimeItemObj instanceof Number ? ((Number) prepTimeItemObj).intValue() : 0;
                Boolean available = (Boolean) item.get("available");

                if (name != null) {
                    itemsText.append(name).append(" x").append(count);
                    if (price > 0) {
                        itemsText.append(" (֏ ").append(String.format("%.2f", price)).append(" each)");
                    }
                    if (description != null && !description.isEmpty()) {
                        itemsText.append("\nDescription: ").append(shortenDescription(description));
                    }
                    if (prepTime > 0) {
                        itemsText.append("\nPrep Time: ").append(prepTime).append(" min");
                    }
                    if (available != null) {
                        itemsText.append("\nAvailability: ").append(available ? "Available" : "Unavailable");
                    }
                    if (image != null && !image.isEmpty()) {
                        itemsText.append("\nImage: Available");
                    }
                    itemsText.append("\n\n");
                }
            }
            itemsListView.setText(itemsText.toString().trim());
        } else {
            itemsListView.setText("No items");
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Order Details")
                .setView(dialogView)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .create();
        dialog.show();
    }

    private String shortenDescription(String description) {
        if (description.isEmpty()) {
            return "No description";
        }
        String[] words = description.split("\\s+");
        int wordCount = Math.min(words.length, 3);
        StringBuilder shortDesc = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            shortDesc.append(words[i]);
            if (i < wordCount - 1) {
                shortDesc.append(" ");
            }
        }
        if (words.length > 3) {
            shortDesc.append("...");
        }
        return shortDesc.toString();
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName;
        ImageView restaurantPhoto, acceptOrder, declineOrder;

        ViewHolder(View view) {
            super(view);
            restaurantName = view.findViewById(R.id.YourOrderRestaurantTitle);
            restaurantPhoto = view.findViewById(R.id.YourOrderRestaurantPhoto);
            acceptOrder = view.findViewById(R.id.AcceptOrder);
            declineOrder = view.findViewById(R.id.DeclineOrder);
        }
    }
}