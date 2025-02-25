package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skipq.Adaptor.CartAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment implements CartAdaptor.OnCartUpdatedListener {
 private RecyclerView recyclerView;
 private CartAdaptor cartAdaptor;
 private TextView totalPriceTextView;
 private TextView timeTillReadyTextView;
 private ArrayList<MenuDomain> cartList;
 private ImageView profileIcon;
 private Button checkOutButton;

 @Override
 public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
  View view = inflater.inflate(R.layout.fragment_cart, container, false);
  CartManager.getInstance().setListener(this);
  checkOutButton = view.findViewById(R.id.CheckOutbutton);
  recyclerView = view.findViewById(R.id.cartRecycleView);
  totalPriceTextView = view.findViewById(R.id.totalPrice);
  timeTillReadyTextView = view.findViewById(R.id.TimeTillReady);
  profileIcon = view.findViewById(R.id.profileIcon);

  profileIcon.setOnClickListener(v -> {
   Intent intent = new Intent(getActivity(), HomeActivity.class);intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
   intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent);
  });

  checkOutButton.setOnClickListener(v -> {
   proceedToOrder();
  });

  cartList = new ArrayList<>(CartManager.getInstance().getCartList());
  cartAdaptor = new CartAdaptor(requireContext(), cartList, this);
  recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
  recyclerView.setAdapter(cartAdaptor);
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
  updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());
  return view;
 }
 private void proceedToOrder() {
  if (cartList.isEmpty()) {
   Log.e("CartFragment", "Cart is empty, cannot place order.");
   return;
  }

  for (MenuDomain item : cartList) {
   if (item.getRestaurantId() == null || item.getRestaurantId().isEmpty()) {
    Log.e("CartFragment", "Item has no restaurant: " + item.getItemName());
    return;
   }
  }

  SharedPreferences sharedPreferences = getActivity().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
  SharedPreferences.Editor editor = sharedPreferences.edit();

  Gson gson = new Gson();
  String cartItemsJson = gson.toJson(cartList);
  String restaurantId = cartList.get(0).getRestaurantId();
  int prepTime = CartManager.getInstance().getTotalPrepTime();

  editor.putString("cartItems", cartItemsJson);
  editor.putString("restaurantId", restaurantId);
  editor.putFloat("totalPrice", (float) CartManager.getInstance().getTotalPrice());
  editor.putInt("prepTime", prepTime);
  editor.apply();

  saveOrderToFirestore(cartList, restaurantId, CartManager.getInstance().getTotalPrice(), prepTime);
 }

 private void saveOrderToFirestore(ArrayList<MenuDomain> cartList, String restaurantId, double totalPrice, int prepTime) {
  FirebaseFirestore db = FirebaseFirestore.getInstance();
  FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

  if (currentUser == null) {
   Log.e("FirestoreError", "User not authenticated");
   return;
  }

  String userId = currentUser.getUid();
  String orderId = db.collection("orders").document().getId();

  long currentTime = System.currentTimeMillis() / 1000;
  long endTime = currentTime + (prepTime * 60);

  Map<String, Object> orderData = new HashMap<>();
  orderData.put("orderId", orderId);
  orderData.put("userId", userId);
  orderData.put("restaurantId", restaurantId);
  orderData.put("totalPrice", totalPrice);
  orderData.put("totalPrepTime", prepTime);
  orderData.put("startTime", FieldValue.serverTimestamp());
  orderData.put("endTime", endTime);
  orderData.put("status", "pending");

  List<Map<String, Object>> itemsList = new ArrayList<>();
  for (MenuDomain item : cartList) {
   Map<String, Object> itemData = new HashMap<>();
   itemData.put("name", item.getItemName());
   itemData.put("price", Double.parseDouble(item.getItemPrice()));
   itemData.put("prepTime", item.getPrepTime());
   itemsList.add(itemData);
  }
  orderData.put("items", itemsList);

  db.collection("orders").document(orderId)
          .set(orderData)
          .addOnSuccessListener(aVoid -> {
           Log.d("FirestoreDebug", "Order stored successfully with ID: " + orderId);

           if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.putExtra("FRAGMENT_TO_LOAD", "YOUR ORDER");
            intent.putExtra("orderId", orderId);
            startActivity(intent);
           } else {
            Log.e("CartFragment", "Fragment is not attached to an activity");
           }
          })
          .addOnFailureListener(e -> {
           Log.e("FirestoreError", "Failed to store order: " + e.getMessage());
          });
 }
 @Override
 public void onCartUpdated(double total, int totalPrepTime) {
  updateTotalPrice(total);
  updateTimeTillReady(totalPrepTime);
 }

 private void updateTimeTillReady(int totalPrepTime) {
  if (timeTillReadyTextView != null) {
   timeTillReadyTextView.setText(totalPrepTime + " min");
   Log.d("CartFragment", "Updated Prep Time: " + totalPrepTime);
  }
 }

 private void updateTotalPrice(double total) {
  if (totalPriceTextView != null) {
   totalPriceTextView.setText(String.format("%.2f֏", total));
  }
 }

 public void refreshCart() {
  cartList.clear();
  cartList.addAll(CartManager.getInstance().getCartList());
  cartAdaptor.notifyDataSetChanged();
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
  updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());
 }
}