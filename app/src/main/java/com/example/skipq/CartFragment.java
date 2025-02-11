package com.example.skipq;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.CartAdaptor;
import com.example.skipq.Domain.MenuDomain;

import java.util.ArrayList;

public class CartFragment extends Fragment implements CartAdaptor.OnCartUpdatedListener {

 private RecyclerView recyclerView;
 private CartAdaptor cartAdaptor;
 private TextView totalPriceTextView;
 private ArrayList<MenuDomain> cartList;
 public ImageView profileIcon;

 @Override
 public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
  View view = inflater.inflate(R.layout.fragment_cart, container, false);

  CartManager.getInstance().setListener(this);

  recyclerView = view.findViewById(R.id.cartRecycleView);
  totalPriceTextView = view.findViewById(R.id.totalPrice);
  profileIcon = view.findViewById(R.id.profileIcon);

  profileIcon.setOnClickListener(v -> {
   ProfileFragment profileFragment = new ProfileFragment();
   requireActivity().getSupportFragmentManager()
           .beginTransaction()
           .replace(R.id.frame_layout, profileFragment)
           .addToBackStack(null)
           .commit();
  });

  cartList = new ArrayList<>(CartManager.getInstance().getCartList());
  cartAdaptor = new CartAdaptor(requireContext(), cartList, this);

  recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
  recyclerView.setAdapter(cartAdaptor);

  double initialTotalPrice = CartManager.getInstance().getTotalPrice();
  int initialPrepTime = CartManager.getInstance().getTotalPrepTime();

  updateTotalPrice(initialTotalPrice);
  updateTimeTillReady(initialPrepTime);

  recyclerView.post(() -> {
   updateTotalPrice(CartManager.getInstance().getTotalPrice());
   updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());
  });

  return view;
 }

 @Override
 public void onCartUpdated(double total, int totalPrepTime) {

  updateTotalPrice(total);
  updateTimeTillReady(totalPrepTime);
 }


 private void updateTimeTillReady(int totalPrepTime) {
  if (getView() != null) {

   TextView timeTillReadyTextView = getView().findViewById(R.id.TimeTillReady);
   timeTillReadyTextView.setText(totalPrepTime + " min");
   Log.d("CartFragment", "Updated Prep Time: " + totalPrepTime);

  }
 }

 private void updateTotalPrice(double total) {
  totalPriceTextView.setText(String.format("%.2f֏", total));
 }

 public void refreshCart() {
  cartList.clear();
  cartList.addAll(CartManager.getInstance().getCartList());
  cartAdaptor.notifyDataSetChanged();
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
  updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());
 }
}
