package com.example.skipq;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Assuming this activity contains the bottom navigation

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set default fragment (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.navigationbar_home) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.navigationbar_favourites) {
                    selectedFragment = new FavouritesFragment();


                } else if (item.getItemId() == R.id.navigationbar_cart) {
                    selectedFragment = new CartFragment();
                }
                else if (item.getItemId() == R.id.navigationbar_profilepicture) {
                    selectedFragment = new ProfileFragment();
                }

                return loadFragment(selectedFragment);
            }
        });
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
}
