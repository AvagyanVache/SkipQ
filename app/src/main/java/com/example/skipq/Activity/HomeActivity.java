package com.example.skipq.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.skipq.Fragment.CartFragment;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Fragment.HomeFragment;
import com.example.skipq.Fragment.ProfileFragment;
import com.example.skipq.Fragment.RestaurantDashboardFragment;
import com.example.skipq.R;
import com.example.skipq.Fragment.YourOrderFragment;
import com.example.skipq.Fragment.YourOrderMainFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String userRole;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Get user role and restaurant ID from intent
        Intent intent = getIntent();
        userRole = intent.getStringExtra("userRole");
        restaurantId = intent.getStringExtra("restaurantId");

        // Adjust navigation based on role
        if ("restaurant".equals(userRole)) {
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_dashboard).setVisible(true);
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_cart).setVisible(false); // Hide cart for restaurants
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_home).setVisible(false);
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_yourorder).setVisible(false);
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_profilepicture).setVisible(true);

        } else {
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_dashboard).setVisible(false);
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_home).setVisible(true); // Hide home for users
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_yourorder).setVisible(true); // Hide your order for users
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_cart).setVisible(true);
            bottomNavigationView.getMenu().findItem(R.id.navigationbar_profilepicture).setVisible(true);

        }

        handleIntent(intent);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return selectFragment(item.getItemId());
            }
        });
    }

    private void handleIntent(Intent intent) {
        String fragmentToLoad = intent.getStringExtra("FRAGMENT_TO_LOAD");

        int selectedId = R.id.navigationbar_home;
        Fragment fragment = new HomeFragment();

        if ("PROFILE".equals(fragmentToLoad)) {
            selectedId = R.id.navigationbar_profilepicture;
            fragment = new ProfileFragment();
        } else if ("HOME".equals(fragmentToLoad)&& "user".equals(userRole)) {
            selectedId = R.id.navigationbar_home;
            fragment = new HomeFragment();
        } else if ("CART".equals(fragmentToLoad) && "user".equals(userRole)) {
            selectedId = R.id.navigationbar_cart;
            fragment = new CartFragment();
        } else if ("YOUR ORDER".equals(fragmentToLoad) && "user".equals(userRole)) {
            selectedId = R.id.navigationbar_yourorder;
            fragment = new YourOrderMainFragment();

            double totalPrice = intent.getDoubleExtra("totalPrice", 0);
            int prepTime = intent.getIntExtra("prepTime", 0);
            ArrayList<MenuDomain> cartItems = intent.getParcelableArrayListExtra("cartItems");

            Bundle bundle = new Bundle();
            bundle.putDouble("totalPrice", totalPrice);
            bundle.putInt("prepTime", prepTime);
            bundle.putParcelableArrayList("cartItems", cartItems);
            fragment.setArguments(bundle);
        } else if ("RESTAURANT_DASHBOARD".equals(fragmentToLoad) && "restaurant".equals(userRole)) {
            selectedId = R.id.navigationbar_dashboard;
            fragment = new RestaurantDashboardFragment();
            Bundle bundle = new Bundle();
            bundle.putString("restaurantId", restaurantId);
            fragment.setArguments(bundle);
        }

        bottomNavigationView.setSelectedItemId(selectedId);
        loadFragment(fragment);
    }

    private boolean selectFragment(int itemId) {
        Fragment fragment = null;

        if (itemId == R.id.navigationbar_home && "user".equals(userRole)) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.navigationbar_yourorder && "user".equals(userRole)) {
            fragment = new YourOrderMainFragment();
        } else if (itemId == R.id.navigationbar_cart && "user".equals(userRole)) {
            fragment = new CartFragment();
        } else if (itemId == R.id.navigationbar_dashboard && "restaurant".equals(userRole)) {
            fragment = new RestaurantDashboardFragment();
            Bundle bundle = new Bundle();
            bundle.putString("restaurantId", restaurantId);
            fragment.setArguments(bundle);
        } else if (itemId == R.id.navigationbar_profilepicture) {
            fragment = new ProfileFragment();
        }

        return loadFragment(fragment);
    }

    public void switchToYourOrderFragment(Bundle args) {
        Fragment yourOrderFragment = new YourOrderFragment();
        yourOrderFragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, yourOrderFragment)
                .addToBackStack(null)
                .commit();

        bottomNavigationView.post(() -> bottomNavigationView.setSelectedItemId(R.id.navigationbar_yourorder));
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
}