package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class ProfileFragment extends Fragment {

    private TextView userNameSurname;
    private TextView userEmail;
    private Button btnLogout;
    private ImageView changePassword;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        changePassword = view.findViewById(R.id.changePassword);
        userNameSurname = view.findViewById(R.id.UserNameSurname);
        userEmail = view.findViewById(R.id.UserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String name = firebaseUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(getContext());
                if (googleSignInAccount != null) {
                    name = googleSignInAccount.getDisplayName();
                }
            }

            if (name == null || name.isEmpty()) {
                name = "Name Surname";
            }

            userNameSurname.setText(name);
            userEmail.setText(firebaseUser.getEmail());
        }


        changePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences preferences = getContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            getActivity().finish();
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }
}
