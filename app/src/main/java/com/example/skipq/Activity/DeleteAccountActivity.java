package com.example.skipq.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skipq.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteAccountActivity extends AppCompatActivity {

    private TextInputEditText passwordEditText;
    private Button deleteButton;
    private TextView backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
backButton=findViewById(R.id.backButton);
        passwordEditText = findViewById(R.id.deleteAccountPassword);
        deleteButton = findViewById(R.id.deleteAccountButton);

        deleteButton.setOnClickListener(v -> verifyAndDeleteAccount());
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void verifyAndDeleteAccount() {
        String password = passwordEditText.getText().toString().trim();

        if (password.isEmpty()) {
            passwordEditText.setError("Please enter your password");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> showDeleteConfirmationDialog(user))
                    .addOnFailureListener(e -> {
                        Toast.makeText(DeleteAccountActivity.this,
                                "Incorrect password", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteConfirmationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount(user))
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show();
    }

    private void deleteAccount(FirebaseUser user) {
        String userId = user.getUid();

        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnSuccessListener(aVoid1 -> {
                                mAuth.signOut();
                                Toast.makeText(DeleteAccountActivity.this,
                                        "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(DeleteAccountActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(DeleteAccountActivity.this,
                                        "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DeleteAccountActivity.this,
                            "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}