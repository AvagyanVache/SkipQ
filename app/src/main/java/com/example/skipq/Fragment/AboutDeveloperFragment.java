package com.example.skipq.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

        // Set static text
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
}