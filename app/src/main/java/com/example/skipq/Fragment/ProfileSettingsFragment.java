package com.example.skipq.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.example.skipq.Activity.ChangePasswordActivity;
import com.example.skipq.Activity.DeleteAccountActivity;
import com.example.skipq.R;

public class ProfileSettingsFragment extends Fragment {

    private static final String TAG = "ProfileSettingsFragment";
    private ImageView backButton;

    public ProfileSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_settings, container, false);
        Log.d(TAG, "onCreateView: Inflating layout");

backButton= view.findViewById(R.id.back);
ImageView changePassword = view.findViewById(R.id.changePassword);
        changePassword.setOnClickListener(v -> {
            Log.d(TAG, "Change password clicked");
            Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        ImageView deleteAccount = view.findViewById(R.id.deleteAccount);
        deleteAccount.setOnClickListener(v -> {
            Log.d(TAG, "Delete account clicked");
            Intent intent = new Intent(requireActivity(), DeleteAccountActivity.class);
            startActivity(intent);
        });
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }
}