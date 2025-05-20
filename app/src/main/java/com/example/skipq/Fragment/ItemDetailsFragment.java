package com.example.skipq.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.skipq.Activity.MainActivity;
import com.example.skipq.CartManager;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;

public class ItemDetailsFragment extends Fragment {

    private MenuDomain menuItem;
    private TextView itemCountTextView;
    private int itemCount = 0;
    private LinearLayout counterLayout;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_item_info, container, false);

        // Retrieve the MenuDomain object from arguments
        if (getArguments() != null) {
            menuItem = getArguments().getParcelable("menuItem");
        }

        if (menuItem == null) {
            Toast.makeText(getContext(), "Error loading item details", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }

        // Initialize views
        ImageView backButton = view.findViewById(R.id.backBtn);
        ImageView itemImage = view.findViewById(R.id.image_image);
        TextView itemName = view.findViewById(R.id.Menu_Item_Name);
        TextView itemPrice = view.findViewById(R.id.Menu_Item_Price);
        TextView itemPrepTime = view.findViewById(R.id.Item_Menu_PrepTime);
        TextView itemDescription = view.findViewById(R.id.Item_Menu_Description);
        ImageView plusButton = view.findViewById(R.id.ItemPlus);
        ImageView minusButton = view.findViewById(R.id.ItemMinus);
        itemCountTextView = view.findViewById(R.id.MenuItem_ItemCount);
        Button addToCartButton = view.findViewById(R.id.addToCart);
        counterLayout = view.findViewById(R.id.linearLayout8);

        // Populate views with item data
        itemName.setText(menuItem.getItemName() != null ? menuItem.getItemName() : "N/A");
        itemDescription.setText(menuItem.getItemDescription() != null ? menuItem.getItemDescription() : "No description");
        double price = 0.0;
        try {
            price = Double.parseDouble(menuItem.getItemPrice() != null ? menuItem.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        itemPrice.setText(String.format("֏ %.2f", price));
        itemPrepTime.setText(menuItem.getPrepTime() > 0 ? String.format("%d min", menuItem.getPrepTime()) : "Not specified");

        if (menuItem.getItemImg() != null && !menuItem.getItemImg().isEmpty() && menuItem.getItemImg().startsWith("http")) {
            Glide.with(requireContext())
                    .load(menuItem.getItemImg())
                    .centerCrop()
                    .transform(new RoundedCorners(48))
                    .into(itemImage);
        }

        // Set initial item count
        itemCountTextView.setText(String.valueOf(itemCount));

        // Plus button click listener
        plusButton.setOnClickListener(v -> {
            itemCount++;
            itemCountTextView.setText(String.valueOf(itemCount));
        });

        minusButton.setOnClickListener(v -> {
            if (itemCount > 1) {
                itemCount--;
                itemCountTextView.setText(String.valueOf(itemCount));
            } else if (itemCount == 1) {
                itemCount--;
                itemCountTextView.setText(String.valueOf(itemCount));

            }
        });

        addToCartButton.setOnClickListener(v -> {
            if (CartManager.getInstance().getSelectedAddress() == null) {
                Toast.makeText(getContext(), "Please select a restaurant address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (itemCount < 1) {
                Toast.makeText(getContext(), "Please select at least 1 item", Toast.LENGTH_SHORT).show();
                return;
            }

            itemCount = 1;
            itemCountTextView.setText(String.valueOf(itemCount));

            MenuDomain cartItem = new MenuDomain();
            cartItem.setItemName(menuItem.getItemName());
            cartItem.setItemDescription(menuItem.getItemDescription());
            cartItem.setItemPrice(menuItem.getItemPrice());
            cartItem.setItemImg(menuItem.getItemImg());
            cartItem.setRestaurantId(menuItem.getRestaurantId());
            cartItem.setPrepTime(menuItem.getPrepTime());
            cartItem.setItemCount(itemCount);
            cartItem.setAvailable(true);

            // Check if item already exists in cart
            boolean itemExists = false;
            for (MenuDomain existingItem : CartManager.getInstance().getCartList()) {
                if (existingItem.getItemName().equals(cartItem.getItemName())) {
                    itemExists = true;
                    existingItem.setItemCount(existingItem.getItemCount() + itemCount);
                    break;
                }
            }

            if (!itemExists) {
                CartManager.getInstance().addToCart(cartItem);
            }

            Toast.makeText(getContext(), "Added " + cartItem.getItemName() + " to cart", Toast.LENGTH_SHORT).show();

            // Update cart in MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateCart();
            }

            // Refresh CartFragment if active
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName()) != null) {
                    CartFragment cartFragment = (CartFragment) activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName());
                    if (cartFragment != null) {
                        cartFragment.refreshCart();
                    }
                }
            }

        });

        // Back button click listener
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }
}