package com.example.skipq;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ItemDetailActivity extends AppCompatActivity {

    private TextView itemName, itemDescription, itemPrice;
    private ImageView itemImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        itemName = findViewById(R.id.itemName);
        itemDescription = findViewById(R.id.itemDescription);
        itemPrice = findViewById(R.id.itemPrice);
        itemImage = findViewById(R.id.itemImage);

        Intent intent = getIntent();
        String name = intent.getStringExtra("itemId");
        String description = intent.getStringExtra("itemDescription");
        String price = intent.getStringExtra("itemPrice");
        String imageUrl = intent.getStringExtra("itemImg");

        itemName.setText(name);
        itemDescription.setText(description);
        itemPrice.setText(price);

        Glide.with(this)
                .load(imageUrl)
                .into(itemImage);
    }
}
