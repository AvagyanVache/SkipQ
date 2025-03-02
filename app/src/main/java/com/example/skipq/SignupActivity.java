package com.example.skipq;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    EditText signupEmail, signupPassword, signupConfirmPassword;
    TextView loginRedirectText;
    Button signupButton;
    TextView back;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        back= findViewById(R.id.backButton);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupConfirmPassword = findViewById(R.id.signup_confirm_password);
        loginRedirectText = findViewById(R.id.LoginRedirectText);
        signupButton = findViewById(R.id.SignUpButton);

        mAuth = FirebaseAuth.getInstance();
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        signupButton.setOnClickListener(view -> {
            String email = signupEmail.getText().toString();
            String password = signupPassword.getText().toString();
            String confirmPassword = signupConfirmPassword.getText().toString();

            if (validateInput(email, password, confirmPassword)) {
                signUpUser(email, password);
            }
        });

        loginRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInput(String email, String password, String confirmPassword) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void signUpUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
