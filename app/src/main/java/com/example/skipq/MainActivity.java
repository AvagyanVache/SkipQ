package com.example.skipq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button LoginButton;
    private EditText Loginpassword;
    private EditText Loginemail;
    private CheckBox CheckBox;
    private TextView signupRedirectText;
    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        mAuth = FirebaseAuth.getInstance();

        Loginemail = findViewById(R.id.Loginemail);
        Loginpassword = findViewById(R.id.Loginpassword);
        LoginButton = findViewById(R.id.LoginButton);
        signupRedirectText = findViewById(R.id.SignUpRedirectText);
        CheckBox = findViewById(R.id.checkbox);
        forgotPassword = findViewById(R.id.forgot_password);

        SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        String savedEmail = preferences.getString("email", "");
        String savedPassword = preferences.getString("password", "");
        boolean isChecked = preferences.getBoolean("rememberMe", false);
        if (isChecked) {
            Loginemail.setText(savedEmail);
            Loginpassword.setText(savedPassword);
            CheckBox.setChecked(true);
            signInUser();
        }

        CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                if (isChecked) {
                    editor.putString("email", Loginemail.getText().toString());
                    editor.putString("password", Loginpassword.getText().toString());
                    editor.putBoolean("rememberMe", true);
                } else {
                    editor.clear();
                }
                editor.apply();
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        LoginButton.setOnClickListener(view -> {
            if (!validateEmail() || !validatePassword()) {
                Toast.makeText(MainActivity.this, "Invalid information", Toast.LENGTH_SHORT).show();
            } else {
                signInUser();
            }
        });

        signupRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    public boolean validateEmail() {
        String val = Loginemail.getText().toString();
        if (val.isEmpty()) {
            Loginemail.setError("Email can't be empty");
            return false;
        } else {
            Loginemail.setError(null);
            return true;
        }
    }

    public boolean validatePassword() {
        String val = Loginpassword.getText().toString();
        if (val.isEmpty()) {
            Loginpassword.setError("Password can't be empty");
            return false;
        } else {
            Loginpassword.setError(null);
            return true;
        }
    }

    public void signInUser() {
        String email = Loginemail.getText().toString().trim();
        String password = Loginpassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Email or password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                            if (CheckBox.isChecked()) {
                                SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("email", email);
                                editor.putString("password", password);
                                editor.putBoolean("rememberMe", true);
                                editor.apply();
                            }

                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void updateCart() {
        CartFragment cartFragment = (CartFragment) getSupportFragmentManager().findFragmentByTag("CartFragment");
        if (cartFragment != null) {
            cartFragment.refreshCart();
        }
    }

}