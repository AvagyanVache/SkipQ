package com.example.skipq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.CategoryAdaptor;
import com.example.skipq.Adaptor.RestaurantAdaptor;
import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.Domain.RestaurantDomain;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewCategoryList;
    private RecyclerView recyclerViewRestaurantList;
    private RestaurantAdaptor restaurantAdaptor;

    private final ArrayList<CategoryDomain> categoryName = new ArrayList<>();
    private final ArrayList<RestaurantDomain> restaurantList = new ArrayList<>();

    private final int[] categoryImg = {R.drawable.white, R.drawable.fastfood, R.drawable.restaurant, R.drawable.coffee};
    private final int[] restaurantImg = {R.drawable.kfc_logo, R.drawable.mcd_logo, R.drawable.kamancha_logo, R.drawable.jellyfish_logo, R.drawable.coffeehouse_logo, R.drawable.icelava_logo};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize RecyclerView for restaurant list
        recyclerViewRestaurantList = view.findViewById(R.id.recyclerViewRestaurants);
        setupRestaurantList();
        restaurantAdaptor = new RestaurantAdaptor(requireContext(), restaurantList, new RestaurantAdaptor.OnItemClickListener() {
            @Override
            public void onItemClick(RestaurantDomain restaurant) {
                // Create a bundle to pass data
                Bundle bundle = new Bundle();
                bundle.putString("restaurantId", restaurant.getName());

                // Instantiate the new fragment (MenuFragment in this case)
                MenuFragment menuFragment = new MenuFragment();
                menuFragment.setArguments(bundle);

                // Replace the fragment dynamically (without needing a container)
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, menuFragment) // Replace the entire activity content
                        .addToBackStack(null)  // Optional: adds the transaction to the back stack
                        .commit();
            }
        });
        recyclerViewRestaurantList.setAdapter(restaurantAdaptor);
        recyclerViewRestaurantList.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize category RecyclerView
        recyclerViewCategoryList = view.findViewById(R.id.recyclerViewCategories);
        setupCategoryName();
        CategoryAdaptor categoryAdaptor = new CategoryAdaptor(requireContext(), categoryName);
        recyclerViewCategoryList.setAdapter(categoryAdaptor);

        return view;
    }

    private void setupCategoryName() {
        // Set horizontal LinearLayoutManager for the categories RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewCategoryList.setLayoutManager(layoutManager);

        String[] categoryNames = getResources().getStringArray(R.array.categories_array);
        for (int i = 0; i < categoryNames.length; i++) {
            categoryName.add(new CategoryDomain(categoryNames[i], categoryImg[i]));
        }
    }

    private void setupRestaurantList() {
        // Set GridLayoutManager for the restaurants RecyclerView (2 columns)
        String[] restaurantNames = getResources().getStringArray(R.array.restaurant_array);
        for (int i = 0; i < restaurantNames.length; i++) {
            restaurantList.add(new RestaurantDomain(restaurantNames[i], restaurantImg[i]));
        }
    }
}
