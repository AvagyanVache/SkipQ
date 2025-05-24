package com.example.skipq.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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

        backButton = view.findViewById(R.id.back);
        ImageView changeLanguage = view.findViewById(R.id.changeLanguage);

        // Scale UI elements
        scaleUIElements(view);

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

        // Scale Settings Title
        TextView settingsTitle = view.findViewById(R.id.settingsTitle);
        if (settingsTitle != null) {
            settingsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) settingsTitle.getLayoutParams();
            params.bottomMargin = (int) (32 * density * scaleFactor);
            settingsTitle.setLayoutParams(params);
        }

        // Scale Language Section
        ImageView languageIcon = view.findViewById(R.id.languageIcon);
        if (languageIcon != null) {
            ViewGroup.LayoutParams params = languageIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) languageIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            languageIcon.setLayoutParams(params);
        }
        TextView languageLabel = view.findViewById(R.id.languageLabel);
        if (languageLabel != null) {
            languageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        TextView languageText = view.findViewById(R.id.languageText);
        if (languageText != null) {
            languageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * scaleFactor);
        }
        ImageView changeLanguage = view.findViewById(R.id.changeLanguage);
        if (changeLanguage != null) {
            ViewGroup.LayoutParams params = changeLanguage.getLayoutParams();
            params.width = (int) (32 * density * scaleFactor);
            params.height = (int) (32 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) changeLanguage.getLayoutParams();
            marginParams.rightMargin = (int) (16 * density * scaleFactor);
            changeLanguage.setLayoutParams(params);
        }

        // Scale Password Section
        ImageView passwordIcon = view.findViewById(R.id.passwordIcon);
        if (passwordIcon != null) {
            ViewGroup.LayoutParams params = passwordIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) passwordIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            passwordIcon.setLayoutParams(params);
        }
        TextView passwordLabel = view.findViewById(R.id.passwordLabel);
        if (passwordLabel != null) {
            passwordLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
        }
        TextView passwordText = view.findViewById(R.id.passwordText);
        if (passwordText != null) {
            passwordText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * scaleFactor);
        }
        ImageView changePassword = view.findViewById(R.id.changePassword);
        if (changePassword != null) {
            ViewGroup.LayoutParams params = changePassword.getLayoutParams();
            params.width = (int) (32 * density * scaleFactor);
            params.height = (int) (32 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) changePassword.getLayoutParams();
            marginParams.rightMargin = (int) (16 * density * scaleFactor);
            changePassword.setLayoutParams(params);
        }

        // Scale Delete Account Section
        ImageView deleteIcon = view.findViewById(R.id.deleteIcon);
        if (deleteIcon != null) {
            ViewGroup.LayoutParams params = deleteIcon.getLayoutParams();
            params.width = (int) (40 * density * scaleFactor);
            params.height = (int) (40 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) deleteIcon.getLayoutParams();
            marginParams.leftMargin = (int) (24 * density * scaleFactor);
            deleteIcon.setLayoutParams(params);
        }
        TextView deleteLabel = view.findViewById(R.id.deleteLabel);
        if (deleteLabel != null) {
            deleteLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scaleFactor);
            ViewGroup.MarginLayoutParams labelParams = (ViewGroup.MarginLayoutParams) deleteLabel.getLayoutParams();
            labelParams.leftMargin = (int) (16 * density * scaleFactor);
            deleteLabel.setLayoutParams(labelParams); // Corrected to use labelParams
        }
        ImageView deleteAccount = view.findViewById(R.id.deleteAccount);
        if (deleteAccount != null) {
            ViewGroup.LayoutParams params = deleteAccount.getLayoutParams();
            params.width = (int) (32 * density * scaleFactor);
            params.height = (int) (32 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) deleteAccount.getLayoutParams();
            marginParams.rightMargin = (int) (16 * density * scaleFactor);
            deleteAccount.setLayoutParams(params);
        }
    }
}