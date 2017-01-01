package com.kitkat.android.mini;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PermissionManager.Callback,
        MyBroadcastReceiver.Callback, IncomingCallBroadcastReceiver.Callback {

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings mSettings;
    private List<ScanFilter> mFilters;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mCharacteristic;
    boolean enabled;

    private boolean mScanning;
    private MainHandler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mHandler = new MainHandler();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        PermissionManager.checkPermission(this);

          /*
            // SMS Message Protocol
            String data = "Test..";
            mCharacteristic.setValue(data.getBytes());
            mBluetoothGatt.writeCharacteristic(mCharacteristic);

            // Incoming Call Protocol
            String data = "#010-0000-0000";
            mCharacteristic.setValue(data.getBytes());
            mBluetoothGatt.writeCharacteristic(mCharacteristic);

            // Call End Protocol
            String data = "&";
            mCharacteristic.setValue(data.getBytes());
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
        */

        MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        this.registerReceiver(myBroadcastReceiver, intentFilter);

        IncomingCallBroadcastReceiver incomingCallBroadcastReceiver = new IncomingCallBroadcastReceiver(this);
        intentFilter = new IntentFilter("android.intent.action.PHONE_STATE");
        this.registerReceiver(incomingCallBroadcastReceiver, intentFilter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionManager.onCheckResult(requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void BLESupportCheck(){
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE Not Supported!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // Work..
            init();
        }
    }

    @Override
    public void init() {
        /** Get the BluetoothAdapter
         *
         *  The BluetoothAdapter is required for any and all Bluetooth activity.
         *  The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio).
         *  There's one Bluetooth adapter for the entire system, and your application can interact with it using this object.
         *  The snippet below shows how to get the adapter. Note that this approach uses getSystemService() to return an instance of BluetoothManager,
         *  which is then used to get the adapter. Android 4.3 (API Level 18) introduces BluetoothManager:
         */
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Get the BluetoothLeScanner
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // ScanSetting Build
        mSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // ScanFilter Instantiate
        mFilters = new ArrayList<ScanFilter>();

        /** Enable Bluetooth
         *
         *  Next, you need to ensure that Bluetooth is enabled. Call isEnabled() to check whether Bluetooth is currently enabled.
         *  If this method returns false, then Bluetooth is disabled. The following snippet checks whether Bluetooth is enabled.
         *  If it isn't, the snippet displays an error prompting the user to go to Settings to enable Bluetooth:
         */

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            scanLeDevice(true);
    }

    /** Finding BLE Devices
     *  To find BLE devices, you use the startLeScan() method.
     *  This method takes a BluetoothAdapter.LeScanCallback as a parameter.
     *  You must implement this callback, because that is how scan results are returned.
     *  Because scanning is battery-intensive, you should observe the following guidelines:
     *
     *  → As soon as you find the desired device, stop scanning.
     *  → Never scan on a loop, and set a time limit on your scan.
     *     A device that was previously available may have moved out of range, and continuing to scan drains the battery.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    else
                        mLEScanner.stopScan(mScanCallback);

                }
            }, SCAN_PERIOD);

            mScanning = true;

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            else
                mLEScanner.startScan(mScanCallback);

        } else {
            mScanning = false;

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            else
                Log.i("BLE", "Scanning..");
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        // Callback when a BLE advertisement has been found.
        // 1 Device Detected.
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            final BluetoothDevice device = result.getDevice();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // mLeDeviceListAdapter.addDevice(device);
                    // mLeDeviceListAdapter.notifyDataSetChanged();
                    connectBluetoothLE(device);
                }
            });
        }

        // Callback when batch results are delivered.
        // Lots of Device Detected.
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult sr : results)
                Log.i("result", sr.toString());
        }

        // Callback when scan could not be started.
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("Scan Failed", "Error Code ".concat(String.valueOf(errorCode)));
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // mLeDeviceListAdapter.addDevice(device);
                    // mLeDeviceListAdapter.notifyDataSetChanged();
                    Log.i("onLeScan", device.toString());
                    connectBluetoothLE(device);
                }
            });
        }
    };

    private void connectBluetoothLE(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        scanLeDevice(false);
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> bluetoothGattServices = gatt.getServices();

            enabled = true;
            mBluetoothGattService = bluetoothGattServices.get(0);
            mCharacteristic = mBluetoothGattService.getCharacteristics().get(0);
            mBluetoothGatt.setCharacteristicNotification(mCharacteristic, enabled);

            // mCharacteristic.setValue("BLE Connected.".getBytes());
            // mCharacteristic.setValue(time().getBytes());
            // mBluetoothGatt.writeCharacteristic(mCharacteristic);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "BLE Connected.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String str = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            Log.i("onCharacteristicRead", characteristic.toString());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @Override
    public Activity getActivity() {
        return this;
    }

    class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    @Override
    public void receivedSMS(String msg) {
        // mCharacteristic.setValue(msg.getBytes());
        // mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    @Override
    public void receivedIncomingCall(String msg) {
        // mCharacteristic.setValue(msg.getBytes());
        // mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    private String time(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
        String time = simpleDateFormat.format(date);
        return time;
    }
}
