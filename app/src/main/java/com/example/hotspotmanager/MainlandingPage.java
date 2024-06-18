package com.example.hotspotmanager;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;

public class MainlandingPage extends AppCompatActivity {

    private ImageView logoutImageView;
    private FirebaseAuth mAuth;
    private ImageView imageView15;
    private ImageView imageView16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainlanding_page); // Make sure your layout file is correctly referenced

        imageView15 = findViewById(R.id.imageView15);
        imageView15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainlandingPage.this, Booking.class);
                startActivity(intent);
            }
        });

        imageView16 = findViewById(R.id.imageView16);
        imageView16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainlandingPage.this, crud_operations.class);
                startActivity(intent);
            }
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize the ImageView for logout
        logoutImageView = findViewById(R.id.imageView18);
        logoutImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logout the user
                logoutUser();

                // Clear local cache
                clearLocalCache();
            }
        });
    }

    private void logoutUser() {
        mAuth.signOut();  // Firebase sign out
        Intent intent = new Intent(MainlandingPage.this, Login1.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();  // Make sure this activity is closed and removed from the back stack
    }

    private void clearLocalCache() {
        deleteCache(this);
    }

    public static void deleteCache(Context context) {
        try {
            java.io.File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new java.io.File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}