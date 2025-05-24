package com.example.skipq.Fragment;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skipq.Activity.MainActivity;
import com.example.skipq.Adaptor.MenuAdaptor;
import com.example.skipq.CartManager;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import java.util.Map;

public class MenuFragment extends Fragment implements OnMapReadyCallback {

    private RecyclerView recyclerViewMenu;
    private MenuAdaptor menuAdaptor;
    private ArrayList<MenuDomain> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private String restaurantId;

    private SearchView searchBar;
    public ImageView backButton;
    private AutoCompleteTextView addressSpinner;
    private ArrayList<RestaurantAddress> addressList = new ArrayList<>();
    private ImageView restaurantInfo;
    private TextView addressText;
    private TextView locationText;// Changed from AutoCompleteTextView to TextView
    private LinearLayout selectLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker currentLocationMarker;
    private GoogleMap gMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        recyclerViewMenu = view.findViewById(R.id.recycleViewMenu);
        recyclerViewMenu.setLayoutManager(new GridLayoutManager(getContext(), 2));

        searchBar = view.findViewById(R.id.searchBar);
        searchBar.clearFocus();

      //  addressSpinner = view.findViewById(R.id.address_spinner);
        restaurantInfo = view.findViewById(R.id.restaurantInfo);

        addressText = view.findViewById(R.id.address_text); // Updated to TextView
        selectLocation = view.findViewById(R.id.select_location);
        locationText = view.findViewById(R.id.location_text);
        ImageView dropdownLocation = view.findViewById(R.id.dropdownLocation);

        // Scale UI elements based on screen width
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = screenWidth / (360 * density); // Reference width: 360dp (typical phone)

        // Scale SearchView padding and text size
        if (searchBar != null) {
            int baseSearchPaddingPx = (int) (8 * density); // Base padding: 8dp
            int scaledSearchPaddingPx = (int) (baseSearchPaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            searchBar.setPadding(scaledSearchPaddingPx, scaledSearchPaddingPx, scaledSearchPaddingPx, scaledSearchPaddingPx);
            EditText searchEditText = searchBar.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                float baseSearchTextSizeSp = 16; // Default SearchView text size
                float scaledSearchTextSizeSp = baseSearchTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSearchTextSizeSp);
            }
        }

        // Scale Back Button (if present)
        if (backButton != null) {
            int baseBackButtonSizePx = (int) (30 * density); // Base size: 30dp (adjust if different)
            int scaledBackButtonSizePx = (int) (baseBackButtonSizePx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ViewGroup.LayoutParams backButtonParams = backButton.getLayoutParams();
            backButtonParams.width = scaledBackButtonSizePx;
            backButtonParams.height = scaledBackButtonSizePx;
            backButton.setLayoutParams(backButtonParams);
        } else {
            Log.w("MenuFragment", "backButton not found in layout");
        }

        // Scale Select Location LinearLayout
        if (selectLocation != null) {
            int baseSelectLocationPaddingPx = (int) (8 * density); // Base padding: 8dp
            int scaledSelectLocationPaddingPx = (int) (baseSelectLocationPaddingPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            selectLocation.setPadding(scaledSelectLocationPaddingPx, scaledSelectLocationPaddingPx, scaledSelectLocationPaddingPx, scaledSelectLocationPaddingPx);
            int baseSelectLocationMarginTopPx = (int) (20 * density); // Base margin: 20dp
            int scaledSelectLocationMarginTopPx = (int) (baseSelectLocationMarginTopPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ConstraintLayout.LayoutParams selectLocationParams = (ConstraintLayout.LayoutParams) selectLocation.getLayoutParams();
            selectLocationParams.topMargin = scaledSelectLocationMarginTopPx;
            selectLocation.setLayoutParams(selectLocationParams);
        }

        // Scale Location TextView
        if (locationText != null) {
            float baseLocationTextSizeSp = 20; // Base text size from XML
            float scaledLocationTextSizeSp = baseLocationTextSizeSp * Math.min(scaleFactor, 1.5f); // Cap at 1.5x
            locationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledLocationTextSizeSp);
            int baseLocationMarginPx = (int) (10 * density); // Base margins: 10dp (top, bottom)
            int scaledLocationMarginPx = (int) (baseLocationMarginPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            LinearLayout.LayoutParams locationTextParams = (LinearLayout.LayoutParams) locationText.getLayoutParams();
            locationTextParams.topMargin = scaledLocationMarginPx;
            locationTextParams.bottomMargin = scaledLocationMarginPx;
            locationText.setLayoutParams(locationTextParams);
        }

        // Scale Dropdown Location ImageView
        if (dropdownLocation != null) {
            int baseDropdownSizePx = (int) (30 * density); // Base size: 30dp
            int scaledDropdownSizePx = (int) (baseDropdownSizePx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ViewGroup.LayoutParams dropdownParams = dropdownLocation.getLayoutParams();
            dropdownParams.width = scaledDropdownSizePx;
            dropdownParams.height = scaledDropdownSizePx;
            dropdownLocation.setLayoutParams(dropdownParams);
        }

        // Scale Restaurant Info ImageView
        if (restaurantInfo != null) {
            int baseRestaurantInfoSizePx = (int) (30 * density); // Base size: 30dp
            int scaledRestaurantInfoSizePx = (int) (baseRestaurantInfoSizePx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseRestaurantInfoMarginPx = (int) (20 * density); // Base margin: 20dp (end)
            int scaledRestaurantInfoMarginPx = (int) (baseRestaurantInfoMarginPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            int baseRestaurantInfoMarginTopPx = (int) (35 * density); // Base margin: 35dp (top)
            int scaledRestaurantInfoMarginTopPx = (int) (baseRestaurantInfoMarginTopPx * Math.min(scaleFactor, 1.5f)); // Cap at 1.5x
            ConstraintLayout.LayoutParams restaurantInfoParams = (ConstraintLayout.LayoutParams) restaurantInfo.getLayoutParams();
            restaurantInfoParams.width = scaledRestaurantInfoSizePx;
            restaurantInfoParams.height = scaledRestaurantInfoSizePx;
            restaurantInfoParams.rightMargin = scaledRestaurantInfoMarginPx;
            restaurantInfoParams.topMargin = scaledRestaurantInfoMarginTopPx;
            restaurantInfo.setLayoutParams(restaurantInfoParams);
        }

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
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
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

        restaurantInfo.setOnClickListener(v -> showRestaurantInfoDialog());
        selectLocation.setOnClickListener(v -> showAddressDialog());

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            restaurantId = getArguments().getString("restaurantId");
            fetchMenuItems();
            fetchRestaurantAddresses();
        }

        MenuAdaptor.OnAddToCartListener onAddToCartListener = new MenuAdaptor.OnAddToCartListener() {
            @Override
            public void onItemAdded(MenuDomain item) {
            }

            @Override
            public void onAddToCart(MenuDomain menuItem) {
                if (addressSpinner.getText().toString().equals("Select an address")) {
                    Toast.makeText(getContext(), "Please select a restaurant address", Toast.LENGTH_SHORT).show();
                    return;
                }

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

        MenuAdaptor.OnItemClickListener onItemClickListener = item -> {
            ItemDetailsFragment itemDetailsFragment = new ItemDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("menuItem", item);
            itemDetailsFragment.setArguments(bundle);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, itemDetailsFragment)
                    .addToBackStack(null)
                    .commit();
        };

        menuAdaptor = new MenuAdaptor(requireContext(), menuList, onAddToCartListener, onItemClickListener);
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
            if (menuItem.isAvailable() && menuItem.getItemName().toLowerCase().contains(text.toLowerCase())) {
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

        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Menu")
                .document("DefaultMenu")
                .collection("Items")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Error listening for menu items", e);
                        Toast.makeText(getContext(), "Failed to load items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    menuList.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot itemDoc : snapshots) {
                            Boolean isAvailable = itemDoc.getBoolean("Available");
                            // Include items where Available is true or null (default to true)
                            if (isAvailable == null || isAvailable) {
                                MenuDomain menuItem = new MenuDomain();
                                menuItem.setItemName(itemDoc.getString("Item Name"));
                                menuItem.setItemDescription(itemDoc.getString("Item Description"));
                                menuItem.setItemPrice(itemDoc.getString("Item Price"));
                                menuItem.setItemImg(itemDoc.getString("Item Img"));
                                menuItem.setRestaurantId(restaurantId);
                                menuItem.setAvailable(true); // Explicitly set to true for display

                                if (itemDoc.contains("Prep Time")) {
                                    menuItem.setPrepTime(itemDoc.getLong("Prep Time").intValue());
                                } else {
                                    menuItem.setPrepTime(0);
                                }

                                menuList.add(menuItem);
                                Log.d("MenuFragment", "Added item: " + menuItem.getItemName() + ", Available: " + isAvailable);
                            } else {
                                Log.d("MenuFragment", "Skipped item: " + itemDoc.getString("Item Name") + ", Available: " + isAvailable);
                            }
                        }
                    } else {
                        Log.w("MenuFragment", "No items found in snapshot");
                        Toast.makeText(getContext(), "No available items", Toast.LENGTH_SHORT).show();
                    }

                    // Update adapter
                    menuAdaptor.updateList(menuList);
                    Log.d("MenuFragment", "Updated menu with " + menuList.size() + " items");
                });
    }
    private void showItemDetailsDialog(MenuDomain item) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_menu_item_details, null);

        ImageView itemImage = dialogView.findViewById(R.id.item_image);
        TextView itemName = dialogView.findViewById(R.id.item_name);
        TextView itemDescription = dialogView.findViewById(R.id.item_description);
        TextView itemPrice = dialogView.findViewById(R.id.item_price);
        TextView itemPrepTime = dialogView.findViewById(R.id.item_prep_time);

        itemName.setText(item.getItemName() != null ? item.getItemName() : "N/A");
        itemDescription.setText(item.getItemDescription() != null ? item.getItemDescription() : "No description");
        double price = 0.0;
        try {
            price = Double.parseDouble(item.getItemPrice() != null ? item.getItemPrice() : "0");
        } catch (NumberFormatException e) {
            Log.e("MenuFragment", "Invalid price format for item: " + item.getItemName(), e);
        }
        itemPrice.setText(String.format("֏ %.2f", price));
        itemPrepTime.setText(item.getPrepTime() > 0 ? String.format("%d min", item.getPrepTime()) : "Not specified");

        if (item.getItemImg() != null && !item.getItemImg().isEmpty() && item.getItemImg().startsWith("http")) {
            Glide.with(getContext())
                    .load(item.getItemImg())

                    .into(itemImage);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Item Details")
                .setView(dialogView)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .create();
        dialog.show();
    }
    private void fetchRestaurantAddresses() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("MenuFragment", "Restaurant ID is null or empty for addresses");
            return;
        }

        addressList.clear();
        addressText.setText("Select an address");
        db.collection("FoodPlaces")
                .document(restaurantId)
                .collection("Addresses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MenuFragment", "No addresses found for restaurant: " + restaurantId);
                        Toast.makeText(getContext(), "No addresses available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<String> addressStrings = new ArrayList<>();
                    addressStrings.add("Select an address");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isAvailable = doc.getBoolean("isAvailable");
                        if (isAvailable == null || isAvailable) {
                            String address = doc.getString("address");
                            Double latitude = doc.getDouble("latitude");
                            Double longitude = doc.getDouble("longitude");
                            if (address != null && latitude != null && longitude != null) {
                                addressList.add(new RestaurantAddress(address, latitude, longitude));
                                addressStrings.add(address);
                            }
                        }
                    }
                    addressText.setTag(addressStrings);
                })
              .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error fetching addresses", e);
                        Toast.makeText(getContext(), "Failed to load addresses", Toast.LENGTH_SHORT).show();
                    });
    }
    private void showAddressDialog() {
        ArrayList<String> addressStrings = (ArrayList<String>) addressText.getTag();
        if (addressStrings == null || addressStrings.size() <= 1) {
            Toast.makeText(getContext(), "No addresses available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Restaurant Address");

        // Create an ArrayAdapter without the "Select an address" placeholder for the dialog
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                addressStrings.subList(1, addressStrings.size()) // Skip "Select an address"
        );

        builder.setAdapter(adapter, (dialog, which) -> {
            String selectedAddress = adapter.getItem(which);
            addressText.setText(selectedAddress);
            locationText.setText(selectedAddress);
            locationText.setMaxWidth((int) (selectLocation.getWidth() * 0.8));
            RestaurantAddress restaurantAddress = addressList.get(which);
            CartManager.getInstance().setSelectedAddress(
                    restaurantAddress.getAddress(),
                    new LatLng(restaurantAddress.getLatitude(), restaurantAddress.getLongitude())
            );
            Toast.makeText(getContext(), "Selected: " + restaurantAddress.getAddress(), Toast.LENGTH_SHORT).show();

            if (gMap != null) {
                LatLng addressLatLng = new LatLng(restaurantAddress.getLatitude(), restaurantAddress.getLongitude());
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(addressLatLng, 15));
                gMap.addMarker(new MarkerOptions().position(addressLatLng).title(restaurantAddress.getAddress()));
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void showRestaurantInfoDialog() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("MenuFragment", "Restaurant ID is null for info dialog");
            Toast.makeText(getContext(), "Error loading restaurant info", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("FoodPlaces").document(restaurantId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String contactPhone = documentSnapshot.getString("contactPhone");
                        Map<String, String> operatingHours = (Map<String, String>) documentSnapshot.get("operatingHours");
                        StringBuilder hoursText = new StringBuilder();
                        if (operatingHours == null || operatingHours.isEmpty()) {
                            hoursText.append("Not specified");
                        } else {
                            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                            for (String day : days) {
                                if (operatingHours.containsKey(day)) {
                                    hoursText.append(day).append(": ").append(operatingHours.get(day)).append("\n");
                                } else {
                                    hoursText.append(day).append(": Closed\n");
                                }
                            }
                        }

                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        View dialogView = inflater.inflate(R.layout.dialog_restaurant_info, null);

                        TextView nameText = dialogView.findViewById(R.id.restaurant_name);
                        TextView hoursTextView = dialogView.findViewById(R.id.operating_hours);
                        TextView phoneText = dialogView.findViewById(R.id.contact_phone);

                        nameText.setText(name != null ? name : "N/A");
                        hoursTextView.setText(hoursText.toString());
                        phoneText.setText(contactPhone != null ? contactPhone : "N/A");

                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setTitle("Restaurant Information")
                                .setView(dialogView)
                                .setPositiveButton("OK", (d, which) -> d.dismiss())
                                .create();
                        dialog.show();
                    } else {
                        Log.e("MenuFragment", "Restaurant document not found: " + restaurantId);
                        Toast.makeText(getContext(), "Restaurant info not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching restaurant info", e);
                    Toast.makeText(getContext(), "Failed to load restaurant info", Toast.LENGTH_SHORT).show();
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

    private static class RestaurantAddress {
        private String address;
        private double latitude;
        private double longitude;

        public RestaurantAddress(String address, double latitude, double longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAddress() { return address; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}