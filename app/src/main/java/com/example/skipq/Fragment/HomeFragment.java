package com.example.skipq.Fragment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

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

    private final int[] categoryImg = {R.drawable.all1, R.drawable.fastfood2, R.drawable.restaurant2, R.drawable.coffee2};

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
        TextView patrastEText = view.findViewById(R.id.patrastEText);
        TextView orderCardText = view.findViewById(R.id.orderCardText);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = screenWidth / (360 * density); // Reference width: 360dp (typical phone)

        // Scale PatrastE TextView
        float basePatrastETextSizeSp = 24; // Base text size from XML
        float scaledPatrastETextSizeSp = basePatrastETextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
        patrastEText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledPatrastETextSizeSp);

        // Scale Profile Icon
        int baseIconSizePx = (int) (40 * density); // Base size: 40dp
        int scaledIconSizePx = (int) (baseIconSizePx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
        ViewGroup.LayoutParams iconParams = profileIcon.getLayoutParams();
        iconParams.width = scaledIconSizePx;
        iconParams.height = scaledIconSizePx;
        profileIcon.setLayoutParams(iconParams);

        // Scale SearchView padding and text size
        int baseSearchPaddingPx = (int) (8 * density); // Base padding: 8dp
        int scaledSearchPaddingPx = (int) (baseSearchPaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
        searchBar.setPadding(scaledSearchPaddingPx, scaledSearchPaddingPx, scaledSearchPaddingPx, scaledSearchPaddingPx);
        // Scale SearchView query text size (requires accessing internal EditText)
        EditText searchEditText = searchBar.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            float baseSearchTextSizeSp = 16; // Default SearchView text size
            float scaledSearchTextSizeSp = baseSearchTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
            searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSearchTextSizeSp);
        }

        // Scale Order Card TextView
        float baseOrderCardTextSizeSp = 16; // Base text size from XML
        float scaledOrderCardTextSizeSp = baseOrderCardTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
        orderCardText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledOrderCardTextSizeSp);
        int baseOrderCardPaddingPx = (int) (16 * density); // Base padding: 16dp
        int scaledOrderCardPaddingPx = (int) (baseOrderCardPaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
        orderCardText.setPadding(scaledOrderCardPaddingPx, scaledOrderCardPaddingPx, scaledOrderCardPaddingPx, scaledOrderCardPaddingPx);
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

        categoryList.clear();
        String[] categoryNames = {"All", "Fastfood", "Cafe", "Coffee"};
        for (int i = 0; i < categoryNames.length; i++) {
            categoryList.add(new CategoryDomain(categoryNames[i], categoryImg[i]));
        }

        CategoryAdaptor categoryAdaptor = new CategoryAdaptor(requireContext(), categoryList, this);
        recyclerViewCategoryList.setAdapter(categoryAdaptor);

        float density = getResources().getDisplayMetrics().density;
        int marginBetweenItemsPx = (int) (12 * density); // 12dp margin between items
        int marginEdgePx = (int) (6 * density); // 6dp margin at start and end
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int itemWidth = categoryAdaptor.getItemWidth(); // Get item width from adaptor
        int totalItemsWidth = itemWidth * categoryList.size(); // Total width of all items
        int totalMargins = marginEdgePx * 2 + marginBetweenItemsPx * (categoryList.size() - 1); // Start, end, and between items
        int remainingSpace = screenWidth - totalItemsWidth - totalMargins; // Space left after items and margins
        int extraPadding = remainingSpace / 2; // Distribute remaining space evenly to left and right

        recyclerViewCategoryList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                outRect.left = position == 0 ? marginEdgePx + extraPadding : marginBetweenItemsPx / 2; // 6dp + padding for first item
                outRect.right = position == parent.getAdapter().getItemCount() - 1 ? marginEdgePx + extraPadding : marginBetweenItemsPx / 2; // 6dp + padding for last item
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
                        recyclerViewRestaurantList.setLayoutManager(new GridLayoutManager(getContext(), 2)); // Fixed 2 columns
                    } else {
                        restaurantAdaptor.updateList(restaurantList); // Use updateList to refresh data
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching restaurants", e);
                });
    }
    private int calculateSpanCount() {
        float screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int itemMinWidthDp = 180; // Adjust this value to control minimum item width
        return Math.max(2, (int) (screenWidthDp / itemMinWidthDp));
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