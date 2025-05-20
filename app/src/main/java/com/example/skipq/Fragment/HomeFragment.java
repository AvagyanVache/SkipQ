package com.example.skipq.Fragment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.Activity.HomeActivity;
import com.example.skipq.Adaptor.CategoryAdaptor;
import com.example.skipq.Adaptor.RestaurantAdaptor;
import com.example.skipq.Domain.CategoryDomain;
import com.example.skipq.Domain.RestaurantDomain;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

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

    private final int[] categoryImg = {R.drawable.all, R.drawable.fastfood1, R.drawable.restaurant1, R.drawable.coffee1};

    private ImageView profileIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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

        view.setOnTouchListener((v, event) -> {
            if (searchBar.hasFocus()) {
                searchBar.clearFocus();
                searchBar.setIconified(true);
            }
            return false;
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
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
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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

        String[] categoryNames = {"All", "Fastfood", "Cafe", "Coffee"};
        for (int i = 0; i < categoryNames.length; i++) {
            categoryList.add(new CategoryDomain(categoryNames[i], categoryImg[i]));
        }

        CategoryAdaptor categoryAdaptor = new CategoryAdaptor(requireContext(), categoryList, this);
        recyclerViewCategoryList.setAdapter(categoryAdaptor);

        int spacing = getResources().getDimensionPixelSize(R.dimen.category_spacing);
        recyclerViewCategoryList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == 0) {
                    outRect.left = spacing;
                }
                outRect.right = spacing;
            }
        });
    }


    private void fetchRestaurants(@Nullable String category) {
        db.collection("FoodPlaces")
                .whereEqualTo("isApproved", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return; // Guard clause to prevent crash

                    restaurantList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getId();
                        String logoUrl = document.getString("logoUrl"); // Changed from imageUrl
                        String restaurantCategory = document.getString("category");

                        Log.d("Firestore", "Fetched Restaurant: " + name + " Logo URL: " + logoUrl);

                        if (category == null || category.equals("All") || category.equals(restaurantCategory)) {
                            restaurantList.add(new RestaurantDomain(name, logoUrl));
                        }
                    }
                    if (!isAdded()) return; // Guard clause to prevent crash

                    if (restaurantAdaptor == null) {
                        restaurantAdaptor = new RestaurantAdaptor(getContext(), restaurantList, restaurant -> openMenuFragment(restaurant.getName()));
                        recyclerViewRestaurantList.setAdapter(restaurantAdaptor);
                        int spanCount = calculateSpanCount();
                        recyclerViewRestaurantList.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
                    } else {
                        restaurantAdaptor.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching restaurants", e);
                });
    }
    private int calculateSpanCount() {
        float screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int itemMinWidthDp = 160; // reduce to allow more columns on small screens
        return Math.max(2, Math.round(screenWidthDp / itemMinWidthDp));
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
                        Log.e("HomeFragment", "Listen failed", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                        loadProfileImage(profilePictureUrl, profileIcon);
                    }
                });
    }

    private void loadProfileImage(String profilePictureUrl, ImageView imageView) {
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty() && isAdded()) {
            Glide.with(this)
                    .load(profilePictureUrl)
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