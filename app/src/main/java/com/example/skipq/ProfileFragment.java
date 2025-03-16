package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView userNameSurname;
    private TextView userEmail;
    private TextView userPhoneNumber;
    private Button btnLogout;
    private ImageView changePassword;
    private ImageView profilePicture;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profilePicture = view.findViewById(R.id.profilePicture);
        changePassword = view.findViewById(R.id.changePassword);
        userNameSurname = view.findViewById(R.id.UserNameSurname);
        userPhoneNumber = view.findViewById(R.id.userPhoneNumber);
        userEmail = view.findViewById(R.id.UserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                userPhoneNumber.setText(phoneNumber);
                            } else {
                                userPhoneNumber.setText("Phone Number: Not set");
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ProfileFragment", "Failed to fetch phone number", e));
        }

        if (firebaseUser != null) {
            String name = firebaseUser.getDisplayName();

            if (name == null || name.isEmpty()) {
                name = "Name Surname";
            }

            userNameSurname.setText(name);
            userEmail.setText(firebaseUser.getEmail());

           /* String phoneNumber = firebaseUser.getPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                userPhoneNumber.setText(phoneNumber); // Set phone number to TextView
            } else {
                userPhoneNumber.setText("Phone Number: Not set");
            }

            */

            Uri photoUrl = firebaseUser.getPhotoUrl();
            if (photoUrl != null) {
                if (isAdded()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .into(profilePicture);
                }
            } else {
                profilePicture.setImageResource(R.drawable.profile_picture);
            }
        }

        changePassword.setOnClickListener(v -> {
            if (isAdded()) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(v -> {
            if (isAdded()) {
                FirebaseAuth.getInstance().signOut();
                SharedPreferences preferences = getContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                getActivity().finish();
                Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }
}
