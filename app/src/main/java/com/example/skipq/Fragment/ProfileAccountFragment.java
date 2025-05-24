package com.example.skipq.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileAccountFragment extends Fragment {

    private static final String TAG = "ProfileAccountFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private TextView  userEmail, userPhoneNumber;
    private ImageView backButton;


    public ProfileAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_account, container, false);
        Log.d(TAG, "onCreateView: Inflating layout");

        backButton= view.findViewById(R.id.back);
        userEmail = view.findViewById(R.id.UserEmail);
        userPhoneNumber = view.findViewById(R.id.userPhoneNumber);
        scaleUIElements(view);
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        loadUserData();
        return view;
    }





    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && isAdded()) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phoneNumber");
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                            userEmail.setText(email != null ? email : "Gmail@gmail.com");
                            userPhoneNumber.setText(phone != null ? phone : "+123-456-7890");

                            Log.d(TAG, "User data loaded: name=" + name + ", email=" + email + ", profilePictureUrl=" + profilePictureUrl);
                        } else {
                            Log.w(TAG, "User document does not exist");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Log.e(TAG, "Error loading user data", e);
                            Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.w(TAG, "No user logged in or fragment not attached");
        }
    }
    private void scaleUIElements(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = Math.min(screenWidth / (360 * density), 1.5f); // Reference width: 360dp, cap at 1.5x

        // Scale Back Button
        ImageView back = view.findViewById(R.id.back);
        if (back != null) {
            ViewGroup.LayoutParams params = back.getLayoutParams();
            params.width = (int) (32 * density * scaleFactor);
            params.height = (int) (32 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) back.getLayoutParams();
            marginParams.leftMargin = (int) (16 * density * scaleFactor);
            marginParams.topMargin = (int) (40 * density * scaleFactor);
            back.setLayoutParams(params);
        }

        // Scale Account Title
        TextView accountTitle = view.findViewById(R.id.AccountTitle);
        if (accountTitle != null) {
            accountTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) accountTitle.getLayoutParams();
            params.bottomMargin = (int) (32 * density * scaleFactor);
            accountTitle.setLayoutParams(params);
        }

        // Scale Email Section
        ImageView emailIcon = view.findViewById(R.id.emailIcon);
        if (emailIcon != null) {
            ViewGroup.LayoutParams params = emailIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) emailIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            emailIcon.setLayoutParams(params);
        }
        TextView emailLabel = view.findViewById(R.id.emailLabel);
        if (emailLabel != null) {
            emailLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        if (userEmail != null) {
            userEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }

        // Scale Phone Section
        ImageView phoneIcon = view.findViewById(R.id.phoneIcon);
        if (phoneIcon != null) {
            ViewGroup.LayoutParams params = phoneIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) phoneIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            phoneIcon.setLayoutParams(params);
        }
        TextView phoneLabel = view.findViewById(R.id.phoneLabel);
        if (phoneLabel != null) {
            phoneLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        if (userPhoneNumber != null) {
            userPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }
        ImageView paymentIcon = view.findViewById(R.id.paymentIcon);
        if (phoneIcon != null) {
            ViewGroup.LayoutParams params = paymentIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) paymentIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            paymentIcon.setLayoutParams(params);
        }
        TextView paymentLabel = view.findViewById(R.id.paymentLabel);
        if (paymentLabel != null) {
            paymentLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        if (userPhoneNumber != null) {
            userPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }
        TextView paymentText = view.findViewById(R.id.paymentText);
        if (paymentText != null) {
            paymentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        if (userPhoneNumber != null) {
            userPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
        }
        ImageView selectPayment = view.findViewById(R.id.selectPayment);
        if (selectPayment != null) {
            ViewGroup.LayoutParams params = selectPayment.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40* density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) selectPayment.getLayoutParams();
            marginParams.rightMargin = (int) (24 * density * scaleFactor);
            selectPayment.setLayoutParams(params);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(getContext(), "Permission denied to access images", Toast.LENGTH_SHORT).show();
        }
    }
}