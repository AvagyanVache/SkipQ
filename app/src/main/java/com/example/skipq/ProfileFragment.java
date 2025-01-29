package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.Toast;

public class ProfileFragment extends Fragment {

    private Button btnLogout;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLogout.setOnClickListener(v -> {
            SharedPreferences preferences = getContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            getActivity().finish();
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        });
    }

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        btnLogout = view.findViewById(R.id.btnLogout);

        return view;
    }
}
