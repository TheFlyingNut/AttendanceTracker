package com.example.hotspotmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class Register1 extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword, editTextName;
    private Button buttonReg;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView textViewLogin;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register1);
        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        textViewLogin = findViewById(R.id.loginNow);

        textViewLogin.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Login1.class);
            startActivity(intent);
            finish();
        });

        buttonReg.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String name = editTextName.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Register1.this, "Email is required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Register1.this, "Password is required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(Register1.this, "Name is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Store the user's name in Firebase Database under their user ID
                            FirebaseUser user = mAuth.getCurrentUser();
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                            Toast.makeText(Register1.this, "Account Created.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Register1.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
