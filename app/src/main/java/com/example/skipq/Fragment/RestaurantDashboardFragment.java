package com.example.skipq.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.skipq.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class RestaurantDashboardFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private String restaurantId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_restaurant_dashboard, container, false);

        // Initialize UI elements
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);

        restaurantId = getArguments().getString("restaurantId");

        // Setup ViewPager2 with adapter
        DashboardPagerAdapter pagerAdapter = new DashboardPagerAdapter(this, restaurantId);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Menu" : "Orders")
        ).attach();

        return view;
    }

    // Adapter for ViewPager2
    private static class DashboardPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final String restaurantId;

        public DashboardPagerAdapter(@NonNull Fragment fragment, String restaurantId) {
            super(fragment);
            this.restaurantId = restaurantId;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle args = new Bundle();
            args.putString("restaurantId", restaurantId);

            if (position == 0) {
                MenuManagementFragment menuFragment = new MenuManagementFragment();
                menuFragment.setArguments(args);
                return menuFragment;
            } else {
                OrderManagementFragment orderFragment = new OrderManagementFragment();
                orderFragment.setArguments(args);
                return orderFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two fragments: MenuManagement and OrderManagement
        }
    }
}