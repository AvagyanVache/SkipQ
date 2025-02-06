package com.example.skipq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

 @Nullable
 @Override
 public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
  View view = inflater.inflate(R.layout.fragment_cart, container, false);

  recyclerView = view.findViewById(R.id.cartRecycleView);
  totalPriceTextView = view.findViewById(R.id.totalPrice);

  cartList = new ArrayList<>(CartManager.getInstance().getCartList());
  cartAdaptor = new CartAdaptor(requireContext(), cartList, this);

  recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
  recyclerView.setAdapter(cartAdaptor);

  updateTotalPrice(CartManager.getInstance().getTotalPrice());

  return view;
 }

 @Override
 public void onCartUpdated(double total) {
  updateTotalPrice(total);
 }

 private void updateTotalPrice(double total) {
  totalPriceTextView.setText(String.format("%.2f֏", total));
 }

 public void refreshCart() {
  cartList.clear();
  cartList.addAll(CartManager.getInstance().getCartList());
  cartAdaptor.notifyDataSetChanged();
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
 }
}
