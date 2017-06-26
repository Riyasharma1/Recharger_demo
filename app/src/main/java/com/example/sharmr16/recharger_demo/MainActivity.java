package com.example.sharmr16.recharger_demo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.medtronic.neuro.HarmonyUI.widget.HarmonySwitch;

import org.json.JSONStringer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "AdvertiseActivity";
    private static final int DEFAULT_VALUE = 20;

    /* Full Bluetooth UUID that defines the Recharger Service */
    public static final ParcelUuid RECHARGER_SERVICE = ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    /* UI to control advertise value */
    private TextView mCurrentValue;
    private TextView mAntennaMovedText;
    private TextView mRetryConnectionText;
    private SeekBar mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCurrentValue = (TextView) findViewById(R.id.current);
        mSlider = (SeekBar) findViewById(R.id.slider);

        mSlider.setMax(100);
        mSlider.setOnSeekBarChangeListener(this);
        mSlider.setProgress(DEFAULT_VALUE);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        startAdvertising();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAdvertising();
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildTempPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Battery Level.");
    }

    private void startAdvertisingAntennaMoved(){

        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data_antenna_moved = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildTempAntennaMovedPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data_antenna_moved, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Antenna Moved.");

    }

    private void startAdvertisingRecharging(){

        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data_retry_connection = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildRechargingPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data_retry_connection, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Recharging.");
    }

    private void startAdvertisingLocated(){

        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data_retry_connection = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildLocatedPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data_retry_connection, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Located.");

    }

    private void startAdvertisingSummaryScreen(){

        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data_retry_connection = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildSummaryScreenPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data_retry_connection, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Summary Screen.");

    }

    private void startAdvertisingTryAnotherLocation(){

        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data_retry_connection = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
                .addServiceUuid(RECHARGER_SERVICE)
                .addServiceData(RECHARGER_SERVICE, buildTryAnotherLocationPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data_retry_connection, mAdvertiseCallback);
        Log.e("TAG:", "Advertising Try Another Location.");

    }

//    private void startSendingJSONFile(){
//
//        if (mBluetoothLeAdvertiser == null) return;
//
//        AdvertiseSettings settings = new AdvertiseSettings.Builder()
//                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
//                .setConnectable(false)
//                .setTimeout(0)
//                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
//                .build();
//
//        AdvertiseData data_retry_connection = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(true)
//                .addServiceUuid(RECHARGER_SERVICE)
//                .addServiceData(RECHARGER_SERVICE, buildJSONFile())
//                .build();
//
//        mBluetoothLeAdvertiser.startAdvertising(settings, data_retry_connection, mAdvertiseCallback);
//        Log.e("TAG:", "Advertising Try Another Location.");
//
//    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private void restartAdvertising(String state) {
        stopAdvertising();
        switch(state){
            case "Recharge Status":
                startAdvertising();
                break;

            case "Antenna Moved":
                startAdvertisingAntennaMoved();
                break;

            case "Recharging":
                startAdvertisingRecharging();
                break;

            case "Located":
                startAdvertisingLocated();
                break;

            case "Summary Screen":
                startAdvertisingSummaryScreen();
                break;

            case "Try Another Location":
                startAdvertisingTryAnotherLocation();
                break;

            case "Send JSON Message":
//                startSendingJSONFile();
                break;
        }
    }

    private byte[] buildTempPacket() {
        int value;
        try {
            value = Integer.parseInt(mCurrentValue.getText().toString());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new byte[] {(byte)value, 0x00};
    }

    private byte[] buildTempAntennaMovedPacket() {
        int value;
        try {
            value  = 0xDD;;

        } catch (NumberFormatException e) {
            value = 0;
        }
        return new byte[] {(byte)value, 0x00};
    }

    private byte[] buildRechargingPacket() {
        int value;
        try {
                value = 0xEE;

        } catch (NumberFormatException e) {
            value = 0;
        }
        return new byte[] {(byte)value, 0x00};
    }

    private byte[] buildLocatedPacket() {
        int value;
        try {
            value = 0xBB;

        } catch (NumberFormatException e) {
            value = 0;
        }
        return new byte[] {(byte)value, 0x00};
    }

    private byte[] buildSummaryScreenPacket() {
        int value;
        try {
            value = 0xAA;

        } catch (NumberFormatException e) {
            value = 0;
        }
        return new byte[] {(byte)value, 0x00};
    }

    private byte[] buildTryAnotherLocationPacket() {
        int value;
        try {
            value = 0xCC;

        } catch (NumberFormatException e) {
            value = 0;
        }
        return new byte[] {(byte)value, 0x00};
    }

    private JSONParser buildJSONFile() {
        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader("f:\\test.json"));

            JSONObject jsonObject = (JSONObject) obj;
            System.out.println(jsonObject);

            String name = (String) jsonObject.get("name");
            System.out.println(name);

            long age = (Long) jsonObject.get("age");
            System.out.println(age);

            // loop array
            JSONArray msg = (JSONArray) jsonObject.get("messages");
            Iterator<String> iterator = msg.iterator();
            while (iterator.hasNext()) {
                System.out.println(iterator.next());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return parser;
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /** Click handler to update advertisement data */

    public void onUpdateClick(View v) {
        restartAdvertising("Recharge Status");
    }

    /** Callbacks to update UI when slider changes */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrentValue.setText(String.valueOf(progress));
        Log.i(TAG, "Temp" + progress);
        restartAdvertising("Recharge Status");
    }

    /** Callbacks to update UI when slider changes */

    public void onAntennaMovedClick(View v) {
        restartAdvertising("Antenna Moved");
    }

    /** Callbacks to update UI when slider changes */

    public void onRechargingClick(View v) {
        restartAdvertising("Recharging");
    }

    /** Callbacks to update UI when slider changes */

    public void onLocatedClick(View v) {
        restartAdvertising("Located");
    }

    /** Callbacks to update UI when slider changes */

    public void onSummaryScreenClick(View v) {
        restartAdvertising("Summary Screen");
    }

    /** Callbacks to update UI when slider changes */

    public void onTryAnotherLocationClick(View v) {
        restartAdvertising("Try Another Location");
    }

    /** Callbacks to update UI when slider changes */

    public void onSendJsonFileClick(View v) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        restartAdvertising("Recharge Status");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        restartAdvertising("Recharge Status");
    }

}
