package com.example.skipq.Activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.skipq.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ChangePasswordActivity extends AppCompatActivity {
    Button btnReset;
    EditText edtEmail;

    FirebaseAuth mAuth;
    String strEmail;

    TextView back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password_activity);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);



                back= findViewById(R.id.backButton);

                btnReset = findViewById(R.id.ResetPasswordButton);
                edtEmail = findViewById(R.id.reset_password);

                mAuth = FirebaseAuth.getInstance();
        scaleUIElements(findViewById(android.R.id.content));


        if (mAuth.getCurrentUser() != null) {
            strEmail = mAuth.getCurrentUser().getEmail(); // Automatically get the email
            edtEmail.setText(strEmail); // Optionally, you can display the email in the EditText (but it's not necessary)
            edtEmail.setEnabled(false); // Disable email editing to prevent changes
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

                btnReset.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!TextUtils.isEmpty(strEmail)) {
                            ResetPassword();
                        } else {
                            edtEmail.setError("Email field can't be empty");

                        }
                    }
                });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });


            }
    @Override
    public void onBackPressed() {

    }

    private void scaleUIElements(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        float scaleFactor = Math.min(screenWidth / (360 * density), 1.5f); // Reference width: 360dp, cap at 1.5x

        // Scale Top Bar (LinearLayout)
        ConstraintLayout topBar = view.findViewById(R.id.topBar);
        if (topBar != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topBar.getLayoutParams();
            params.topMargin = (int) (24 * density * scaleFactor);
            params.bottomMargin = (int) (24 * density * scaleFactor);
            topBar.setPadding(
                    (int) (16 * density * scaleFactor),
                    (int) (24 * density * scaleFactor),
                    (int) (16 * density * scaleFactor),
                    (int) (24 * density * scaleFactor)
            );
            topBar.setLayoutParams(params);
        }

        TextView backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
            backButton.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (6 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (6 * density * scaleFactor)
            );
            backButton.setLayoutParams(params);
        }

        // Scale Logo Text
        TextView logoText = view.findViewById(R.id.text);
        if (logoText != null) {
            logoText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) logoText.getLayoutParams();
            logoText.setLayoutParams(params);
        }

        // Scale CardView
        androidx.cardview.widget.CardView cardView = view.findViewById(R.id.cardView);
        if (cardView != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
            params.width = (int) (335 * density * scaleFactor); // Constrain CardView width
            params.leftMargin = (int) (20 * density * scaleFactor);
            params.rightMargin = (int) (20 * density * scaleFactor);
            params.bottomMargin = (int) (20 * density * scaleFactor);
            cardView.setRadius((int) (20 * density * scaleFactor));
            cardView.setLayoutParams(params);
        }

        // Scale Title Text
        TextView titleText = view.findViewById(R.id.title);
        if (titleText != null) {
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) titleText.getLayoutParams();
            params.bottomMargin = (int) (8 * density * scaleFactor);
            titleText.setLayoutParams(params);
        }

        // Scale Subtitle Text
        TextView subtitleText = view.findViewById(R.id.subtitle);
        if (subtitleText != null) {
            subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) subtitleText.getLayoutParams();
            params.bottomMargin = (int) (16 * density * scaleFactor);
            subtitleText.setLayoutParams(params);
        }

        // Scale TextInputLayout
        com.google.android.material.textfield.TextInputLayout textInputLayout = view.findViewById(R.id.textInputLayout1);
        if (textInputLayout != null) {
            textInputLayout.setPadding(
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor),
                    (int) (12 * density * scaleFactor)
            );
            ViewGroup.LayoutParams params = textInputLayout.getLayoutParams();
            params.width = (int) (280 * density * scaleFactor); // Match TextInputEditText minWidth
            textInputLayout.setLayoutParams(params);
        }

        // Scale Email EditText
        EditText emailEditText = view.findViewById(R.id.reset_password);
        if (emailEditText != null) {
            emailEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            emailEditText.setMinimumWidth((int) (280 * density * scaleFactor));
        }

        // Scale Reset Button
        Button resetButton = view.findViewById(R.id.ResetPasswordButton);
        if (resetButton != null) {
            resetButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scaleFactor);
            ViewGroup.LayoutParams params = resetButton.getLayoutParams();
            params.width = (int) (280 * density * scaleFactor); // Match TextInputEditText minWidth
            params.height = (int) (56 * density * scaleFactor);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) resetButton.getLayoutParams();
            marginParams.topMargin = (int) (16 * density * scaleFactor);
            resetButton.setLayoutParams(params);
        }
    }
            private void ResetPassword() {
                btnReset.setVisibility(View.INVISIBLE);

                mAuth.sendPasswordResetEmail(strEmail)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(ChangePasswordActivity.this, "Reset Password link has been sent to your registered Email", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                Intent intent = new Intent(ChangePasswordActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ChangePasswordActivity.this, "Error :- " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnReset.setVisibility(View.VISIBLE);
                            }
                        });
            }
        }


