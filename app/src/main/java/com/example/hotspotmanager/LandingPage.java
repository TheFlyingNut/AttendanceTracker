package com.example.hotspotmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

public class LandingPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);
        ImageView myImageView = findViewById(R.id.imageView2);
        ImageView myImageView2 = findViewById(R.id.imageView6);

        // Set an OnClickListener for the Login1 button
        myImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the Login1 activity
                Intent intent = new Intent(LandingPage.this, Login1.class);
                startActivity(intent);
            }
        });

        myImageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandingPage.this, Register2.class);
                startActivity(intent);
            }
        });
    }
}