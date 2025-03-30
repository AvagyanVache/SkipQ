package com.example.skipq.Fragment;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.example.skipq.Activity.MainActivity;
import com.example.skipq.CartManager;
import com.example.skipq.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skipq.Adaptor.MenuAdaptor;
import com.example.skipq.Domain.MenuDomain;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MenuFragment extends Fragment implements OnMapReadyCallback {

    private RecyclerView recyclerViewMenu;
    private MenuAdaptor menuAdaptor;
    private ArrayList<MenuDomain> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;

    private SearchView searchBar;

    public TextView backButton;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker currentLocationMarker;
    private GoogleMap gMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        recyclerViewMenu = view.findViewById(R.id.recycleViewMenu);
        recyclerViewMenu.setLayoutManager(new LinearLayoutManager(getContext()));


        searchBar = view.findViewById(R.id.searchBar);
        searchBar.clearFocus();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());


        view.setOnTouchListener((v, event) -> {
            if (searchBar.hasFocus()) {
                searchBar.clearFocus();
                searchBar.setIconified(true);
            }
            return false;
        });


        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    fetchMenuItems();
                } else {
                    filterList(newText);
                }
                return true;
            }

        });


        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            HomeFragment homeFragment = new HomeFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .addToBackStack(null)
                    .commit();
            stopSearchBar();
        });

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
            fetchMenuItems();
        }

        MenuAdaptor.OnAddToCartListener onAddToCartListener = new MenuAdaptor.OnAddToCartListener() {
            @Override
            public void onItemAdded(MenuDomain item) {
            }

            @Override
            public void onAddToCart(MenuDomain menuItem) {
                boolean itemExists = false;

                for (MenuDomain cartItem : CartManager.getInstance().getCartList()) {
                    if (cartItem.getItemName().equals(menuItem.getItemName())) {
                        itemExists = true;
                        cartItem.setItemCount(cartItem.getItemCount() + menuItem.getItemCount());
                        break;
                    }
                }

                if (!itemExists) {
                    CartManager.getInstance().addToCart(menuItem);
                }

                Toast.makeText(getContext(), "Added " + menuItem.getItemName() + " to cart", Toast.LENGTH_SHORT).show();

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateCart();
                }

                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName()) != null) {
                        CartFragment cartFragment = (CartFragment) activity.getSupportFragmentManager().findFragmentByTag(CartFragment.class.getName());
                        if (cartFragment != null) {
                            cartFragment.refreshCart();
                        }
                    }
                }

                menuAdaptor.notifyDataSetChanged();
            }


        };

        menuAdaptor = new MenuAdaptor(requireContext(), menuList, onAddToCartListener);
        recyclerViewMenu.setAdapter(menuAdaptor);

        return view;
    }

    private void stopSearchBar() {
        searchBar.clearFocus();
        searchBar.setIconified(true);
    }

    private void filterList(String text) {
        ArrayList<MenuDomain> filteredList = new ArrayList<>();

        for (MenuDomain menuItem : menuList) {
            if (menuItem.getItemName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(menuItem);
            }
        }

        menuAdaptor.updateList(filteredList);
    }

    private void fetchMenuItems() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("MenuFragment", "Restaurant ID is null or empty");
            Toast.makeText(getContext(), "Error loading menu", Toast.LENGTH_SHORT).show();
            return;
        }

        menuList.clear();

        Log.d("MenuFragment", "Fetching menu for restaurant: " + restaurantId);

        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .get()
                .addOnSuccessListener(menuSnapshots -> {
                    if (menuSnapshots.isEmpty()) {
                        Log.e("MenuFragment", "No menus found for restaurant: " + restaurantId);
                        Toast.makeText(getContext(), "No menus available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d("MenuFragment", "Found " + menuSnapshots.size() + " menus");

                    for (QueryDocumentSnapshot menuDoc : menuSnapshots) {
                        String menuId = menuDoc.getId();
                        Log.d("MenuFragment", "Fetching items for menu: " + menuId);

                        db.collection("FoodPlaces")
                                .document(restaurantId)
                                .collection("Menu")
                                .document(menuId)
                                .collection("Items")
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    if (itemSnapshots.isEmpty()) {
                                        Log.e("MenuFragment", "No items found for menu: " + menuId);
                                        return;
                                    }

                                    Log.d("MenuFragment", "Found " + itemSnapshots.size() + " items for menu: " + menuId);

                                    for (QueryDocumentSnapshot itemDoc : itemSnapshots) {
                                        MenuDomain menuItem = new MenuDomain();
                                        menuItem.setItemName(itemDoc.getString("Item Name"));
                                        menuItem.setItemDescription(itemDoc.getString("Item Description"));
                                        menuItem.setItemPrice(itemDoc.getString("Item Price"));
                                        menuItem.setItemImg(itemDoc.getString("Item Img"));
                                        menuItem.setRestaurantId(restaurantId);

                                        if (itemDoc.contains("Prep Time")) {
                                            menuItem.setPrepTime(itemDoc.getLong("Prep Time").intValue());
                                        } else {
                                            menuItem.setPrepTime(0);
                                        }

                                        menuList.add(menuItem);
                                    }

                                    menuAdaptor.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Error fetching menu items", e);
                                    Toast.makeText(getContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching menu", e);
                    Toast.makeText(getContext(), "Failed to load menu", Toast.LENGTH_SHORT).show();
                });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        LatLng defaultLocation = new LatLng(40.1776, 44.5125);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
            updateLocation();
        }
    }

    private void updateLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            if (currentLocationMarker != null) {
                                currentLocationMarker.setPosition(currentLatLng);
                            } else {
                                currentLocationMarker = gMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                            }

                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your current location.", Toast.LENGTH_LONG).show();
            }
        }
    }
}


