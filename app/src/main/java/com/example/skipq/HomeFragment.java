package com.example.skipq;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.CategoryAdaptor;
import com.example.skipq.Adaptor.RestaurantAdaptor;
import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class HomeFragment extends Fragment implements CategoryAdaptor.CategoryClickListener {

    private RecyclerView recyclerViewCategoryList;
    private RecyclerView recyclerViewRestaurantList;
    private RestaurantAdaptor restaurantAdaptor;
    private FirebaseFirestore db;
    private String selectedCategory = "All";

    private final ArrayList<CategoryDomain> categoryList = new ArrayList<>();
    private final ArrayList<RestaurantDomain> restaurantList = new ArrayList<>();

    private final int[] categoryImg = {R.drawable.white, R.drawable.fastfood, R.drawable.restaurant, R.drawable.coffee};
   // private final int[] restaurantImg = {R.drawable.coffeehouse_logo, R.drawable.icelava_logo, R.drawable.jellyfish_logo, R.drawable.kfc_logo, R.drawable.kamancha_logo, R.drawable.mcd_logo};

    private ImageView profileIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerViewRestaurantList = view.findViewById(R.id.recyclerViewRestaurants);
        recyclerViewCategoryList = view.findViewById(R.id.recyclerViewCategories);
        profileIcon = view.findViewById(R.id.profileIcon);

        db = FirebaseFirestore.getInstance();

        setupCategoryList();
        fetchRestaurants(null);

        profileIcon.setOnClickListener(v -> {
            ProfileFragment profileFragment = new ProfileFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void setupCategoryList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewCategoryList.setLayoutManager(layoutManager);

        String[] categoryNames = {"All", "FastFood", "Restaurants", "CoffeeShops"};
        for (int i = 0; i < categoryNames.length; i++) {
            categoryList.add(new CategoryDomain(categoryNames[i], categoryImg[i]));
        }

        CategoryAdaptor categoryAdaptor = new CategoryAdaptor(requireContext(), categoryList, this);
        recyclerViewCategoryList.setAdapter(categoryAdaptor);
    }

    private void fetchRestaurants(@Nullable String category) {
        db.collection("FoodPlaces")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    restaurantList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getId();
                        String imageUrl = document.getString("imageUrl");
                        String restaurantCategory = document.getString("category");

                        Log.d("Firestore", "Fetched Restaurant: " + name + " Image URL: " + imageUrl);

                        if (category == null || category.equals("All") || category.equals(restaurantCategory)) {
                            restaurantList.add(new RestaurantDomain(name, imageUrl));
                        }
                    }

                    if (restaurantAdaptor == null) {
                        restaurantAdaptor = new RestaurantAdaptor(requireContext(), restaurantList, restaurant -> openMenuFragment(restaurant.getName()));
                        recyclerViewRestaurantList.setAdapter(restaurantAdaptor);
                        recyclerViewRestaurantList.setLayoutManager(new GridLayoutManager(getContext(), 2));
                    } else {
                        restaurantAdaptor.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching restaurants", e);
                });
    }


    @Override
    public void onCategoryClick(String category) {
        selectedCategory = category;
        fetchRestaurants(category);
    }

    private void openMenuFragment(String restaurantId) {
        Bundle bundle = new Bundle();
        bundle.putString("restaurantId", restaurantId);

        MenuFragment menuFragment = new MenuFragment();
        menuFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, menuFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
