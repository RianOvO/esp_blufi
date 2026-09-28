# esp_blufi

Wi-Fi provisioning for Espressif devices over the [BLUFI](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-guides/ble/blufi.html) protocol, for Android and iOS.

## Requirements

| Platform | Minimum | Built against |
| --- | --- | --- |
| Android | API 21 (Android 5.0) | compileSdk 36 (Android 16), AGP 9.1, Java 17 |
| iOS | iOS 13 | Swift Package Manager or CocoaPods; no prebuilt binaries, so it builds for device and arm64 simulators |
| Flutter | 3.27 | 3.47 |

## Setup

### Android

The plugin declares every Bluetooth permission it needs in its own manifest; nothing has to be
added to your app manifest.

* Android 12 and later: `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` are requested at runtime the
  first time `scanDeviceInfo()` is called. `BLUETOOTH_SCAN` is declared with
  `neverForLocation`, so no location permission is needed.
  If your app derives location from BLE scans, remove the flag in your app manifest:

  ```xml
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
      tools:remove="android:usesPermissionFlags" />
  ```

* Android 11 and earlier: `ACCESS_FINE_LOCATION` is requested at runtime and location services
  must be turned on for BLE scans to return results.

If the user denies the permissions, `scanDeviceInfo()` completes with `false` and a
`permission_denied` message is sent to the `onMessageReceived` callback.

### iOS

Add a Bluetooth usage description to `ios/Runner/Info.plist`; iOS terminates the app on the
first Bluetooth access without it:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Bluetooth is used to find nearby devices and send them Wi-Fi settings.</string>
```

The Bluetooth permission prompt is shown the first time `scanDeviceInfo()` is called, not at
app launch. The plugin ships a privacy manifest (`PrivacyInfo.xcprivacy`).

The plugin supports both Swift Package Manager and CocoaPods. When Swift Package Manager is
enabled (`flutter config --enable-swift-package-manager`, the default on recent Flutter
versions), Flutter uses `ios/esp_blufi/Package.swift`; otherwise it falls back to the podspec.

## Usage

```dart
final blufi = EspBlufi();

blufi.onMessageReceived(successCallback: (String? json) {
  // e.g. {"key":"ble_scan_result","value":{"address":"...","name":"BLUFI_DEVICE","rssi":"-50"}}
});

await blufi.scanDeviceInfo(filterString: 'BLUFI');
await blufi.stopScan();
await blufi.connectPeripheral(peripheralAddress: address);
await blufi.configProvision(username: ssid, password: password);
await blufi.requestDeviceStatus();
await blufi.requestCloseConnection();
```

See `example/lib/main.dart` for a complete app.
