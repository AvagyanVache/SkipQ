package com.example.skipq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private Switch switchNotifications;
    private Switch switchDarkMode;
    private Spinner languageSpinner;
    private Button btnLogout;
    private boolean languageChanged = false; // Track language change

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set listeners for switches
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("notificationsEnabled", isChecked);
            editor.apply();
            String message = isChecked ? "Notifications Enabled" : "Notifications Disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences prefs = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("darkModeEnabled", isChecked);
            editor.apply();
            String message = isChecked ? "Dark Mode Enabled" : "Dark Mode Disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        // Set language spinner item selection listener
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parentView, View view, int position, long id) {
                changeLanguage(position); // Handle language change
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parentView) {
                // No action needed
            }
        });


        btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        });
    }

    public SettingsFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for the fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        languageSpinner = view.findViewById(R.id.languageSpinner);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Set current language in spinner based on saved preferences
        setInitialLanguage();

        return view;
    }

    private void setInitialLanguage() {
        // Get saved language preference (default to English)
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String language = sharedPreferences.getString("language", "en");

        // Set the language spinner selection based on saved preference
        switch (language) {
            case "es":
                languageSpinner.setSelection(1); // Spanish
                break;
            case "fr":
                languageSpinner.setSelection(2); // French
                break;
            case "de":
                languageSpinner.setSelection(3); // German
                break;
            default:
                languageSpinner.setSelection(0); // English
                break;
        }
    }

    private void changeLanguage(int position) {
        String languageCode = "en"; // Default to English

        switch (position) {
            case 1:
                languageCode = "es"; // Spanish
                break;
            case 2:
                languageCode = "fr"; // French
                break;
            case 3:
                languageCode = "de"; // German
                break;
        }

        // Only change the language if it's different from the current one
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String currentLanguage = sharedPreferences.getString("language", "en");

        if (!languageCode.equals(currentLanguage)) {
            // Save the new language preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("language", languageCode);
            editor.apply();

            // Change the app's locale
            updateLocale(languageCode);

            // Set the language changed flag to true for feedback
            languageChanged = true;
        }
    }

    private void updateLocale(String languageCode) {
        // Update the locale based on the selected language
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Recreate the activity to apply the new language
        getActivity().recreate();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Only show the toast if the language has been changed
        if (languageChanged) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
            String language = sharedPreferences.getString("language", "en");
            Toast.makeText(getContext(), "Language changed to " + language, Toast.LENGTH_SHORT).show();
            languageChanged = false; // Reset the flag
        }
    }
}
