package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

public class ProfileFragment extends Fragment {

    private TextView userNameSurname;
    private TextView userEmail;
    private Button btnLogout;
    private ImageView changePassword;
    private ImageView profilePicture;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profilePicture = view.findViewById(R.id.profilePicture);
        changePassword = view.findViewById(R.id.changePassword);
        userNameSurname = view.findViewById(R.id.UserNameSurname);
        userEmail = view.findViewById(R.id.UserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String name = firebaseUser.getDisplayName();

            if (name == null || name.isEmpty()) {
                name = "Name Surname";
            }

            userNameSurname.setText(name);
            userEmail.setText(firebaseUser.getEmail());

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
