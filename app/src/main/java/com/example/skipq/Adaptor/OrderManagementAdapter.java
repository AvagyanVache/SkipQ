package com.example.skipq.Adaptor;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.skipq.R;
import com.google.firebase.firestore.FirebaseFirestore;
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
            String imagePath = "restaurant_logos/" + restaurantName + "_logo.jpg";

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imagePath);

            storageReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(holder.itemView.getContext())
                                .load(uri.toString())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.white)
                                .error(R.drawable.white)
                                .into(holder.restaurantPhoto);
                    })
                    .addOnFailureListener(e -> {
                        holder.restaurantPhoto.setImageResource(R.drawable.white);
                    });
        } else {
            holder.restaurantPhoto.setImageResource(R.drawable.white);
        }

        holder.acceptOrder.setOnClickListener(v -> onAccept.accept(orderId));
        holder.declineOrder.setOnClickListener(v -> onDecline.accept(orderId));
        holder.itemView.setOnClickListener(v -> showOrderDetailsDialog(holder.itemView.getContext(), order));
    }


    private void showOrderDetailsDialog(android.content.Context context, Map<String, Object> order) {
        // Inflate dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_order_details, null);

        // Initialize dialog views
        TextView totalPriceView = dialogView.findViewById(R.id.totalPrice);
        TextView prepTimeView = dialogView.findViewById(R.id.prepTime);
        TextView orderTypeView = dialogView.findViewById(R.id.orderType);
        TextView customerCountView = dialogView.findViewById(R.id.customerCount);
        TextView itemsListView = dialogView.findViewById(R.id.itemsList);


        Object priceObj = order.get("totalPrice");
        Double totalPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : null;
        Object prepTimeObj = order.get("totalPrepTime");
        Integer totalPrepTime = prepTimeObj instanceof Number ? ((Number) prepTimeObj).intValue() : null;
        String orderType = (String) order.get("orderType");
        Object customerCountObj = order.get("customerCount");
        Integer customerCount = customerCountObj instanceof Number ? ((Number) customerCountObj).intValue() : null;
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");

        // Populate dialog
        totalPriceView.setText(totalPrice != null ? String.format("Total Price: %.2f֏", totalPrice) : "Total Price: 0.00֏");
        prepTimeView.setText(totalPrepTime != null ? "Prep Time: " + totalPrepTime + " mins" : "Prep Time: 0 mins");
        orderTypeView.setText(orderType != null ? "Order Type: " + orderType : "Order Type: Unknown");

        // Handle customer count
        if (orderType != null && orderType.equals("Eat In") && customerCount != null) {
            customerCountView.setText("Customers: " + customerCount);
            customerCountView.setVisibility(View.VISIBLE);
        } else {
            customerCountView.setVisibility(View.GONE);
        }

        // Populate items list
        if (items != null && !items.isEmpty()) {
            StringBuilder itemsText = new StringBuilder();
            for (Map<String, Object> item : items) {
                String name = (String) item.get("name");
                Object priceObjItem = item.get("price");
                Double price = priceObjItem instanceof Number ? ((Number) priceObjItem).doubleValue() : null;
                Object countObj = item.get("itemCount");
                Integer count = countObj instanceof Number ? ((Number) countObj).intValue() : null;
                if (name != null && count != null) {
                    itemsText.append(name)
                            .append(" x")
                            .append(count);
                    if (price != null) {
                        itemsText.append(" (").append(String.format("%.2f֏", price)).append(" each)");
                    }
                    itemsText.append("\n");
                }
            }
            itemsListView.setText(itemsText.toString().trim());
        } else {
            itemsListView.setText("No items");
        }

        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Order Details")
                .setView(dialogView)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .create();
        dialog.show();
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