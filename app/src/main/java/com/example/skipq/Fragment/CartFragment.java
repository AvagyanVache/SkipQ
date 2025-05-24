package com.example.skipq.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.Activity.HomeActivity;
import com.example.skipq.Adaptor.CartAdaptor;
import com.example.skipq.CartManager;
import com.example.skipq.Domain.MenuDomain;
import com.example.skipq.Domain.YourOrderMainDomain;
import com.example.skipq.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment implements CartAdaptor.OnCartUpdatedListener {
 private RecyclerView recyclerView;
 private CartAdaptor cartAdaptor;
 private TextView totalPriceTextView;
 private TextView timeTillReadyTextView;
 private TextView selectedLocationTextView;
 private ArrayList<MenuDomain> cartList;
 private ImageView profileIcon;
 private Button checkOutButton;
 private ImageView emptyCartImg;
 private TextView cartEmpty, cartEmpty2;
 private Button searchFood;
 private View textInputLayoutPhone;
 private View textInputName;
 private View linearLayout;
 private FirebaseFirestore db;
 private ListenerRegistration profileListener;
 private TextInputLayout customerCountLayout;
 private TextInputEditText customerCountInput;
 private String selectedOrderType = "Pick Up";
 private RadioGroup orderTypeRadioGroup;
 private RadioButton radioPickUp;
 private RadioButton radioEatIn;
 private TextView orderTypeLabel, selectedLocationText;

 private ImageView locationPhoto;
 @Override
 public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
  View view = inflater.inflate(R.layout.fragment_cart, container, false);
  CartManager.getInstance().setListener(this);

  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
  if (getActivity() != null) {
   getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  checkOutButton = view.findViewById(R.id.CheckOutbutton);
  recyclerView = view.findViewById(R.id.cartRecycleView);
  totalPriceTextView = view.findViewById(R.id.totalPrice);
  timeTillReadyTextView = view.findViewById(R.id.TimeTillReady);
  selectedLocationTextView = view.findViewById(R.id.selected_location);
  profileIcon = view.findViewById(R.id.profileIcon);
  cartEmpty = view.findViewById(R.id.cartEmpty);
  cartEmpty2 = view.findViewById(R.id.cartEmpty2);
  textInputLayoutPhone = view.findViewById(R.id.textInputLayoutPhone);
  textInputName = view.findViewById(R.id.textInputName);
  linearLayout = view.findViewById(R.id.linearLayout);
  emptyCartImg = view.findViewById(R.id.emptyCartImg);
  customerCountLayout = view.findViewById(R.id.customerCountLayout);
  customerCountInput = view.findViewById(R.id.customerCountInput);
  orderTypeRadioGroup = view.findViewById(R.id.orderTypeRadioGroup);
  radioPickUp = view.findViewById(R.id.radioPickUp);
  radioEatIn = view.findViewById(R.id.radioEatIn);
  orderTypeLabel = view.findViewById(R.id.orderTypeLabel);
  locationPhoto=view.findViewById(R.id.location);
  searchFood=view.findViewById(R.id.searchFood);
  selectedLocationText=view.findViewById(R.id.selected_location_text);
  cartList = new ArrayList<>(CartManager.getInstance().getCartList());
  db = FirebaseFirestore.getInstance();
  DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
  int screenWidth = displayMetrics.widthPixels;
  float density = displayMetrics.density;
  float scaleFactor = screenWidth / (360 * density); // Reference width: 360dp
  int targetWidthPx = (int) (screenWidth * 0.75); // Target width: 75% of screen

  // Scale Profile Icon
  if (profileIcon != null) {
   int baseIconSizePx = (int) (40 * density);
   int scaledIconSizePx = (int) (baseIconSizePx * Math.min(scaleFactor, 1.5f));
   int baseIconPaddingPx = (int) (4 * density);
   int scaledIconPaddingPx = (int) (baseIconPaddingPx * Math.min(scaleFactor, 1.5f));
   int baseIconMarginTopPx = (int) (16 * density);
   int scaledIconMarginTopPx = (int) (baseIconMarginTopPx * Math.min(scaleFactor, 1.5f));

   ViewGroup.LayoutParams iconParams = profileIcon.getLayoutParams();
   iconParams.width = scaledIconSizePx;
   iconParams.height = scaledIconSizePx;
   profileIcon.setLayoutParams(iconParams);
   profileIcon.setPadding(scaledIconPaddingPx, scaledIconPaddingPx, scaledIconPaddingPx, scaledIconPaddingPx);
   ConstraintLayout.LayoutParams iconConstraintParams = (ConstraintLayout.LayoutParams) profileIcon.getLayoutParams();
   iconConstraintParams.topMargin = scaledIconMarginTopPx;
   profileIcon.setLayoutParams(iconConstraintParams);
  }

  // Scale Title Layout
  LinearLayout titleLayout = view.findViewById(R.id.title);
  if (titleLayout != null) {
   int baseTitleMarginTopPx = (int) (60 * density);
   int scaledTitleMarginTopPx = (int) (baseTitleMarginTopPx * Math.min(scaleFactor, 1.5f));
   ConstraintLayout.LayoutParams titleParams = (ConstraintLayout.LayoutParams) titleLayout.getLayoutParams();
   titleParams.topMargin = scaledTitleMarginTopPx;
   titleLayout.setLayoutParams(titleParams);
  }

  // Scale Title Text
  TextView titleText = view.findViewById(R.id.textView);
  if (titleText != null) {
   float baseTitleTextSizeSp = 40;
   float scaledTitleTextSizeSp = baseTitleTextSizeSp * Math.min(scaleFactor, 1.5f);
   titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTitleTextSizeSp);
  }

  // Scale Cart Icon
  ImageView cartIcon = view.findViewById(R.id.imageView2);
  if (cartIcon != null) {
   int baseCartIconWidthPx = (int) (63 * density);
   int scaledCartIconWidthPx = (int) (baseCartIconWidthPx * Math.min(scaleFactor, 1.5f));
   int baseCartIconHeightPx = (int) (52 * density);
   int scaledCartIconHeightPx = (int) (baseCartIconHeightPx * Math.min(scaleFactor, 1.5f));

   ViewGroup.LayoutParams cartIconParams = cartIcon.getLayoutParams();
   cartIconParams.width = scaledCartIconWidthPx;
   cartIconParams.height = scaledCartIconHeightPx;
   cartIcon.setLayoutParams(cartIconParams);
  }

  // Scale Empty Cart Image
  if (emptyCartImg != null) {
   int baseEmptyImageWidthPx = (int) (150 * density);
   int scaledEmptyImageWidthPx = (int) (baseEmptyImageWidthPx * Math.min(scaleFactor, 1.5f));
   int baseEmptyImageHeightPx = (int) (150 * density);
   int scaledEmptyImageHeightPx = (int) (baseEmptyImageHeightPx * Math.min(scaleFactor, 1.5f));

   ViewGroup.LayoutParams emptyImageParams = emptyCartImg.getLayoutParams();
   emptyImageParams.width = scaledEmptyImageWidthPx;
   emptyImageParams.height = scaledEmptyImageHeightPx;
   emptyCartImg.setLayoutParams(emptyImageParams);
  }

  // Scale Empty Cart Text
  if (cartEmpty != null) {
   float baseEmptyTextSizeSp = 25;
   float scaledEmptyTextSizeSp = baseEmptyTextSizeSp * Math.min(scaleFactor, 1.5f);
   cartEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledEmptyTextSizeSp);
  }

  // Scale Empty Cart Subtext
  if (cartEmpty2 != null) {
   float baseEmptySubtextSizeSp = 18;
   float scaledEmptySubtextSizeSp = baseEmptySubtextSizeSp * Math.min(scaleFactor, 1.5f);
   cartEmpty2.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledEmptySubtextSizeSp);
  }

  // Scale Search Food Button
  if (searchFood != null) {
   float baseButtonTextSizeSp = 16;
   float scaledButtonTextSizeSp = baseButtonTextSizeSp * Math.min(scaleFactor, 1.5f);
   int baseButtonMarginTopPx = (int) (90 * density);
   int scaledButtonMarginTopPx = (int) (baseButtonMarginTopPx * Math.min(scaleFactor, 1.5f));
   searchFood.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledButtonTextSizeSp);
   ConstraintLayout.LayoutParams buttonParams = (ConstraintLayout.LayoutParams) searchFood.getLayoutParams();
   buttonParams.topMargin = scaledButtonMarginTopPx;
   searchFood.setLayoutParams(buttonParams);
  }

  // Scale Order Type Label
  if (orderTypeLabel != null) {
   float baseLabelTextSizeSp = 20;
   float scaledLabelTextSizeSp = baseLabelTextSizeSp * Math.min(scaleFactor, 1.5f);
   orderTypeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledLabelTextSizeSp);
  }

  // Scale Radio Buttons
  if (radioPickUp != null && radioEatIn != null) {
   float baseRadioTextSizeSp = 16;
   float scaledRadioTextSizeSp = baseRadioTextSizeSp * Math.min(scaleFactor, 1.5f);
   radioPickUp.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledRadioTextSizeSp);
   radioEatIn.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledRadioTextSizeSp);
  }

  // Scale Location Text
  if (selectedLocationText != null && selectedLocationTextView != null) {
   float baseLocationTextSizeSp = 20;
   float scaledLocationTextSizeSp = baseLocationTextSizeSp * Math.min(scaleFactor, 1.5f);
   selectedLocationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledLocationTextSizeSp);
   selectedLocationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledLocationTextSizeSp);
  }

  // Scale Location Icon
  if (locationPhoto != null) {
   int baseLocationIconSizePx = (int) (30 * density);
   int scaledLocationIconSizePx = (int) (baseLocationIconSizePx * Math.min(scaleFactor, 1.5f));

   ViewGroup.LayoutParams locationIconParams = locationPhoto.getLayoutParams();
   locationIconParams.width = scaledLocationIconSizePx;
   locationIconParams.height = scaledLocationIconSizePx;
   locationPhoto.setLayoutParams(locationIconParams);
  }

  // Scale Order Summary Text
  TextView orderSummaryText = view.findViewById(R.id.textViewOrderSummary);
  if (orderSummaryText != null) {
   float baseSummaryTextSizeSp = 25;
   float scaledSummaryTextSizeSp = baseSummaryTextSizeSp * Math.min(scaleFactor, 1.5f);
   orderSummaryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSummaryTextSizeSp);
  }

  // Scale Summary Items Text
  TextView discountText = view.findViewById(R.id.textViewDiscount);
  TextView discountValue = view.findViewById(R.id.Discount);
  TextView readyTimeText = view.findViewById(R.id.textViewReadyTime);
  TextView readyTimeValue = view.findViewById(R.id.TimeTillReady);
  TextView totalText = view.findViewById(R.id.textViewTotal);
  TextView totalValue = view.findViewById(R.id.totalPrice);

  if (discountText != null && discountValue != null && readyTimeText != null &&
          readyTimeValue != null && totalText != null && totalValue != null) {
   float baseSummaryItemTextSizeSp = 20;
   float scaledSummaryItemTextSizeSp = baseSummaryItemTextSizeSp * Math.min(scaleFactor, 1.5f);
   float baseTimeTextSizeSp = 18;
   float scaledTimeTextSizeSp = baseTimeTextSizeSp * Math.min(scaleFactor, 1.5f);

   discountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSummaryItemTextSizeSp);
   discountValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSummaryItemTextSizeSp);
   totalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSummaryItemTextSizeSp);
   totalValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSummaryItemTextSizeSp);
   readyTimeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTimeTextSizeSp);
   readyTimeValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTimeTextSizeSp);
  }

  // Scale Checkout Button
  if (checkOutButton != null) {
   float baseCheckoutTextSizeSp = 16;
   float scaledCheckoutTextSizeSp = baseCheckoutTextSizeSp * Math.min(scaleFactor, 1.5f);
   int baseCheckoutMarginTopPx = (int) (20 * density);
   int scaledCheckoutMarginTopPx = (int) (baseCheckoutMarginTopPx * Math.min(scaleFactor, 1.5f));
   checkOutButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledCheckoutTextSizeSp);
   LinearLayout.LayoutParams checkoutParams = (LinearLayout.LayoutParams) checkOutButton.getLayoutParams();
   checkoutParams.topMargin = scaledCheckoutMarginTopPx;
   checkoutParams.width = targetWidthPx; // Set to 75% of screen width
   checkOutButton.setLayoutParams(checkoutParams);
  }

  // Scale RecyclerView
  if (recyclerView != null) {
   ViewGroup.LayoutParams recyclerParams = recyclerView.getLayoutParams();
   recyclerParams.width = targetWidthPx; // Set to 75% of screen width
   recyclerView.setLayoutParams(recyclerParams);
  }

  // Scale TextInputLayoutPhone
  if (textInputLayoutPhone != null) {
   ViewGroup.LayoutParams phoneLayoutParams = textInputLayoutPhone.getLayoutParams();
   phoneLayoutParams.width = targetWidthPx; // Set to 75% of screen width
   textInputLayoutPhone.setLayoutParams(phoneLayoutParams);
  }

  // Scale TextInputName
  if (textInputName != null) {
   ViewGroup.LayoutParams nameLayoutParams = textInputName.getLayoutParams();
   nameLayoutParams.width = targetWidthPx; // Set to 75% of screen width
   textInputName.setLayoutParams(nameLayoutParams);
  }

  // Scale Order Type Section
  LinearLayout orderTypeSection = view.findViewById(R.id.orderTypeSection);
  if (orderTypeSection != null) {
   ViewGroup.LayoutParams orderTypeParams = orderTypeSection.getLayoutParams();
   orderTypeParams.width = targetWidthPx; // Set to 75% of screen width
   orderTypeSection.setLayoutParams(orderTypeParams);
  }

  // Scale Customer Count Layout
  if (customerCountLayout != null) {
   ViewGroup.LayoutParams customerCountParams = customerCountLayout.getLayoutParams();
   customerCountParams.width = targetWidthPx; // Set to 75% of screen width
   customerCountLayout.setLayoutParams(customerCountParams);
  }

  // Scale LinearLayout (Order Summary)
  if (linearLayout != null) {
   ViewGroup.LayoutParams linearLayoutParams = linearLayout.getLayoutParams();
   linearLayoutParams.width = targetWidthPx; // Set to 75% of screen width
   linearLayout.setLayoutParams(linearLayoutParams);

  }
  requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
   @Override
   public void handleOnBackPressed() {

   }
  });
  FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
  orderTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
   if (checkedId == R.id.radioPickUp) {
    selectedOrderType = "Pick Up";
    customerCountLayout.setVisibility(View.GONE);
    customerCountInput.setVisibility(View.GONE);
   } else if (checkedId == R.id.radioEatIn) {
    selectedOrderType = "Eat In";
    customerCountLayout.setVisibility(View.VISIBLE);
    customerCountInput.setVisibility(View.VISIBLE);
   }
  });

  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
          requireContext(),
          R.array.order_types,
          android.R.layout.simple_dropdown_item_1line
  );
  adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);


  searchFood.setOnClickListener(v -> {
   Intent intent = new Intent(getActivity(), HomeActivity.class);
   intent.putExtra("FRAGMENT_TO_LOAD", "HOME");
   intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
   startActivity(intent);
  });  profileIcon.setOnClickListener(v -> {
   Intent intent = new Intent(getActivity(), HomeActivity.class);
   intent.putExtra("FRAGMENT_TO_LOAD", "PROFILE");
   intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
   startActivity(intent);
  });

  // Set up checkout button click listener
  checkOutButton.setOnClickListener(v -> proceedToOrder());

  // Initialize input fields
  com.hbb20.CountryCodePicker countryCodePicker = view.findViewById(R.id.countryCodePicker);
  TextInputEditText phoneNumberInput = view.findViewById(R.id.phoneNumberInput);
  TextInputEditText nameInput = view.findViewById(R.id.userNameSurname);

  // Update UI casting on cart state
  updateCartVisibility(cartEmpty, cartEmpty2, recyclerView, textInputLayoutPhone, textInputName, linearLayout, checkOutButton, emptyCartImg, selectedLocationTextView, searchFood);
  // Load user data if authenticated
  if (currentUser != null) {
   setupProfileListener(currentUser);
   loadUserData(currentUser, phoneNumberInput, nameInput, countryCodePicker);
  }

  // Load saved user data
  if (currentUser != null) {
   db.collection("users").document(currentUser.getUid())
           .get()
           .addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
             String savedPhoneNumber = documentSnapshot.getString("phoneNumber");
             String savedName = documentSnapshot.getString("name");

             if (savedPhoneNumber != null && !savedPhoneNumber.isEmpty()) {
              String countryCode = countryCodePicker.getSelectedCountryCodeWithPlus();
              if (savedPhoneNumber.startsWith(countryCode)) {
               String nationalNumber = savedPhoneNumber.substring(countryCode.length());
               phoneNumberInput.setText(nationalNumber);
              } else {
               phoneNumberInput.setText(savedPhoneNumber);
              }
             }
             if (savedName != null && !savedName.isEmpty()) {
              nameInput.setText(savedName);
             }
            }
           })
           .addOnFailureListener(e -> Log.e("CartFragment", "Failed to fetch user data", e));
  }

  // Save phone number on focus loss
  phoneNumberInput.setOnFocusChangeListener((v, hasFocus) -> {
   if (!hasFocus && currentUser != null) {
    String nationalNumber = phoneNumberInput.getText().toString().trim();
    if (!nationalNumber.isEmpty() && validatePhoneNumber()) {
     String fullNumber = countryCodePicker.getSelectedCountryCodeWithPlus() + nationalNumber;
     db.collection("users").document(currentUser.getUid())
             .update("phoneNumber", fullNumber)
             .addOnSuccessListener(aVoid -> Log.d("CartFragment", "Phone number updated"))
             .addOnFailureListener(e -> Log.e("CartFragment", "Failed to update phone number", e));
    }
   }
  });

  // Save name on focus loss
  nameInput.setOnFocusChangeListener((v, hasFocus) -> {
   if (!hasFocus && currentUser != null) {
    String name = nameInput.getText().toString().trim();
    if (!name.isEmpty()) {
     db.collection("users").document(currentUser.getUid())
             .update("name", name)
             .addOnSuccessListener(aVoid -> Log.d("CartFragment", "Name updated"))
             .addOnFailureListener(e -> Log.e("CartFragment", "Failed to update name", e));
    }
   }
  });

  // Initialize RecyclerView
  cartAdaptor = new CartAdaptor(requireContext(), cartList, this);
  recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
  recyclerView.setAdapter(cartAdaptor);

  // Update total price and prep time
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
  updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());

  // Update selected address display
  updateSelectedAddress();

  return view;
 }

 private void setupProfileListener(FirebaseUser firebaseUser) {
  profileListener = db.collection("users").document(firebaseUser.getUid())
          .addSnapshotListener((documentSnapshot, e) -> {
           if (e != null) {
            Log.e("CartFragment", "Listen failed", e);
            return;
           }
           if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
            loadProfileImage(profilePictureUrl, profileIcon);
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

 private void loadUserData(FirebaseUser currentUser, TextInputEditText phoneNumberInput,
                           TextInputEditText nameInput, com.hbb20.CountryCodePicker countryCodePicker) {
  db.collection("users").document(currentUser.getUid())
          .get()
          .addOnSuccessListener(documentSnapshot -> {
           if (documentSnapshot.exists()) {
            String savedPhoneNumber = documentSnapshot.getString("phoneNumber");
            String savedName = documentSnapshot.getString("name");

            if (savedPhoneNumber != null && !savedPhoneNumber.isEmpty()) {
             String countryCode = countryCodePicker.getSelectedCountryCodeWithPlus();
             if (savedPhoneNumber.startsWith(countryCode)) {
              String nationalNumber = savedPhoneNumber.substring(countryCode.length());
              phoneNumberInput.setText(nationalNumber);
             } else {
              phoneNumberInput.setText(savedPhoneNumber);
             }
            }
            if (savedName != null && !savedName.isEmpty()) {
             nameInput.setText(savedName);
            }
           }
          })
          .addOnFailureListener(e -> Log.e("CartFragment", "Failed to fetch user data", e));
 }

 private boolean validatePhoneNumber() {
  com.hbb20.CountryCodePicker countryCodePicker = getView().findViewById(R.id.countryCodePicker);
  TextInputEditText phoneNumberInput = getView().findViewById(R.id.phoneNumberInput);
  com.google.android.material.textfield.TextInputLayout phoneLayout = getView().findViewById(R.id.textInputLayoutPhone);

  String phoneNumber = phoneNumberInput.getText().toString().trim();
  String countryCode = countryCodePicker.getSelectedCountryCode();

  if (phoneNumber.isEmpty()) {
   phoneLayout.setError("Phone number can't be empty");
   return false;
  }

  PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  try {
   String fullPhoneNumber = "+" + countryCode + phoneNumber;
   Phonenumber.PhoneNumber parsedNumber = phoneUtil.parse(fullPhoneNumber, null);

   String regionCode = phoneUtil.getRegionCodeForCountryCode(Integer.parseInt(countryCode));
   boolean isValid = phoneUtil.isValidNumberForRegion(parsedNumber, regionCode);

   if (!isValid) {
    phoneLayout.setError("Invalid phone number for " + countryCodePicker.getSelectedCountryName());
    return false;
   }

   phoneLayout.setError(null);
   return true;
  } catch (NumberParseException e) {
   phoneLayout.setError("Invalid phone number format");
   return false;
  } catch (NumberFormatException e) {
   phoneLayout.setError("Invalid country code");
   return false;
  }
 }

 private boolean validateName() {
  TextInputEditText nameInput = getView().findViewById(R.id.userNameSurname);
  String name = nameInput.getText().toString().trim();
  if (name.isEmpty()) {
   nameInput.setError("Name can't be empty");
   return false;
  } else {
   nameInput.setError(null);
   return true;
  }
 }

 private boolean validateAddress() {
  String selectedAddress = CartManager.getInstance().getSelectedAddress();
  if (selectedAddress == null || selectedAddress.isEmpty()) {
   Toast.makeText(getContext(), "Please select a restaurant address", Toast.LENGTH_SHORT).show();
   return false;
  }
  return true;
 }
 private boolean validateCustomerCount() {
  if (selectedOrderType.equals("Eat In")) {
   String countStr = customerCountInput.getText().toString().trim();
   if (countStr.isEmpty()) {
    customerCountLayout.setError("Please enter the number of customers");
    return false;
   }
   try {
    int count = Integer.parseInt(countStr);
    if (count < 1) {
     customerCountLayout.setError("Number of customers must be at least 1");
     return false;
    }
    if (count > 100) {
     customerCountLayout.setError("Number of customers cannot exceed 100");
     return false;
    }
    customerCountLayout.setError(null);
    return true;
   } catch (NumberFormatException e) {
    customerCountLayout.setError("Invalid number format");
    return false;
   }
  }
  return true; // Valid for non-"Eat In" orders
 }
 private void proceedToOrder() {
  if (!validateName() || !validatePhoneNumber() || !validateAddress() || !validateCustomerCount()) {
   Toast.makeText(getContext(), "Please fill in all required fields, select an address, and provide a valid customer count", Toast.LENGTH_SHORT).show();
   return;
  }

  com.hbb20.CountryCodePicker countryCodePicker = getView().findViewById(R.id.countryCodePicker);
  TextInputEditText phoneNumberInput = getView().findViewById(R.id.phoneNumberInput);
  TextInputEditText nameInput = getView().findViewById(R.id.userNameSurname);

  String fullPhoneNumber = countryCodePicker.getSelectedCountryCodeWithPlus() + phoneNumberInput.getText().toString().trim();
  String name = nameInput.getText().toString().trim();

  if (fullPhoneNumber.isEmpty() || name.isEmpty()) {
   Toast.makeText(getContext(), "Name and phone number cannot be empty", Toast.LENGTH_LONG).show();
   return;
  }

  if (cartList.isEmpty()) {
   Log.e("CartFragment", "Cart is empty, cannot place order.");
   return;
  }

  Map<String, List<MenuDomain>> ordersByRestaurant = new HashMap<>();
  for (MenuDomain item : cartList) {
   if (item.getRestaurantId() == null || item.getRestaurantId().isEmpty()) {
    Log.e("CartFragment", "Item has no restaurant: " + item.getItemName());
    return;
   }
   ordersByRestaurant.computeIfAbsent(item.getRestaurantId(), k -> new ArrayList<>()).add(item);
  }

  if (ordersByRestaurant.size() > 1) {
   new AlertDialog.Builder(getContext())
           .setTitle("Can't place an order as items from different food places are added")
           .setMessage("Please ensure all items in your cart are from the same restaurant to proceed with the order.")
           .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
           .setCancelable(false)
           .show();
   return; // Prevent order placement
  }

  // Single order case
  if (!isAdded() || getContext() == null) {
   Log.w("CartFragment", "Fragment detached, skipping single order processing");
   return;
  }
  String restaurantId = cartList.get(0).getRestaurantId();
  double totalPrice = CartManager.getInstance().getTotalPrice();
  int totalPrepTime = CartManager.getInstance().getTotalPrepTime();

  SharedPreferences sharedPreferences = getContext().getSharedPreferences("cart_data", Context.MODE_PRIVATE);
  SharedPreferences.Editor editor = sharedPreferences.edit();
  Gson gson = new Gson();
  String cartItemsJson = gson.toJson(cartList);
  editor.putString("cartItems", cartItemsJson);
  editor.putString("restaurantId", restaurantId);
  editor.putFloat("totalPrice", (float) totalPrice);
  editor.putInt("prepTime", totalPrepTime);
  editor.putString("orderType", selectedOrderType);
  editor.putInt("customerCount", selectedOrderType.equals("Eat In") ? Integer.parseInt(customerCountInput.getText().toString().trim()) : 1);
  editor.apply();

  YourOrderMainDomain order = new YourOrderMainDomain();
  order.setTotalPrepTime(totalPrepTime);
  order.setItems(new ArrayList<>(cartList));
  order.setOrderType(selectedOrderType);
  if (selectedOrderType.equals("Eat In")) {
   order.setCustomerCount(Integer.parseInt(customerCountInput.getText().toString().trim()));
  } else {
   order.setCustomerCount(1); // Default for Pick Up
  }
  saveOrderToFirestore(new ArrayList<>(cartList), restaurantId, totalPrice, totalPrepTime, order);

  CartManager.getInstance().clearCart();
  refreshCart();
  Intent intent = new Intent(getContext(), HomeActivity.class);
  intent.putExtra("FRAGMENT_TO_LOAD", "YOUR ORDER");
  intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
  startActivity(intent);
 }

 private void saveOrderToFirestore(ArrayList<MenuDomain> cartList, String restaurantId, double totalPrice, int prepTime, YourOrderMainDomain order) {
  FirebaseFirestore db = FirebaseFirestore.getInstance();
  FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

  if (currentUser == null) {
   Log.e("FirestoreError", "User not authenticated");
   return;
  }

  String userId = currentUser.getUid();
  String orderId = db.collection("orders").document().getId();
  order.setOrderId(orderId);

  Timestamp startTime = Timestamp.now();

  // Get user device token for notifications
  FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
   if (!task.isSuccessful()) {
    Log.e("FCM", "Failed to get user device token", task.getException());
    // Proceed without token to avoid blocking order creation
   }

   String userDeviceToken = task.isSuccessful() ? task.getResult() : null;

   Map<String, Object> orderData = new HashMap<>();
   orderData.put("orderId", orderId);
   orderData.put("userId", userId);
   orderData.put("restaurantId", restaurantId);
   orderData.put("totalPrice", totalPrice);
   orderData.put("totalPrepTime", prepTime);
   orderData.put("startTime", startTime);
   orderData.put("items", order.getItems());
   orderData.put("endTime", null);
   orderData.put("status", "pending");
   orderData.put("approvalStatus", "pendingApproval");
   orderData.put("selectedAddress", CartManager.getInstance().getSelectedAddress());
   orderData.put("orderType", order.getOrderType());
   orderData.put("customerCount", order.getCustomerCount());
   if (userDeviceToken != null) {
    orderData.put("userDeviceToken", userDeviceToken);
   }

   List<Map<String, Object>> itemsList = new ArrayList<>();
   for (MenuDomain item : cartList) {
    Map<String, Object> itemData = new HashMap<>();
    itemData.put("name", item.getItemName());
    itemData.put("price", Double.parseDouble(item.getItemPrice()));
    itemData.put("prepTime", item.getPrepTime());
    itemData.put("itemCount", item.getItemCount());
    itemsList.add(itemData);
   }
   orderData.put("items", itemsList);

   db.collection("orders").document(order.getOrderId())
           .set(orderData)
           .addOnSuccessListener(aVoid -> {
            Log.d("FirestoreDebug", "Order stored successfully with ID: " + orderId);
            if (isAdded() && getActivity() != null) {
             Intent intent = new Intent(getActivity(), HomeActivity.class);
             intent.putExtra("FRAGMENT_TO_LOAD", "YOUR ORDER");
             intent.putExtra("orderId", orderId);
             startActivity(intent);
            } else {
             Log.e("CartFragment", "Fragment is not attached to an activity");
            }
           })
           .addOnFailureListener(e -> {
            Log.e("FirestoreError", "Failed to store order: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to place order", Toast.LENGTH_SHORT).show();
           });
  });
 }

 private void updateCartVisibility(TextView cartEmpty, TextView cartEmpty2, View recyclerView,
                                   View textInputLayoutPhone, View textInputName,
                                   View linearLayout, Button checkOutButton,
                                   ImageView emptyCartImg, TextView selectedLocationTextView, Button searchFood) {
  if (cartList.isEmpty()) {
   cartEmpty.setVisibility(View.VISIBLE);
   cartEmpty2.setVisibility(View.VISIBLE);
   emptyCartImg.setVisibility(View.VISIBLE);
   recyclerView.setVisibility(View.GONE);
   textInputLayoutPhone.setVisibility(View.GONE);
   textInputName.setVisibility(View.GONE);
   linearLayout.setVisibility(View.GONE);
   checkOutButton.setVisibility(View.GONE);
   selectedLocationTextView.setVisibility(View.GONE);
   orderTypeRadioGroup.setVisibility(View.GONE);
   customerCountLayout.setVisibility(View.GONE);
   customerCountInput.setVisibility(View.GONE);
   orderTypeLabel.setVisibility(View.GONE);
   selectedLocationText.setVisibility(View.GONE);
   locationPhoto.setVisibility(View.GONE);
   searchFood.setVisibility(View.VISIBLE);
  } else {
   cartEmpty.setVisibility(View.GONE);
   cartEmpty2.setVisibility(View.GONE);
   emptyCartImg.setVisibility(View.GONE);
   recyclerView.setVisibility(View.VISIBLE);
   textInputLayoutPhone.setVisibility(View.VISIBLE);
   textInputName.setVisibility(View.VISIBLE);
   linearLayout.setVisibility(View.VISIBLE);
   checkOutButton.setVisibility(View.VISIBLE);
   orderTypeLabel.setVisibility(View.VISIBLE);
   selectedLocationTextView.setVisibility(View.VISIBLE);
   orderTypeRadioGroup.setVisibility(View.VISIBLE);
   selectedLocationText.setVisibility(View.VISIBLE);
   locationPhoto.setVisibility(View.VISIBLE);
   searchFood.setVisibility(View.GONE);
   customerCountLayout.setVisibility(selectedOrderType.equals("Eat In") ? View.VISIBLE : View.GONE);
   customerCountInput.setVisibility(selectedOrderType.equals("Eat In") ? View.VISIBLE : View.GONE);
  }
 }
 private void updateSelectedAddress() {
  String selectedAddress = CartManager.getInstance().getSelectedAddress();
  if (selectedAddress != null && !selectedAddress.isEmpty()) {
   selectedLocationTextView.setText(selectedAddress);
  } else {
   selectedLocationTextView.setText("No address selected");
  }
 }

 @Override
 public void onCartUpdated(double total, int totalPrepTime) {
  updateTotalPrice(total);
  updateTimeTillReady(totalPrepTime);
  updateSelectedAddress();
  if (cartEmpty != null && textInputLayoutPhone != null && textInputName != null && linearLayout != null) {
   updateCartVisibility(cartEmpty,cartEmpty2, recyclerView, textInputLayoutPhone, textInputName, linearLayout, checkOutButton, emptyCartImg, selectedLocationTextView,searchFood);
  }
 }

 private void updateTimeTillReady(int totalPrepTime) {
  if (timeTillReadyTextView != null) {
   timeTillReadyTextView.setText(totalPrepTime + " min");
   Log.d("CartFragment", "Updated Prep Time: " + totalPrepTime);
  }
 }

 private void updateTotalPrice(double total) {
  if (totalPriceTextView != null) {
   totalPriceTextView.setText(String.format("%.2f֏", total));
  }
 }

 public void refreshCart() {
  cartList.clear();
  cartList.addAll(CartManager.getInstance().getCartList());
  cartAdaptor.notifyDataSetChanged();
  updateTotalPrice(CartManager.getInstance().getTotalPrice());
  updateTimeTillReady(CartManager.getInstance().getTotalPrepTime());
  updateSelectedAddress();
  updateCartVisibility(cartEmpty, cartEmpty2, recyclerView, textInputLayoutPhone, textInputName, linearLayout, checkOutButton, emptyCartImg, selectedLocationTextView,searchFood);
 }

 @Override
 public void onDestroyView() {
  super.onDestroyView();
  if (profileListener != null) {
   profileListener.remove();
  }
 }
}