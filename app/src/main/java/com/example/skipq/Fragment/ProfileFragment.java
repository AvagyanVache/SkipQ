package com.example.skipq.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.skipq.Activity.ChangePasswordActivity;
import com.example.skipq.Activity.DeleteAccountActivity;
import com.example.skipq.Activity.MainActivity;
import com.example.skipq.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private TextView userNameSurname;
    private TextView userEmail;
    private TextView userPhoneNumber;
    private Button btnLogout;
    private ImageView changePassword;
    private ImageView profilePicture;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;
    private ImageView deleteAccount;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadImageToFirestore(uri);
                }
            });
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profilePicture = view.findViewById(R.id.profilePicture);
        changePassword = view.findViewById(R.id.changePassword);
        userNameSurname = view.findViewById(R.id.UserNameSurname);
        userPhoneNumber = view.findViewById(R.id.userPhoneNumber);
        userEmail = view.findViewById(R.id.UserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        deleteAccount=view.findViewById(R.id.deleteAccount);

        db = FirebaseFirestore.getInstance();

        profilePicture.setOnClickListener(v -> {
            if (isAdded()) {
                imagePickerLauncher.launch("image/*");
            }
        });
        deleteAccount.setOnClickListener(v->{
            Intent intent = new Intent(getActivity(), DeleteAccountActivity.class);
            startActivity(intent);
        });


        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseUser != null) {
            setupProfileListener(firebaseUser);
        }

        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                userNameSurname.setText(name);
                            } else {
                                userNameSurname.setText("Name: Not set");
                            }

                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                userPhoneNumber.setText(phoneNumber);
                            } else {
                                userPhoneNumber.setText("Phone Number: Not set");
                            }
                        } else {
                            userNameSurname.setText("Name: Not set");
                            userPhoneNumber.setText("Phone Number: Not set");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileFragment", "Failed to fetch user data", e);
                        userNameSurname.setText("Name: Error");
                        userPhoneNumber.setText("Phone Number: Error");
                    });

            userEmail.setText(firebaseUser.getEmail());

            Uri photoUrl = firebaseUser.getPhotoUrl();
            if (photoUrl != null) {
                if (isAdded()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .into(profilePicture);
                }
            } else {
                profilePicture.setImageResource(R.drawable.profile_picture);
            }
        }

        changePassword.setOnClickListener(v -> {
            if (isAdded()) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(v -> {
            if (isAdded()) {
                FirebaseAuth.getInstance().signOut();
                SharedPreferences preferences = getContext().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                getActivity().finish();
                Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupProfileListener(FirebaseUser firebaseUser) {
        profileListener = db.collection("users").document(firebaseUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("ProfileFragment", "Listen failed", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists() && isAdded()) {
                        String name = documentSnapshot.getString("name");
                        userNameSurname.setText(name != null && !name.isEmpty() ? name : "Name: Not set");

                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        userPhoneNumber.setText(phoneNumber != null && !phoneNumber.isEmpty() ?
                                phoneNumber : "Phone Number: Not set");

                        String base64Image = documentSnapshot.getString("profileImage");
                        loadProfileImage(base64Image, profilePicture);

                        userEmail.setText(firebaseUser.getEmail());
                    }
                });
    }
    private void loadProfileImage(String base64Image, ImageView imageView) {
        if (base64Image != null && !base64Image.isEmpty() && isAdded()) {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Glide.with(this)
                    .load(decodedByte)
                    .transform(new CircleCrop())
                    .into(imageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.profile_picture)
                    .transform(new CircleCrop())
                    .into(imageView);
        }
    }
    private void uploadImageToFirestore(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && isAdded()) {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();

                if (imageBytes.length > 900000) {
                    Toast.makeText(getContext(), "Image too large, please select a smaller image",
                            Toast.LENGTH_SHORT).show();
                    inputStream.close();
                    return;
                }

                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                db.collection("users").document(user.getUid())
                        .update("profileImage", base64Image)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProfileFragment", "Failed to upload image to Firestore", e);
                            Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                        });

                inputStream.close();
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error processing image", e);
                Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }
}
