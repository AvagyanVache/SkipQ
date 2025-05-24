package com.example.skipq.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.skipq.R;

public class AboutDeveloperFragment extends Fragment {
    private ImageView backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about_developer, container, false);

        // Initialize views
        backButton = view.findViewById(R.id.back);
        ImageView adminPicture = view.findViewById(R.id.AdminPicture);
        TextView gmailText = view.findViewById(R.id.Gmail);
        TextView phoneNumberText = view.findViewById(R.id.PhoneNumber);
        ImageView telegramIcon = view.findViewById(R.id.telegram);
        TextView telegramProfile = view.findViewById(R.id.telegramProfile);
        ImageView linkedinIcon = view.findViewById(R.id.linkedin);
        TextView linkedinProfile = view.findViewById(R.id.LinkedInProfile);

        scaleUIElements(view);
        gmailText.setText("awagyan.wache@gmail.com");
        phoneNumberText.setText("+37455252592");
        telegramProfile.setText("Telegram");
        linkedinProfile.setText("LinkedIn");

        // Back button click listener
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Telegram link click listener (for both text and icon)
        View.OnClickListener telegramClickListener = v -> {
            String telegramUrl = "https://t.me/avagyannnnn007";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl));
            startActivity(intent);
        };
        telegramProfile.setOnClickListener(telegramClickListener);
        telegramIcon.setOnClickListener(telegramClickListener);

        // LinkedIn link click listener (for both text and icon)
        View.OnClickListener linkedinClickListener = v -> {
            String linkedinUrl = "https://www.linkedin.com/in/vache-avagyan";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedinUrl));
            startActivity(intent);
        };
        linkedinProfile.setOnClickListener(linkedinClickListener);
        linkedinIcon.setOnClickListener(linkedinClickListener);

        // Admin picture click listener (placeholder for future action)
        adminPicture.setOnClickListener(v -> {
            // Add action for admin picture click, e.g., open a larger image or profile
        });

        return view;
    }
    private void scaleUIElements(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = Math.min(screenWidth / (360 * density), 1.5f); // Reference width: 360dp, cap at 1.5x

        // Scale Back Button
        if (backButton != null) {
            ViewGroup.LayoutParams params = backButton.getLayoutParams();
            params.width = (int) (32 * density * scaleFactor);
            params.height = (int) (32 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
            marginParams.leftMargin = (int) (16 * density * scaleFactor);
            marginParams.topMargin = (int) (40 * density * scaleFactor);
            backButton.setLayoutParams(params);
        }

        // Scale About Developer Title
        TextView aboutDeveloperTitle = view.findViewById(R.id.text_account);
        if (aboutDeveloperTitle != null) {
            aboutDeveloperTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) aboutDeveloperTitle.getLayoutParams();
            params.bottomMargin = (int) (34 * density * scaleFactor);
            aboutDeveloperTitle.setLayoutParams(params);
        }

        // Scale Admin Picture
        ImageView adminPicture = view.findViewById(R.id.AdminPicture);
        if (adminPicture != null) {
            ViewGroup.LayoutParams params = adminPicture.getLayoutParams();
            params.width = (int) (125 * density * scaleFactor);
            params.height = (int) (125 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) adminPicture.getLayoutParams();
            marginParams.bottomMargin = (int) (20 * density * scaleFactor);
            adminPicture.setLayoutParams(params);
        }

        // Scale Email Section
        ImageView emailIcon = view.findViewById(R.id.emailIcon);
        if (emailIcon != null) {
            ViewGroup.LayoutParams params = emailIcon.getLayoutParams();
            params.width = (int) (45 * density * scaleFactor);
            params.height = (int) (45 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) emailIcon.getLayoutParams();
            marginParams.leftMargin = (int) (30 * density * scaleFactor);
            emailIcon.setLayoutParams(params);
        }
        TextView gmailText = view.findViewById(R.id.Gmail);
        if (gmailText != null) {
            gmailText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) gmailText.getLayoutParams();
            marginParams.leftMargin = (int) (20 * density * scaleFactor);
            gmailText.setLayoutParams(marginParams);
        }

        // Scale Phone Section
        ImageView phoneIcon = view.findViewById(R.id.phoneIcon);
        if (phoneIcon != null) {
            ViewGroup.LayoutParams params = phoneIcon.getLayoutParams();
            params.width = (int) (45 * density * scaleFactor);
            params.height = (int) (45 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) phoneIcon.getLayoutParams();
            marginParams.leftMargin = (int) (26 * density * scaleFactor);
            phoneIcon.setLayoutParams(params);
        }
        TextView phoneNumberText = view.findViewById(R.id.PhoneNumber);
        if (phoneNumberText != null) {
            phoneNumberText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) phoneNumberText.getLayoutParams();
            marginParams.leftMargin = (int) (20 * density * scaleFactor);
            phoneNumberText.setLayoutParams(marginParams);
        }

        // Scale Telegram Section
        ImageView telegramIcon = view.findViewById(R.id.telegram);
        if (telegramIcon != null) {
            ViewGroup.LayoutParams params = telegramIcon.getLayoutParams();
            params.width = (int) (45 * density * scaleFactor);
            params.height = (int) (45 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) telegramIcon.getLayoutParams();
            marginParams.leftMargin = (int) (26 * density * scaleFactor);
            telegramIcon.setLayoutParams(params);
        }
        TextView telegramProfile = view.findViewById(R.id.telegramProfile);
        if (telegramProfile != null) {
            telegramProfile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) telegramProfile.getLayoutParams();
            marginParams.leftMargin = (int) (20 * density * scaleFactor);
            telegramProfile.setLayoutParams(marginParams);
        }

        // Scale LinkedIn Section
        ImageView linkedinIcon = view.findViewById(R.id.linkedin);
        if (linkedinIcon != null) {
            ViewGroup.LayoutParams params = linkedinIcon.getLayoutParams();
            params.width = (int) (45 * density * scaleFactor);
            params.height = (int) (45 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) linkedinIcon.getLayoutParams();
            marginParams.leftMargin = (int) (26 * density * scaleFactor);
            linkedinIcon.setLayoutParams(params);
        }
        TextView linkedinProfile = view.findViewById(R.id.LinkedInProfile);
        if (linkedinProfile != null) {
            linkedinProfile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) linkedinProfile.getLayoutParams();
            marginParams.leftMargin = (int) (20 * density * scaleFactor);
            linkedinProfile.setLayoutParams(marginParams);
        }
    }
}