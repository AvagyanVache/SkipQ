package com.example.skipq;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.MenuAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MenuFragment extends Fragment {

    private RecyclerView recyclerViewMenu;
    private MenuAdaptor menuAdaptor;
    private ArrayList<MenuDomain> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;
    public ImageView profileIcon;

    private SearchView searchBar;

    public TextView backButton;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        recyclerViewMenu = view.findViewById(R.id.recycleViewMenu);
        recyclerViewMenu.setLayoutManager(new LinearLayoutManager(getContext()));


        searchBar = view.findViewById(R.id.searchBar);
        searchBar.clearFocus();

        view.setOnTouchListener((v, event) -> {
            if (searchBar.hasFocus()) {
                searchBar.clearFocus();
                searchBar.setIconified(true);
            }
            return false;
        });



        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    fetchMenuItems();
                } else {
                    filterList(newText);
                }
                return true;
            }

        });


        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            HomeFragment homeFragment = new HomeFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .addToBackStack(null)
                    .commit();
            stopSearchBar();
        });
        profileIcon = view.findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            ProfileFragment profileFragment = new ProfileFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, profileFragment)
                    .addToBackStack(null)
                    .commit();
            stopSearchBar();
        });
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
            fetchMenuItems();
        }

        MenuAdaptor.OnAddToCartListener onAddToCartListener = new MenuAdaptor.OnAddToCartListener() {
            @Override
            public void onItemAdded(MenuDomain item) {
            }

            @Override
            public void onAddToCart(MenuDomain menuItem) {
                boolean itemExists = false;

                for (MenuDomain cartItem : CartManager.getInstance().getCartList()) {
                    if (cartItem.getItemName().equals(menuItem.getItemName())) {
                        itemExists = true;
                        cartItem.setItemCount(cartItem.getItemCount() + menuItem.getItemCount());
                        break;
                    }
                }

                if (!itemExists) {
                    CartManager.getInstance().addToCart(menuItem);
                }

                Toast.makeText(getContext(), "Added " + menuItem.getItemName() + " to cart", Toast.LENGTH_SHORT).show();

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateCart();
                }

                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName()) != null) {
                        CartFragment cartFragment = (CartFragment) activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName());
                        if (cartFragment != null) {
                            cartFragment.refreshCart();
                        }
                    }
                }

                menuAdaptor.notifyDataSetChanged();
            }


        };

        menuAdaptor = new MenuAdaptor(requireContext(), menuList, onAddToCartListener);
        recyclerViewMenu.setAdapter(menuAdaptor);

        return view;
    }

    private void stopSearchBar() {
        searchBar.clearFocus();
        searchBar.setIconified(true);
    }

    private void filterList(String text) {
        ArrayList<MenuDomain> filteredList = new ArrayList<>();

        for (MenuDomain menuItem : menuList) {
            if (menuItem.getItemName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(menuItem);
            }
        }

        menuAdaptor.updateList(filteredList);
    }

    private void fetchMenuItems() {
        if (restaurantId == null) {
            Log.e("MenuFragment", "Restaurant ID is null");
            Toast.makeText(getContext(), "Error loading menu", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .get()
                .addOnSuccessListener(menuSnapshots -> {
                    for (QueryDocumentSnapshot menuDoc : menuSnapshots) {
                        String menuId = menuDoc.getId();

                        db.collection("FoodPlaces")
                                .document(restaurantId)
                                .collection("Menu")
                                .document(menuId)
                                .collection("Items")
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    menuList.clear();
                                    for (QueryDocumentSnapshot itemDoc : itemSnapshots) {
                                        MenuDomain menuItem = new MenuDomain();
                                        menuItem.setItemName(itemDoc.getString("Item Name"));
                                        menuItem.setItemDescription(itemDoc.getString("Item Description"));
                                        menuItem.setItemPrice(itemDoc.getString("Item Price"));
                                        menuItem.setItemImg(itemDoc.getString("Item Img"));

                                        if (itemDoc.contains("Prep Time")) {
                                            menuItem.setPrepTime(itemDoc.getLong("Prep Time").intValue());
                                        } else {
                                            menuItem.setPrepTime(0);
                                        }
                                        menuList.add(menuItem);
                                    }
                                    menuAdaptor.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching menu items", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching menu", e);
                    Toast.makeText(getContext(), "Failed to load menu", Toast.LENGTH_SHORT).show();
                });
    }
}
