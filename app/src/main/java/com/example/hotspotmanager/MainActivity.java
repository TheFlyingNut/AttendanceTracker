package com.example.hotspotmanager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.io.IOException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "HotspotManager";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    private static final int PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1002;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean isWifiP2pEnabled = false;
    private boolean isHotspotEnabled = false;
    private int connectedDeviceCount = 0;
    private DatePickerDialog datePickerDialog;
    private Button dateButton;
    private Button downloadButton;
    private String selectedDate;
    private String lastKey = null;
    private Switch mySwitch;

    private final List<String> connectedDevicesList = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable clearListRunnable = new Runnable() {
        @Override
        public void run() {
            connectedDevicesList.clear();
            handler.postDelayed(this, 24 * 60 * 60 * 1000); // Schedule clearing after 24 hours
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.textViewLog);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView = findViewById(R.id.textViewDevices);
        initDatePicker();
        textView.setMovementMethod(new ScrollingMovementMethod());
        dateButton = findViewById(R.id.datePickerButton);
        dateButton.setText(getTodaysDate());
        mySwitch = findViewById(R.id.switch1);
        downloadButton = findViewById(R.id.button);

        handler.postDelayed(clearListRunnable, 24 * 60 * 60 * 1000);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!initP2p()) {
            finish();
        }

        mySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                onButtonStartTapped(buttonView);
            } else {
                onButtonStopTapped(buttonView);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MainActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            // Permission is already granted
            fetchAndSavePdf();
        }

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDate != null) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                    } else {
                        generatePDF();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a date first.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void generatePDF() {
        fetchAttendanceData(selectedDate, new DataCallback() {
            @Override
            public void onDataFetched(Map<String, Boolean> attendanceData) {
                if (attendanceData != null) {
                    byte[] pdfData = PDFGenerator.generatePDF(selectedDate, attendanceData);
                    if (pdfData != null) {
                        try {
                            savePdfToDownloads(pdfData, "Attendance_" + selectedDate + ".pdf");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error saving PDF", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No attendance data found for " + selectedDate, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error fetching attendance data", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getTodaysDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        month = month + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return makeDateString(day, month, year);
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = makeDateString(day, month, year);
                dateButton.setText(date);
                selectedDate = date;
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int style = AlertDialog.THEME_HOLO_LIGHT;

        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);
    }

    private String makeDateString(int day, int month, int year) {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month) {
        if (month == 1) return "JAN";
        if (month == 2) return "FEB";
        if (month == 3) return "MAR";
        if (month == 4) return "APR";
        if (month == 5) return "MAY";
        if (month == 6) return "JUN";
        if (month == 7) return "JUL";
        if (month == 8) return "AUG";
        if (month == 9) return "SEP";
        if (month == 10) return "OCT";
        if (month == 11) return "NOV";
        if (month == 12) return "DEC";
        return "JAN";
    }

    public void openDatePicker(View view) {
        datePickerDialog.show();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
            case PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    generatePDF();
                } else {
                    Toast.makeText(this, "Permission denied to write to external storage", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean initP2p() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }
        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }
        manager = (WifiP2pManager) getApplicationContext().getSystemService(WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }
        channel = manager.initialize(this, getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }

        return true;
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void onButtonStartTapped(View view) {
        if (!isWifiP2pEnabled) {
            outputLog("error: cannot start hotspot. WifiP2p is not enabled\n");
            return;
        }

        EditText editText = findViewById(R.id.editSSID);
        String ssid = "DIRECT-hs-" + editText.getText().toString();
        editText = findViewById(R.id.editPassword);
        String password = editText.getText().toString();
        int band = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
        if (((RadioButton) findViewById(R.id.radioButton2G)).isChecked()) {
            band = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
        } else if (((RadioButton) findViewById(R.id.radioButton5G)).isChecked()) {
            band = WifiP2pConfig.GROUP_OWNER_BAND_5GHZ;
        }

        WifiP2pConfig config = new WifiP2pConfig.Builder()
                .setNetworkName(ssid)
                .setPassphrase(password)
                .enablePersistentMode(false)
                .setGroupOperatingBand(band)
                .build();

        int finalBand = band;
        manager.createGroup(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                outputLog("hotspot started\n");
                isHotspotEnabled = true;
                outputLog("------------------- Hotspot Info -------------------\n");
                outputLog("SSID: " + ssid + "\n");
                outputLog("Password: " + password + "\n");
                outputLog("Band: " + ((finalBand == WifiP2pConfig.GROUP_OWNER_BAND_2GHZ) ? "2.4" : "5") + "GHz\n");
                outputLog("-----------------------------------------------------------\n");
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.BUSY) {
                    outputLog("hotspot failed to start. reason: BUSY. Retrying...\n");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonStartTapped(view);
                        }
                    }, 5000); // Retry after 5 seconds
                } else {
                    outputLog("hotspot failed to start. reason: " + reason + "\n");
                }
            }
        });
    }

    public void onButtonStopTapped(View view) {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                outputLog("hotspot stopped\n");
                isHotspotEnabled = false;
                connectedDeviceCount = 0;
                TextView textViewDevices = findViewById(R.id.textViewDevices);
                textViewDevices.setText("");
            }

            @Override
            public void onFailure(int i) {
                outputLog("hotspot failed to stop. reason: " + i + "\n");
            }
        });
    }

    public void updateConnectedDeviceList() {
        if (!isHotspotEnabled) {
            outputLog("Hotspot is not enabled\n");
            return;
        }
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                if (wifiP2pGroup == null) {
                    outputLog("No group info available\n");
                    return;
                }

                connectedDevicesList.clear();
                outputLog("Connected devices list cleared\n");

                TextView textViewDevices = findViewById(R.id.textViewDevices);
                textViewDevices.setText("");
                int i = 0;

                for (WifiP2pDevice client : wifiP2pGroup.getClientList()) {
                    String deviceInfo = "Device" + ++i + ": " + client.deviceAddress + "\n";
                    textViewDevices.append(deviceInfo);
                    connectedDevicesList.add(client.deviceAddress);  // Add the MAC address to the list
                    outputLog("Added device: " + client.deviceAddress + "\n");
                }

                if (i > connectedDeviceCount) {
                    outputLog("Device connected\n");
                    connectedDeviceCount = i;
                } else if (i < connectedDeviceCount) {
                    outputLog("Device disconnected\n");
                    connectedDeviceCount = i;
                }
            }
        });
    }
    private void savePdfToDownloads(byte[] pdfData, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pdfFile = new File(downloadsDir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pdfFile);
            fos.write(pdfData);
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "IO Exception occurred", Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error closing output stream", Toast.LENGTH_SHORT).show();
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void updateFirebase(View view) {
        outputLog("Updating Firebase...\n");
        if (selectedDate != null && !connectedDevicesList.isEmpty()) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                    .getReferenceFromUrl("https://attendencetracker-c7c0c-default-rtdb.firebaseio.com/");

            databaseReference.child("connected_devices").child(selectedDate).setValue(connectedDevicesList)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            outputLog("Data updated successfully\n");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            outputLog("Failed to update data: " + e.getMessage() + "\n");
                        }
                    });
        } else {
            outputLog("No data to update or date not selected\n");
        }
    }

    public void fetchAttendanceData(String selectedDate, final DataCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("attendance")
                .child(selectedDate);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Boolean> attendanceData = (Map<String, Boolean>) dataSnapshot.getValue();
                    callback.onDataFetched(attendanceData);
                } else {
                    callback.onDataFetched(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    private void fetchAndSavePdf() {
        // Simulate fetching PDF data
        PDFGenerator pdfGenerator = new PDFGenerator();
        String dummyDate = "April 15, 2024"; // Replace with your desired date format
        Map<String, Boolean> dummyAttendanceData = new HashMap<>();
        byte[] pdfData = PDFGenerator.generatePDF(dummyDate, dummyAttendanceData);

        // Save PDF to Downloads directory
        savePdfToDownloads(pdfData, "Attendance_APR_15_2024.pdf");
    }

    private byte[] fetchPdfData() {
        // Simulated PDF data fetching logic
        // Replace this with your actual PDF generation logic
        return new byte[]{/* PDF data */};
    }
    public interface DataCallback {
        void onDataFetched(Map<String, Boolean> attendanceData);
        void onError(Exception e);
    }

    public void onButtonUpdateTapped(View view) {
        outputLog("updating connected device list...\n");
        updateConnectedDeviceList();
    }

    private void outputLog(String msg) {
        TextView textViewLog = findViewById(R.id.textViewLog);
        textViewLog.append("  " + msg);
    }
}
