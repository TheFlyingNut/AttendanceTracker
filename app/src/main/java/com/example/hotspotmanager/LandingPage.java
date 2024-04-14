package com.example.hotspotmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class LandingPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);
        Button login1Button = findViewById(R.id.Login1);
        Button login2Button = findViewById(R.id.Login2);

        // Set an OnClickListener for the Login1 button
        login1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the Login1 activity
                Intent intent = new Intent(LandingPage.this, Login1.class);
                startActivity(intent);
            }
        });

        login2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandingPage.this, Register2.class);
                startActivity(intent);
            }
        });
    }
}