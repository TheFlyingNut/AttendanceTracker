package com.example.hotspotmanager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "HotspotManager";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean isWifiP2pEnabled = false;
    private boolean isHotspotEnabled = false;
    private int connectedDeviceCount = 0;
    private DatePickerDialog datePickerDialog;
    private Button dateButton;
    private String selectedDate;
    private String lastKey = null;

    private final List<String> connectedDevicesList = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable clearListRunnable = new Runnable() {
        @Override
        public void run() {
            connectedDevicesList.clear();
            handler.postDelayed(this, 24 * 60 * 60 * 1000); // Schedule clearing after 24 hours
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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


        handler.postDelayed(clearListRunnable, 24 * 60 * 60 * 1000);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!initP2p()) {
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MainActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
    }

    private String getTodaysDate()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        month = month + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return makeDateString(day, month, year);
    }

    private void initDatePicker()
    {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day)
            {
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
        //datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

    }

    private String makeDateString(int day, int month, int year)
    {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month)
    {
        if(month == 1)
            return "JAN";
        if(month == 2)
            return "FEB";
        if(month == 3)
            return "MAR";
        if(month == 4)
            return "APR";
        if(month == 5)
            return "MAY";
        if(month == 6)
            return "JUN";
        if(month == 7)
            return "JUL";
        if(month == 8)
            return "AUG";
        if(month == 9)
            return "SEP";
        if(month == 10)
            return "OCT";
        if(month == 11)
            return "NOV";
        if(month == 12)
            return "DEC";

        //default should never happen
        return "JAN";
    }

    public void openDatePicker(View view)
    {
        datePickerDialog.show();
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if  (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean initP2p() {
        // Device capability definition check
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        // Hardware capability check
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
    public void onButtonStartTapped(View view){
        if(!isWifiP2pEnabled){
            outputLog("error: cannot start hotspot. WifiP2p is not enabled\n");
            return;
        }

        EditText editText = findViewById(R.id.editSSID);
        String ssid = "DIRECT-hs-" + editText.getText().toString();
        editText = findViewById(R.id.editPassword);
        String password = editText.getText().toString();
        int band = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
        if(((RadioButton) findViewById(R.id.radioButton2G)).isChecked()){
            band = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
        }else if(((RadioButton) findViewById(R.id.radioButton5G)).isChecked()){
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
                outputLog("Band: "+((finalBand==WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)?"2.4":"5")+"GHz\n");
                outputLog("-----------------------------------------------------------\n");
            }

            @Override
            public void onFailure(int reason) {
                if (reason == WifiP2pManager.BUSY) {
                    outputLog("hotspot failed to start. reason: BUSY. Retrying...\n");
                    // Retry after a delay
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onButtonStartTapped(view);
                        }
                    }, 5000); // Retry after 5 seconds
                } else {
                    outputLog("hotspot failed to start. reason: " + String.valueOf(reason) + "\n");
                }
            }
        });
    }

    public void onButtonStopTapped(View view){
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
                outputLog("hotspot failed to stop. reason: " + String.valueOf(i) + "\n");
            }
        });
    }

    public void updateFirebase(View view){
        if (selectedDate != null && connectedDevicesList.size() > 0) {
            // Get a reference to your Firebase database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://attendencetracker-c7c0c-default-rtdb.firebaseio.com/");

            // If lastKey is not null, remove the old value from Firebase
            if (lastKey != null) {
                databaseReference.child("connected_devices").child(selectedDate).child(lastKey).removeValue();
            }

            // Create a new key for the data
            String key = databaseReference.child("connected_devices").child(selectedDate).push().getKey();

            // Update the data in the database
            databaseReference.child("connected_devices").child(selectedDate).child(key).setValue(connectedDevicesList)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            outputLog("Data updated successfully\n");
                            lastKey = key; // Update lastKey with the new key
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            outputLog("Failed to update data: " + e.getMessage() + "\n");
                        }
                    });
        } else {
            outputLog("No data to update\n");
        }
    }

    public void onButtonUpdateTapped(View view){
        outputLog("updating connected device list...\n");
        updateConnectedDeviceList();
    }

    public void updateConnectedDeviceList(){
        if(!isHotspotEnabled){
            return;
        }
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                // Clear the list if necessary
                if (connectedDevicesList.size() > 0) {
                    connectedDevicesList.clear();
                }

                TextView textViewDevices = findViewById(R.id.textViewDevices);
                textViewDevices.setText("");
                int i = 0;
                for(WifiP2pDevice client : wifiP2pGroup.getClientList()){
                    String deviceInfo = "  Device" + ++i + ":  " + client.deviceAddress + "\n";
                    textViewDevices.append(deviceInfo);
                    connectedDevicesList.add(deviceInfo);
                }
                if(i > connectedDeviceCount){
                    outputLog("device connected\n");
                    connectedDeviceCount = i;
                }else if(i < connectedDeviceCount){
                    outputLog("device disconnected\n");
                    connectedDeviceCount = i;
                }
            }
        });
    }


    private void outputLog(String msg){
        TextView textViewLog = findViewById(R.id.textViewLog);
        textViewLog.append("  " + msg);
    }
}