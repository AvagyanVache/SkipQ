package com.example.skipq.Fragment;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import androidx.constraintlayout.widget.ConstraintLayout;
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
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = screenWidth / (360 * density); // Reference width: 360dp

        // Scale Back Button
        if (backButton != null) {
            int baseIconSizePx = (int) (50 * density); // Base size: 50dp
            int scaledIconSizePx = (int) (baseIconSizePx * Math.min(scaleFactor, 1.5f));
            int baseIconMarginStartPx = (int) (8 * density); // Base margin: 8dp
            int scaledIconMarginStartPx = (int) (baseIconMarginStartPx * Math.min(scaleFactor, 1.5f));
            int baseIconMarginTopPx = (int) (8 * density); // Base margin: 8dp
            int scaledIconMarginTopPx = (int) (baseIconMarginTopPx * Math.min(scaleFactor, 1.5f));
            ViewGroup.LayoutParams backParams = backButton.getLayoutParams();
            backParams.width = scaledIconSizePx;
            backParams.height = scaledIconSizePx;
            backButton.setLayoutParams(backParams);
            ConstraintLayout.LayoutParams backConstraintParams = (ConstraintLayout.LayoutParams) backButton.getLayoutParams();
            backConstraintParams.leftMargin = scaledIconMarginStartPx;
            backConstraintParams.topMargin = scaledIconMarginTopPx;
            backButton.setLayoutParams(backConstraintParams);
        }

        // Scale Item Image
        if (itemImage != null) {
            int baseImageMarginHorizontalPx = (int) (8 * density); // Base margin: 8dp
            int scaledImageMarginHorizontalPx = (int) (baseImageMarginHorizontalPx * Math.min(scaleFactor, 1.5f));
            int baseImageMarginTopPx = (int) (48 * density); // Base margin: 48dp
            int scaledImageMarginTopPx = (int) (baseImageMarginTopPx * Math.min(scaleFactor, 1.5f));
            ConstraintLayout.LayoutParams imageParams = (ConstraintLayout.LayoutParams) itemImage.getLayoutParams();
            imageParams.leftMargin = scaledImageMarginHorizontalPx;
            imageParams.rightMargin = scaledImageMarginHorizontalPx;
            imageParams.topMargin = scaledImageMarginTopPx;
            itemImage.setLayoutParams(imageParams);
        }

        // Scale Menu Item Name
        if (itemName != null) {
            float baseNameTextSizeSp = 25; // Base size: 18sp
            float scaledNameTextSizeSp = baseNameTextSizeSp * Math.min(scaleFactor, 1.5f);
            int baseNameMarginTopPx = (int) (16 * density); // Base margin: 16dp
            int scaledNameMarginTopPx = (int) (baseNameMarginTopPx * Math.min(scaleFactor, 1.5f));
            itemName.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledNameTextSizeSp);
            ConstraintLayout.LayoutParams nameParams = (ConstraintLayout.LayoutParams) itemName.getLayoutParams();
            nameParams.topMargin = scaledNameMarginTopPx;
            itemName.setLayoutParams(nameParams);
        }

        // Scale LinearLayout9 (Price and Prep Time)
        if (view.findViewById(R.id.linearLayout9) != null) {
            int baseLinear9MarginTopPx = (int) (20 * density); // Base margin: 20dp
            int scaledLinear9MarginTopPx = (int) (baseLinear9MarginTopPx * Math.min(scaleFactor, 1.5f));
            ConstraintLayout.LayoutParams linear9Params = (ConstraintLayout.LayoutParams) view.findViewById(R.id.linearLayout9).getLayoutParams();
            linear9Params.topMargin = scaledLinear9MarginTopPx;
            view.findViewById(R.id.linearLayout9).setLayoutParams(linear9Params);
        }

        // Scale Menu Item Price
        if (itemPrice != null) {
            float basePriceTextSizeSp = 20; // Base size: 16sp
            float scaledPriceTextSizeSp = basePriceTextSizeSp * Math.min(scaleFactor, 1.5f);
            itemPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledPriceTextSizeSp);
        }

        // Scale Subtract Image
        if (view.findViewById(R.id.image_subtract) != null) {
            int baseSubtractSizePx = (int) (30 * density); // Base size: 30dp
            int scaledSubtractSizePx = (int) (baseSubtractSizePx * Math.min(scaleFactor, 1.5f));
            int baseSubtractMarginStartPx = (int) (16 * density); // Base margin: 16dp
            int scaledSubtractMarginStartPx = (int) (baseSubtractMarginStartPx * Math.min(scaleFactor, 1.5f));
            ViewGroup.LayoutParams subtractParams = view.findViewById(R.id.image_subtract).getLayoutParams();
            subtractParams.width = scaledSubtractSizePx;
            subtractParams.height = scaledSubtractSizePx;
            view.findViewById(R.id.image_subtract).setLayoutParams(subtractParams);
            LinearLayout.LayoutParams subtractLinearParams = (LinearLayout.LayoutParams) view.findViewById(R.id.image_subtract).getLayoutParams();
            subtractLinearParams.leftMargin = scaledSubtractMarginStartPx;
            view.findViewById(R.id.image_subtract).setLayoutParams(subtractLinearParams);
        }

        // Scale Prep Time
        if (itemPrepTime != null) {
            float basePrepTextSizeSp = 20; // Base size: 14sp
            float scaledPrepTextSizeSp = basePrepTextSizeSp * Math.min(scaleFactor, 1.5f);
            int basePrepMarginStartPx = (int) (8 * density); // Base margin: 8dp
            int scaledPrepMarginStartPx = (int) (basePrepMarginStartPx * Math.min(scaleFactor, 1.5f));
            itemPrepTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledPrepTextSizeSp);
            LinearLayout.LayoutParams prepParams = (LinearLayout.LayoutParams) itemPrepTime.getLayoutParams();
            prepParams.leftMargin = scaledPrepMarginStartPx;
            itemPrepTime.setLayoutParams(prepParams);
        }

        // Scale Description Title
        if (view.findViewById(R.id.text_description_title) != null) {
            float baseDescTitleTextSizeSp = 25; // Base size: 16sp
            float scaledDescTitleTextSizeSp = baseDescTitleTextSizeSp * Math.min(scaleFactor, 1.5f);
            int baseDescTitleMarginTopPx = (int) (24 * density); // Base margin: 24dp
            int scaledDescTitleMarginTopPx = (int) (baseDescTitleMarginTopPx * Math.min(scaleFactor, 1.5f));
            TextView descTitle = view.findViewById(R.id.text_description_title);
            descTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledDescTitleTextSizeSp);
            ConstraintLayout.LayoutParams descTitleParams = (ConstraintLayout.LayoutParams) descTitle.getLayoutParams();
            descTitleParams.topMargin = scaledDescTitleMarginTopPx;
            descTitle.setLayoutParams(descTitleParams);
        }

        // Scale Description
        if (itemDescription != null) {
            float baseDescTextSizeSp = 22; // Base size: 14sp
            float scaledDescTextSizeSp = baseDescTextSizeSp * Math.min(scaleFactor, 1.5f);
            int baseDescMarginHorizontalPx = (int) (8 * density); // Base margin: 8dp
            int scaledDescMarginHorizontalPx = (int) (baseDescMarginHorizontalPx * Math.min(scaleFactor, 1.5f));
            int baseDescMarginTopPx = (int) (8 * density); // Base margin: 8dp
            int scaledDescMarginTopPx = (int) (baseDescMarginTopPx * Math.min(scaleFactor, 1.5f));
            itemDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledDescTextSizeSp);
            ConstraintLayout.LayoutParams descParams = (ConstraintLayout.LayoutParams) itemDescription.getLayoutParams();
            descParams.leftMargin = scaledDescMarginHorizontalPx;
            descParams.rightMargin = scaledDescMarginHorizontalPx;
            descParams.topMargin = scaledDescMarginTopPx;
            itemDescription.setLayoutParams(descParams);
        }

        // Scale Bottom LinearLayout
        if (counterLayout != null) {
            int baseLinear8MarginTopPx = (int) (8 * density); // Base margin: 8dp
            int scaledLinear8MarginTopPx = (int) (baseLinear8MarginTopPx * Math.min(scaleFactor, 1.5f));
            LinearLayout.LayoutParams linear8Params = (LinearLayout.LayoutParams) counterLayout.getLayoutParams();
            linear8Params.topMargin = scaledLinear8MarginTopPx;
            counterLayout.setLayoutParams(linear8Params);
        }

        // Scale Minus Button
        if (minusButton != null) {
            int baseButtonSizePx = (int) (30 * density); // Base size: 30dp (consistent with other icons)
            int scaledButtonSizePx = (int) (baseButtonSizePx * Math.min(scaleFactor, 1.5f));
            ViewGroup.LayoutParams minusParams = minusButton.getLayoutParams();
            minusParams.width = scaledButtonSizePx;
            minusParams.height = scaledButtonSizePx;
            minusButton.setLayoutParams(minusParams);
            // Remove top margin to prevent vertical shift
            LinearLayout.LayoutParams minusLinearParams = (LinearLayout.LayoutParams) minusButton.getLayoutParams();
            minusLinearParams.topMargin = 0;
            minusButton.setLayoutParams(minusLinearParams);
        }

// Scale Item Count
        if (itemCountTextView != null) {
            float baseCountTextSizeSp = 25; // Base size: 20sp (match XML)
            float scaledCountTextSizeSp = baseCountTextSizeSp * Math.min(scaleFactor, 1.5f);
            int baseCountMarginHorizontalPx = (int) (8 * density); // Base margin: 8dp
            int scaledCountMarginHorizontalPx = (int) (baseCountMarginHorizontalPx * Math.min(scaleFactor, 1.5f));
            itemCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledCountTextSizeSp);
            LinearLayout.LayoutParams countParams = (LinearLayout.LayoutParams) itemCountTextView.getLayoutParams();
            countParams.leftMargin = scaledCountMarginHorizontalPx;
            countParams.rightMargin = scaledCountMarginHorizontalPx;
            countParams.topMargin = 0; // Remove top margin to align with buttons
            itemCountTextView.setLayoutParams(countParams);
        }

// Scale Plus Button
        if (plusButton != null) {
            int baseButtonSizePx = (int) (30 * density); // Base size: 30dp (consistent with minus)
            int scaledButtonSizePx = (int) (baseButtonSizePx * Math.min(scaleFactor, 1.5f));
            ViewGroup.LayoutParams plusParams = plusButton.getLayoutParams();
            plusParams.width = scaledButtonSizePx;
            plusParams.height = scaledButtonSizePx;
            plusButton.setLayoutParams(plusParams);
            // Remove top margin to prevent vertical shift
            LinearLayout.LayoutParams plusLinearParams = (LinearLayout.LayoutParams) plusButton.getLayoutParams();
            plusLinearParams.topMargin = 0;
            plusButton.setLayoutParams(plusLinearParams);
        }

// Scale Add to Cart Button
        if (addToCartButton != null) {
            float baseButtonTextSizeSp = 20; // Base size: 16sp (standard button text size)
            float scaledButtonTextSizeSp = baseButtonTextSizeSp * Math.min(scaleFactor, 1.5f);
            int baseButtonMarginStartPx = (int) (16 * density); // Base margin: 16dp
            int scaledButtonMarginStartPx = (int) (baseButtonMarginStartPx * Math.min(scaleFactor, 1.5f));
            addToCartButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledButtonTextSizeSp);
            LinearLayout.LayoutParams buttonParams = (LinearLayout.LayoutParams) addToCartButton.getLayoutParams();
            buttonParams.leftMargin = scaledButtonMarginStartPx;
            buttonParams.topMargin = 0; // Ensure no vertical offset
            addToCartButton.setLayoutParams(buttonParams);
        }

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