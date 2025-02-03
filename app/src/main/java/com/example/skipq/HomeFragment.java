package com.example.skipq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    private final ArrayList<CategoryDomain> categoryName = new ArrayList<>();
    private final ArrayList<RestaurantDomain> restaurantList = new ArrayList<>();

    private final int[] categoryImg = {R.drawable.white, R.drawable.fastfood, R.drawable.restaurant, R.drawable.coffee};
    private final int[] restaurantImg = {R.drawable.coffeehouse_logo, R.drawable.icelava_logo, R.drawable.jellyfish_logo,R.drawable.kfc_logo,R.drawable.kamancha_logo,   R.drawable.mcd_logo};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerViewRestaurantList = view.findViewById(R.id.recyclerViewRestaurants);
        recyclerViewCategoryList = view.findViewById(R.id.recyclerViewCategories);

        db = FirebaseFirestore.getInstance();

        setupCategoryName();
        fetchRestaurants();

        return view;
    }

    private void setupCategoryName() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewCategoryList.setLayoutManager(layoutManager);

        String[] categoryNames = getResources().getStringArray(R.array.categories_array);
        for (int i = 0; i < categoryNames.length; i++) {
            categoryName.add(new CategoryDomain(categoryNames[i], categoryImg[i]));
        }

        CategoryAdaptor categoryAdaptor = new CategoryAdaptor(requireContext(), categoryName, this);
        recyclerViewCategoryList.setAdapter(categoryAdaptor);
    }

    private void fetchRestaurants() {
        db.collection("FoodPlaces")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    restaurantList.clear();
                    int restaurantIndex = 0;  // Index to cycle through restaurantImg
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getId();
                        String imageUrl = document.getString("ImageUrl");

                        // Get the local image from the restaurantImg array
                        int localImage = restaurantImg[restaurantIndex % restaurantImg.length];

                        // If there is an imageUrl, we can use that image for Firebase (otherwise, local image will be used)
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            restaurantList.add(new RestaurantDomain(name, localImage, imageUrl));
                        } else {
                            // Fallback to the local image if Firebase imageUrl is empty or null
                            restaurantList.add(new RestaurantDomain(name, localImage, ""));
                        }

                        restaurantIndex++;  // Move to the next restaurant image
                    }

                    restaurantAdaptor = new RestaurantAdaptor(requireContext(), restaurantList, restaurant -> openMenuFragment(restaurant.getName()));
                    recyclerViewRestaurantList.setAdapter(restaurantAdaptor);
                    recyclerViewRestaurantList.setLayoutManager(new GridLayoutManager(getContext(), 2));
                })
                .addOnFailureListener(e -> {
                    // Handle failure case
                });
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

    @Override
    public void onCategoryClick(String category) {
        MenuFragment menuFragment = new MenuFragment();
        Bundle bundle = new Bundle();
        bundle.putString("category", category);
        menuFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, menuFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
