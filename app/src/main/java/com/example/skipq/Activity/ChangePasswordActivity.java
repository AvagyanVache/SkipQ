package com.example.skipq.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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




                back= findViewById(R.id.backButton);

                btnReset = findViewById(R.id.ResetPasswordButton);
                edtEmail = findViewById(R.id.reset_password);

                mAuth = FirebaseAuth.getInstance();

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



            }

            private void ResetPassword() {
                btnReset.setVisibility(View.INVISIBLE);

                mAuth.sendPasswordResetEmail(strEmail)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(ChangePasswordActivity.this, "Reset Password link has been sent to your registered Email", Toast.LENGTH_SHORT).show();
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


