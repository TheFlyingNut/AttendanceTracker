package com.example.hotspotmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Register2 extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextSapId, editTextMacAddress;
    private Button buttonSubmit;
    private TextView textViewMacAddress;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://attendencetracker-c7c0c-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextSapId = findViewById(R.id.editTextSapId);
        editTextMacAddress = findViewById(R.id.editTextMacAddress);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        textViewMacAddress = findViewById(R.id.textViewMacAddress);
        Button buttonShowMacAddress = findViewById(R.id.buttonShowMacAddress);

        // Set OnClickListener for the button
        buttonShowMacAddress.setOnClickListener(v -> showMacAddress());
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString();
                String email = editTextEmail.getText().toString();
                String sapId = editTextSapId.getText().toString();
                String macAddress = editTextMacAddress.getText().toString();

                // TODO: Send data to Firebase

                if(name.isEmpty() || email.isEmpty() || sapId.isEmpty() || macAddress.isEmpty()){
                    Toast.makeText(Register2.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.hasChild(sapId)){
                                Toast.makeText(Register2.this, "SapID already registered", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                //Sending data to firebase
                                databaseReference.child("users").child(sapId).child("name").setValue(name);
                                databaseReference.child("users").child(sapId).child("email").setValue(email);
                                databaseReference.child("users").child(sapId).child("macAddress").setValue(macAddress);
                                Toast.makeText(Register2.this, "Information successfully registered", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }
            }
        });
    }

    private void showMacAddress() {
        // Get WifiManager
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check if WifiManager is not null
        if (wifiManager != null) {
            // Get WifiInfo
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                // Get MAC address
                String macAddress = wifiInfo.getMacAddress();
                // Display MAC address
                textViewMacAddress.setText("MAC Address: " + macAddress);
            } else {
                // WifiInfo is null
                textViewMacAddress.setText("WifiInfo is null");
            }
        } else {
            // WifiManager is null
            textViewMacAddress.setText("WifiManager is null");
        }
    }
}