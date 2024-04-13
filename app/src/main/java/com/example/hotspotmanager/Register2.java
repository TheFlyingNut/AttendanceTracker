package com.example.hotspotmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Register2 extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextSapId, editTextMacAddress;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextSapId = findViewById(R.id.editTextSapId);
        editTextMacAddress = findViewById(R.id.editTextMacAddress);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString();
                String email = editTextEmail.getText().toString();
                String sapId = editTextSapId.getText().toString();
                String macAddress = editTextMacAddress.getText().toString();

                // TODO: Send data to Firebase
            }
        });
    }
}