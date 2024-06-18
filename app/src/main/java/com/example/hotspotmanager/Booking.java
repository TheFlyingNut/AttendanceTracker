package com.example.hotspotmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Booking extends AppCompatActivity {

    private ImageView imageViewSeminarHall;
    private TextView textViewDescription;
    private Button buttonBookHall;
    private DatabaseReference databaseReference;
    private Calendar availableDates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        imageViewSeminarHall = findViewById(R.id.imageViewSeminarHall);
        textViewDescription = findViewById(R.id.textViewDescription);
        buttonBookHall = findViewById(R.id.buttonBookHall);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("BookedDates");

        // Assume availableDates is initialized with some logic
        availableDates = Calendar.getInstance();

        buttonBookHall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                Booking.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Handle the date selection
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);

                        // Save the booked date to the database
                        saveBookedDate(selectedDate);
                    }
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        // Disable past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // Set logic to disable unavailable dates
        datePickerDialog.getDatePicker().setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int month, int dayOfMonth) {
                // Logic to grey out unavailable dates
                // For example, we could use a simple array or list of unavailable dates
                // Here we grey out weekends as an example
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    view.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                }
            }
        });

        datePickerDialog.show();
    }

    private void saveBookedDate(Calendar date) {
        String dateString = date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH) + 1) + "-" + date.get(Calendar.DAY_OF_MONTH);

        // Create a new booking entry
        Map<String, String> booking = new HashMap<>();
        booking.put("date", dateString);
        booking.put("status", "booked");

        // Push the booking entry to the database
        databaseReference.push().setValue(booking).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(Booking.this, "Seminar hall booked for: " + dateString, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Booking.this, "Failed to book seminar hall.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
