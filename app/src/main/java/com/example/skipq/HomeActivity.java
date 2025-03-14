package com.example.skipq;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.skipq.Domain.MenuDomain;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        handleIntent(getIntent());

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
        } else if ("CART".equals(fragmentToLoad)) {
            selectedId = R.id.navigationbar_cart;
            fragment = new CartFragment();
        } else if ("YOUR ORDER".equals(fragmentToLoad)) {
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
        }

        bottomNavigationView.setSelectedItemId(selectedId);
        loadFragment(fragment);
    }

    private boolean selectFragment(int itemId) {
        Fragment fragment = null;

        if (itemId == R.id.navigationbar_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.navigationbar_yourorder) {
            fragment = new YourOrderMainFragment();
        } else if (itemId == R.id.navigationbar_cart) {
            fragment = new CartFragment();
        } else if (itemId == R.id.navigationbar_profilepicture) {
            fragment = new ProfileFragment();
        }

        return loadFragment(fragment);
    }
   /* public void navigateToFragment(Fragment fragment, int menuItemId, Bundle args) {
        if (args != null) {
            fragment.setArguments(args);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(menuItemId);
    }

    */
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
