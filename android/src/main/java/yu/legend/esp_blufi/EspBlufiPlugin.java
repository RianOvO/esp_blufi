package yu.legend.esp_blufi;
import android.Manifest;
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
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import yu.legend.esp_blufi.params.BlufiConfigureParams;
import yu.legend.esp_blufi.params.BlufiParameter;
import yu.legend.esp_blufi.response.BlufiScanResult;
import yu.legend.esp_blufi.response.BlufiStatusResponse;
import yu.legend.esp_blufi.response.BlufiVersionResponse;

/** EspBlufiPlugin */
public class EspBlufiPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware,
        PluginRegistry.RequestPermissionsResultListener {
  private static final int REQUEST_BLE_PERMISSIONS = 1452;
  private String mPendingScanFilter;
  private Result mPendingScanResult;
  private List<ScanResult> mBleList;
  private Map<String, ScanResult> mDeviceMap;
  private ScanCallback mScanCallback;
  private String mBlufiFilter;
  private volatile long mScanStartTime;
  private ExecutorService mThreadPool;
  private Future mUpdateFuture;
  private BluetoothDevice mDevice;
  private BlufiClient mBlufiClient;
  private volatile boolean mConnected;
  private Context mContext;
  private ActivityPluginBinding activityBinding;
  private EventChannel stateChannel;
  private EventChannel.StreamHandler streamHandler;
  private EventChannel.EventSink sink;
  private final BlufiLog mLog = new BlufiLog(getClass());
  private Handler handler;
  private Activity activity;

  private BluetoothManager mBluetoothManager;

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    handler = new Handler(Looper.getMainLooper());
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "esp_blufi");
    channel.setMethodCallHandler(this);
    stateChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "esp_blufi/state");
//        channel.setMethodCallHandler(new FlutterBlufiPlugin(flutterPluginBinding.activity()));
    streamHandler = new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object arguments, EventChannel.EventSink events) {
        sink = events;
      }

      @Override
      public void onCancel(Object arguments) {
        sink = null;

      }
    };

    stateChannel.setStreamHandler(streamHandler);
    stateChannel.setStreamHandler(streamHandler);
    mContext = flutterPluginBinding.getApplicationContext();
    mThreadPool = Executors.newSingleThreadExecutor();
    mBleList = new LinkedList<>();
    mDeviceMap = new HashMap<>();
    mScanCallback = new ScanCallback();
    mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
  }

  /**
   * Runtime permissions needed to scan and connect:
   * Android 12+ uses BLUETOOTH_SCAN / BLUETOOTH_CONNECT, older versions need location for BLE scans.
   */
  private String[] requiredBlePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
    }
    return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
  }

  private boolean hasBlePermissions() {
    for (String permission : requiredBlePermissions()) {
      if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasConnectPermission() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            || ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
  }

  private boolean hasScanPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }
    return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode != REQUEST_BLE_PERMISSIONS) {
      return false;
    }
    Result result = mPendingScanResult;
    String filter = mPendingScanFilter;
    mPendingScanResult = null;
    mPendingScanFilter = null;
    if (result == null) {
      return true;
    }
    if (hasBlePermissions()) {
      scan(filter, result);
    } else {
      updateMessage(makeJson("permission_denied", "1"));
      result.success(false);
    }
    return true;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + Build.VERSION.RELEASE);
    } else if (call.method.equals("testFunction")) {
      result.success("called testFunction and code is triggered in Android....");
      Log.d("esp_blufi_tcm", "This is my message");
//            if (this.activity.isInitialised){
      if (this.activity != null) {
        Toast.makeText(this.activity, "Android native code is trigggered from android plugin....", Toast.LENGTH_LONG).show();
      }
//            }

    } else if (call.method.equals("scanDeviceInfo")) {
      String filter = call.argument("filter");
      if (hasBlePermissions()) {
        scan(filter, result);
      } else if (activity == null) {
        result.error("no_activity", "Bluetooth permissions are required but no Activity is attached to request them", null);
      } else if (mPendingScanResult != null) {
        result.error("permission_request_in_progress", "A Bluetooth permission request is already running", null);
      } else {
        mPendingScanFilter = filter;
        mPendingScanResult = result;
        ActivityCompat.requestPermissions(activity, requiredBlePermissions(), REQUEST_BLE_PERMISSIONS);
      }
    } else if (call.method.equals("stopScan")) {
      stopScan();
      result.success(null);
    } else if (call.method.equals("connectPeripheral")) {
      String deviceId = call.argument("peripheral");
      ScanResult scanResult = deviceId == null ? null : mDeviceMap.get(deviceId);
      if (scanResult != null && hasConnectPermission()) {
        connectDevice(scanResult.getDevice());
        result.success(true);
      } else {
        result.success(false);
      }
    } else if (call.method.equals("requestCloseConnection")) {
      Log.d("esp_blufi_tcm", "request close connection received in android native code...");
      disconnectGatt();
      result.success(null);
    } else if (call.method.equals("requestDeviceWifiScan")) {
      Log.d("esp_blufi_tcm", "request device wifi scan in android native code...");
      requestDeviceWifiScan();
      result.success(null);
    } else if (call.method.equals("negotiateSecurity")) {
      negotiateSecurity();
      result.success(null);
    } else if (call.method.equals("requestDeviceVersion")) {
      requestDeviceVersion();
      result.success(null);
    } else if (call.method.equals("configProvision")) {
      Log.d("esp_blufi_tcm", "configProvision called in android plugin side");

      String userName = call.argument("username");
      String password = call.argument("password");
      configure(userName, password);
      result.success(null);
//            configure("The Coding Machine 2.4", "Tcm#pcw3626");
    } else if (call.method.equals("getAllPairedDevice")) {
//            updateMessage(makeJson("getAllPairedDevice called onMethodCall", "0x0x0x"));
      getAllPairedDevice();
      result.success(null);
    } else if (call.method.equals("requestDeviceStatus")) {
//            updateMessage(makeJson("requestDeviceStatus called onMethodCall", "0x0x0x"));
      Log.d("esp_blufi_tcm", "requestDeviceStatus is called on methodCall");
      requestDeviceStatus();
      result.success(null);
    } else if (call.method.equals("sendCustomData")) {
      Log.d("esp_blufi_tcm", "sendCustomData is called on methodCall");
      String data = call.argument("data");

      postCustomData(data);
      result.success(null);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    stateChannel.setStreamHandler(null);
    sink = null;
    if (mBlufiClient != null) {
      mBlufiClient.close();
      mBlufiClient = null;
    }
    if (mThreadPool != null) {
      mThreadPool.shutdownNow();
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
    attachToActivity(activityPluginBinding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    detachFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
    attachToActivity(activityPluginBinding);
  }

  @Override
  public void onDetachedFromActivity() {
    detachFromActivity();
    // The permission result can no longer arrive, complete the pending scan request
    if (mPendingScanResult != null) {
      mPendingScanResult.success(false);
      mPendingScanResult = null;
      mPendingScanFilter = null;
    }
  }

  private void attachToActivity(ActivityPluginBinding binding) {
    activityBinding = binding;
    activity = binding.getActivity();
    binding.addRequestPermissionsResultListener(this);
  }

  private void detachFromActivity() {
    if (activityBinding != null) {
      activityBinding.removeRequestPermissionsResultListener(this);
    }
    activityBinding = null;
    activity = null;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private class ScanCallback extends android.bluetooth.le.ScanCallback {

    @Override
    public void onScanFailed(int errorCode) {
      super.onScanFailed(errorCode);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      for (ScanResult result : results) {
        onLeScan(result);
      }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      onLeScan(result);
    }

    private void onLeScan(ScanResult scanResult) {
//            Log.d("Flutter_blufi_tcm", "running onLeScan .....");
      if (!hasConnectPermission()) {
        return;
      }

      String name = scanResult.getDevice().getName();
      if (name == null && scanResult.getScanRecord() != null) {
        name = scanResult.getScanRecord().getDeviceName();
      }

      if (!TextUtils.isEmpty(mBlufiFilter)) {
        if (name == null || !name.toLowerCase().contains(mBlufiFilter.toLowerCase())) {
          return;
        }
      }

      Log.v("ble scan", scanResult.getDevice().getAddress());

      if (name != null) {
        mDeviceMap.put(scanResult.getDevice().getAddress(), scanResult);
        updateMessage(makeScanDeviceJson(scanResult.getDevice().getAddress(), name, scanResult.getRssi()));
      }
    }

    private String makeScanDeviceJson(String address, String name, int rssi) {

      return String.format("{\"key\":\"ble_scan_result\",\"value\":{\"address\":\"%s\",\"name\":\"%s\",\"rssi\":\"%s\"}}", address, name, rssi);
    }
  }

  private void updateMessage(String message) {
    Log.v("message", message);

    if (sink != null) {
      handler.post(
              new Runnable() {
                @Override
                public void run() {
                  sink.success(message);
                }
              });
    }
  }

  private String makeJson(String command, String data) {

    String address = "";
    if (mDevice != null) {
      address = mDevice.getAddress();
    }
    return String.format("{\"key\":\"%s\",\"value\":\"%s\",\"address\":\"%s\"}", command, data, address);
  }

  private String makeWifiInfoJson(String ssid, int rssi) {
    String address = "";
    if (mDevice != null) {
      address = mDevice.getAddress();
    }
    return String.format("{\"key\":\"wifi_info\",\"value\":{\"ssid\":\"%s\",\"rssi\":\"%s\",\"address\":\"%s\"}}", ssid, rssi, address);
  }

  private void getAllPairedDevice() {
    if (mBluetoothManager == null || mBluetoothManager.getAdapter() == null || !hasConnectPermission()) {
      return;
    }
    Set<BluetoothDevice> myBondedDevices = mBluetoothManager.getAdapter().getBondedDevices();
    if (myBondedDevices == null) {
      return;
    }
    for (BluetoothDevice device : myBondedDevices) {
      String nameAndDeviceAddess = "name: " + device.getName() + " address: " + device.getAddress();
      updateMessage(makeJson("each_connected_device", nameAndDeviceAddess));
    }
  }

  private void scan(String filter, Result result) {
    System.out.println("calling scan()....");

    startScan21(filter, result);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void startScan21(String filter, Result result) {
    BluetoothAdapter adapter = mBluetoothManager == null ? null : mBluetoothManager.getAdapter();
    if (adapter == null || !adapter.isEnabled()) {
      mLog.d("Bluetooth adapter is not available or not enabled");
      result.success(false);
      return;
    }
    if (!hasScanPermission()) {
      mLog.d("Bluetooth scan permission is not granted");
      result.success(false);
      return;
    }
    BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
    if (scanner == null) {
      result.success(false);
      return;
    }
    mDeviceMap.clear();
    mBleList.clear();
    mBlufiFilter = filter;
    mScanStartTime = SystemClock.elapsedRealtime();

    mLog.d("Start scan ble from Android side");
    try {
      scanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
              mScanCallback);
      result.success(true);
    } catch (SecurityException e) {
      mLog.w("Start scan failed: " + e.getMessage());
      result.success(false);
    }
  }

  private void stopScan() {
    stopScan21();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void stopScan21() {
    BluetoothAdapter adapter = mBluetoothManager == null ? null : mBluetoothManager.getAdapter();
    BluetoothLeScanner scanner = adapter == null ? null : adapter.getBluetoothLeScanner();

    if (scanner != null && hasScanPermission()) {
      try {
        scanner.stopScan(mScanCallback);
      } catch (SecurityException | IllegalStateException e) {
        mLog.w("Stop scan failed: " + e.getMessage());
      }
    }
    if (mUpdateFuture != null) {
      mUpdateFuture.cancel(true);
    }
    mLog.d("Stop scan ble");
    updateMessage(makeJson("stop_scan_ble", "1"));
  }

  void connectDevice(BluetoothDevice device) {
//        if (mDevice != null) {
//            mDevice = null;
//        }
    mDevice = device;
//        mBlufiClient = null;
    if (mBlufiClient != null) {
      mBlufiClient.close();
      mBlufiClient = null;
    }

    mBlufiClient = new BlufiClient(mContext, mDevice);
    mBlufiClient.setGattCallback(new GattCallback());
    mBlufiClient.setBlufiCallback(new BlufiCallbackMain());
    mBlufiClient.setGattWriteTimeout(BlufiParameter.GATT_WRITE_TIMEOUT);
    mBlufiClient.connect();
  }

  private void disconnectGatt() {

    if (mDevice != null) {
      mLog.d("mDevice is not null");
//            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
      if (!hasConnectPermission()) {
        return;
      }
      mLog.d(mDevice.getName());
    }
    if (mBlufiClient != null) {
      mLog.d("disconnectGatt: mBlufiClient != null");

      mBlufiClient.requestCloseConnection();
//            mBlufiClient = null;
    } else {
      mLog.d("disconnectGatt: mBlufiClient == null");
    }
  }

  /**
   * If negotiate security success, the continue communication data will be encrypted.
   */
  private void negotiateSecurity() {
    if (mBlufiClient != null) {
      mBlufiClient.negotiateSecurity();
    }
  }


  private void configure(String userName, String password) {
    if (mBlufiClient == null || userName == null) {
      updateMessage(makeJson("configure_params", "0"));
      return;
    }
    if (password == null) {
      password = "";
    }
    BlufiConfigureParams params = new BlufiConfigureParams();
    params.setOpMode(1);
    byte[] ssidBytes = userName.getBytes(StandardCharsets.UTF_8);
    params.setStaSSIDBytes(ssidBytes);
    params.setStaPassword(password);
    mBlufiClient.configure(params);
  }

  /**
   * Request to get device current status
   */
  private void requestDeviceStatus() {

    if (mBlufiClient != null) {
      mBlufiClient.requestDeviceStatus();
    }
  }

  /**
   * Request to get device blufi version
   */
  private void requestDeviceVersion() {
    if (mBlufiClient != null) {
      mBlufiClient.requestDeviceVersion();
    }
  }

  /**
   * Request to get AP list that the device scanned
   */
  private void requestDeviceWifiScan() {
    if (mBlufiClient != null) {
      mBlufiClient.requestDeviceWifiScan();
    }
  }

  /**
   * Try to post custom data
   */
  private void postCustomData(String dataString) {
    if (dataString != null && mBlufiClient != null) {
      mBlufiClient.postCustomData(dataString.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void onGattConnected() {
    mConnected = true;
  }

  private void onGattDisconnected() {
    mConnected = false;
    updateMessage(makeJson("gatt_disconnected", "1"));
//        updateMessage(makeJson("gatt_disconnected_updated_plugin_x5", "1"));
//
//        if(mBlufiClient != null){
//            mBlufiClient = null;
//        }
//        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//        if (ContextCompat.checkSelfPermission(mContext,Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
//            updateMessage(makeJson("connected_devices_scanning", "0x0x0x"));
//            Set<BluetoothDevice> myBondedDevices = adapter.getBondedDevices();
//
//
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                myBondedDevices.forEach((BluetoothDevice e) -> {
////                    System.out.println(e);
//                    String uuid = e.getUuids().toString();
//                    updateMessage(makeJson("each_connected_device",uuid ));
//                    try {
//                        Method m = e.getClass()
//                                .getMethod("removeBond", (Class[]) null);
//                        m.invoke(e, (Object[]) null);
//                    } catch (Exception ex) {
////                        Log.e("Removing has been failed.", ex.getMessage());
//                        updateMessage(makeJson("Removing has been failed.", ex.getMessage()));
//                    }
//
//                });
//            }
//        }
  }

  private void onGattServiceCharacteristicDiscovered() {

  }


  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private class GattCallback extends BluetoothGattCallback {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String devAddr = gatt.getDevice().getAddress();
      mLog.d(String.format(Locale.ENGLISH, "onConnectionStateChange addr=%s, status=%d, newState=%d",
              devAddr, status, newState));
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d("Flutter_blufi_tcm", "BluetoothGatt.GATT_SUCCESS");
        switch (newState) {
          case BluetoothProfile.STATE_CONNECTED:
            onGattConnected();
            updateMessage(makeJson("peripheral_connect", "1"));
//            updateMessage(String.format("Connected %s", devAddr), false);
            break;
          case BluetoothProfile.STATE_DISCONNECTED:
//                        if (ContextCompat.checkSelfPermission(mContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                            return;
//                        }
            if (!hasConnectPermission()) {
              return;
            }
            gatt.close();
            onGattDisconnected();
//                        if(mDevice != null){
//                            mDevice = null;
//                            updateMessage(makeJson("set_mdevice_null", "1"));
//                        }
            updateMessage(makeJson("peripheral_connect", "0"));
            updateMessage(makeJson("disconnected_device", "1"));
            break;
        }
      } else {
//                gatt.disconnect(); //added
        gatt.close();
        onGattDisconnected();
//                if(mDevice != null){
//                    mDevice = null;
//                    updateMessage(makeJson("set_mdevice_null_else", "1"));
//                }

        updateMessage(makeJson("peripheral_disconnect", "1"));
//        updateMessage(String.format(Locale.ENGLISH, "Disconnect %s, status=%d", devAddr, status),
//                false);
      }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
      mLog.d(String.format(Locale.ENGLISH, "onMtuChanged status=%d, mtu=%d", status, mtu));
      if (status == BluetoothGatt.GATT_SUCCESS) {
        // BLUFI frames carry a one-byte length; keep packets within the negotiated ATT MTU as well
        mBlufiClient.setPostPackageLengthLimit(Math.min(255, mtu - 4));
//        updateMessage(makeJson("peripheral_disconnect","1"));
        updateMessage(makeJson("Set mtu complete, mtu=%d ", Integer.toString(mtu)));
        updateMessage(makeJson("GATT_SUCCESS", "1"));
      } else {
        mBlufiClient.setPostPackageLengthLimit(20);
        updateMessage(makeJson("Set mtu failed, mtu=%d, status=%d", Integer.toString(mtu)));
      }

      onGattServiceCharacteristicDiscovered();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      mLog.d(String.format(Locale.ENGLISH, "onServicesDiscovered status=%d", status));
      if (status != BluetoothGatt.GATT_SUCCESS) {
//                if (ContextCompat.checkSelfPermission(mContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
        if (!hasConnectPermission()) {
          return;
        }
        gatt.disconnect();
        updateMessage(makeJson("discover_services", "1"));
//        updateMessage(String.format(Locale.ENGLISH, "Discover services error status %d", status));
      }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      mLog.d(String.format(Locale.ENGLISH, "onDescriptorWrite status=%d", status));
      if (descriptor.getUuid().equals(BlufiParameter.UUID_NOTIFICATION_DESCRIPTOR) &&
              descriptor.getCharacteristic().getUuid().equals(BlufiParameter.UUID_NOTIFICATION_CHARACTERISTIC)) {
        String msg = String.format(Locale.ENGLISH, "Set notification enable %s", (status == BluetoothGatt.GATT_SUCCESS ? " complete" : " failed"));
//        updateMessage(msg);
      }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

      updateMessage(makeJson("onCharacteristicWrite", "triggered..."));
      if (status != BluetoothGatt.GATT_SUCCESS) {
//                if (ContextCompat.checkSelfPermission(mContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
        if (!hasConnectPermission()) {
          return;
        }
        gatt.disconnect();
//                updateMessage(String.format(Locale.ENGLISH, "WriteChar status %d", status));
        updateMessage(makeJson("ON_CHARACTERISTICWRITE_GATT_DISCONNECT", "true"));
      } else {
//                updateMessage(String.format(Locale.ENGLISH, "WriteChar - failed status %d", status));
        updateMessage(makeJson("ON_CHARACTERISTICWRITE_GATT_DISCONNECT", "false"));
      }
    }
  }

  private class BlufiCallbackMain extends BlufiCallback {
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onGattPrepared(BlufiClient client, BluetoothGatt gatt, BluetoothGattService service,
                               BluetoothGattCharacteristic writeChar, BluetoothGattCharacteristic notifyChar) {

      updateMessage(makeJson("onGattPrepared_is_triggered", "0"));
      if (service == null) {
        mLog.w("Discover service failed");
//                if (ContextCompat.checkSelfPermission(mContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
        if (!hasConnectPermission()) {
          return;
        }
        gatt.disconnect();
//        updateMessage("Discover service failed");
        updateMessage(makeJson("discover_service", "0"));
        return;
      }
      if (writeChar == null) {
        mLog.w("Get write characteristic failed");
        gatt.disconnect();
//        updateMessage("Get write characteristic failed");
        updateMessage(makeJson("get_write_characteristic", "0"));
        return;
      }
      if (notifyChar == null) {
        mLog.w("Get notification characteristic failed");
        gatt.disconnect();
//        updateMessage("Get notification characteristic failed");
        updateMessage(makeJson("get_notification_characteristic", "0"));
        return;
      }
      updateMessage(makeJson("discover_service", "1"));
//      updateMessage("Discover service and characteristics success");

      int mtu = BlufiParameter.DEFAULT_MTU_LENGTH;
      mLog.d("Request MTUUUUU " + mtu);
      boolean requestMtu = false;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        requestMtu = gatt.requestMtu(mtu);

        if (!requestMtu) {
          mLog.w("Request mtu failed");
          updateMessage(makeJson("request_mtu", "0"));
          updateMessage(String.format(Locale.ENGLISH, "Request mtu %d failed", mtu));
          onGattServiceCharacteristicDiscovered();
        } else {
          updateMessage(makeJson("request_mtu", "1"));
        }
      }
    }

    @Override
    public void onNegotiateSecurityResult(BlufiClient client, int status) {
      if (status == STATUS_SUCCESS) {
        updateMessage("Negotiate security complete");
        updateMessage(makeJson("negotiate_security", "1"));
      } else {
        updateMessage("Negotiate security failed， code=" + status);
        updateMessage(makeJson("negotiate_security", "0"));
      }
    }

    @Override
    public void onPostConfigureParams(BlufiClient client, int status) {
      System.out.println("Post configure params complete");
      if (status == STATUS_SUCCESS) {
        updateMessage(makeJson("configure_params", "1"));
      } else {
        updateMessage(makeJson("configure_params", "0"));
      }
    }

    @Override
    public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
      System.out.println("onDeviceStatusResponse()");
      System.out.println(response);
      if (status == STATUS_SUCCESS) {
//                updateMessage(makeJson("device_status", "1"));
//        updateMessage(String.format("Receive device status response:\n%s"));
        if (response.isStaConnectWifi()) {
//                    updateMessage(makeJson("device_status", "1"));
          updateMessage(makeJson("device_wifi_connect", "1"));
        } else {
//                    updateMessage(makeJson("device_wifi_connect", "0"));
          updateMessage(makeJson("device_wifi_connect", "0"));
        }
      } else {
        updateMessage(makeJson("device_status", "0"));
//                updateMessage(makeJson("device_status", "0"));
//        updateMessage("Device status response error, code=" + status);
      }
    }

    @Override
    public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
      if (status == STATUS_SUCCESS) {
//        StringBuilder msg = new StringBuilder();
//        msg.append("Receive device scan result:\n");
        for (BlufiScanResult scanResult : results) {
//          msg.append(scanResult.toString()).append("\n");
          updateMessage(makeWifiInfoJson(scanResult.getSsid(), scanResult.getRssi()));
        }
//        updateMessage(msg.toString());
      } else {
        updateMessage(makeJson("wifi_info", "0"));
//        updateMessage("Device scan result error, code=" + status);
      }
    }

    @Override
    public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
      if (status == STATUS_SUCCESS) {
        updateMessage(makeJson("device_version", response.getVersionString()));
//        updateMessage(String.format("Receive device version: %s", response.getVersionString()));
      } else {
        updateMessage(makeJson("device_version", "0"));
//        updateMessage("Device version error, code=" + status);
      }

    }

    @Override
    public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
      String dataStr = new String(data);
      String format = "Post data %s %s";
      if (status == STATUS_SUCCESS) {
        updateMessage(makeJson("post_custom_data", "1"));
//        updateMessage(String.format(format, dataStr, "complete"));
      } else {
        updateMessage(makeJson("post_custom_data", "0"));
//        updateMessage(String.format(format, dataStr, "failed"));
      }
    }

    @Override
    public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
      if (status == STATUS_SUCCESS) {
        String customStr = new String(data);
        System.out.println("constructed String from bytes");
        System.out.println(customStr);
        if (!customStr.contains("DISCONN")) {
          customStr = customStr.replace("\"", "\\\"");
          updateMessage(makeJson("receive_device_custom_data", customStr));
        } else {
          customStr = customStr.trim();
          updateMessage(makeJson("receive_device_custom_data", customStr));
        }

//        updateMessage(String.format("Receive custom data:\n%s", customStr));
//                updateMessage(makeJson("receive_device_custom_data", customStr));
      } else {
        updateMessage(makeJson("receive_device_custom_data", "0"));
        System.out.println("receive custom data error, code=" + status);
//        updateMessage(String.format("Receive custom data error, code=" + status);
      }
    }

    @Override
    public void onError(BlufiClient client, int errCode) {
      updateMessage(makeJson("receive_error_code", errCode + ""));
      if (errCode == CODE_GATT_WRITE_TIMEOUT) {
        updateMessage(makeJson("gatt_write_timeout", "false"));
        client.close();
        onGattDisconnected();
      }
//      updateMessage(String.format(Locale.ENGLISH, "Receive error code %d", errCode));
    }
  }

}
