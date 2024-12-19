package com.example.skipq;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skipq.R;

public class MainActivity extends AppCompatActivity {


    Button LoginButton;
    EditText password;
    EditText email;

    ImageView facebook_button;
    ImageView gmail_button;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        facebook_button = findViewById(R.id.Facebook_login);
        gmail_button = findViewById(R.id.Gmail_login);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        LoginButton = findViewById(R.id.LoginButton);

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredEmail = email.getText().toString();
                String enteredPassword = password.getText().toString();

                if (enteredEmail.equals("email@gmail.com") && enteredPassword.equals("1234")) {
                    Toast.makeText(MainActivity.this, "Logged in successfully", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_LONG).show();
                }
            }
        });
        facebook_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }
}
