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
import android.util.Log;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(getContext(), "Permission denied to access images", Toast.LENGTH_SHORT).show();
        }
    }
}