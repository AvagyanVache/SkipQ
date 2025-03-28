package com.example.skipq;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.Adaptor.CategoryAdaptor;
import com.example.skipq.Adaptor.RestaurantAdaptor;
import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CategoryAdaptor.CategoryClickListener {

    private RecyclerView recyclerViewCategoryList;
    private RecyclerView recyclerViewRestaurantList;
    private RestaurantAdaptor restaurantAdaptor;
    private FirebaseFirestore db;
    private String selectedCategory = "All";
    private SearchView searchBar;

    private ListenerRegistration profileListener;

    private final ArrayList<CategoryDomain> categoryList = new ArrayList<>();
    private final ArrayList<RestaurantDomain> restaurantList = new ArrayList<>();

    private final int[] categoryImg = {R.drawable.white, R.drawable.fastfood, R.drawable.restaurant, R.drawable.coffee};
   // private final int[] restaurantImg = {R.drawable.coffeehouse_logo, R.drawable.icelava_logo, R.drawable.jellyfish_logo, R.drawable.kfc_logo, R.drawable.kamancha_logo, R.drawable.mcd_logo};

    private ImageView profileIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            setupProfileListener(currentUser);
        }

        searchBar = view.findViewById(R.id.searchBar);
        searchBar.clearFocus();

        recyclerViewRestaurantList = view.findViewById(R.id.recyclerViewRestaurants);
        recyclerViewCategoryList = view.findViewById(R.id.recyclerViewCategories);
        profileIcon = view.findViewById(R.id.profileIcon);

        db = FirebaseFirestore.getInstance();


        view.setOnTouchListener((v, event) -> {
            if (searchBar.hasFocus()) {
                searchBar.clearFocus();
                searchBar.setIconified(true);
            }
            return false;
        });
        setupCategoryList();
        fetchRestaurants(null);

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    fetchRestaurants(selectedCategory);
                } else {
                    filterList(newText);
                }
                return true;
            }
        });



        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HomeActivity.class);intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent);
        });


        return view;
    }

    private void stopSearchBar() {
        searchBar.clearFocus();
        searchBar.setIconified(true);
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
        stopSearchBar();
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
    private void setupProfileListener(FirebaseUser firebaseUser) {
        profileListener = db.collection("users").document(firebaseUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("CartFragment", "Listen failed", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                        String base64Image = documentSnapshot.getString("profileImage");
                        loadProfileImage(base64Image, profileIcon);
                    }
                });
    }
    private void loadProfileImage(String base64Image, ImageView imageView) {
        if (base64Image != null && !base64Image.isEmpty() && isAdded()) {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Glide.with(this)
                    .load(decodedByte)
                    .transform(new CircleCrop())
                    .into(imageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.profile_picture)
                    .transform(new CircleCrop())
                    .into(imageView);
        }
    }
    private void filterList(String text) {
        ArrayList<RestaurantDomain> filteredList = new ArrayList<>();

        for (RestaurantDomain restaurant : restaurantList) {
            if (restaurant.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(restaurant);
            }
        }

        restaurantAdaptor.updateList(filteredList);
    }





}
