# esp_blufi

Wi-Fi provisioning for Espressif devices over the
[BLUFI](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-guides/ble/blufi.html)
protocol, for Android and iOS. The app scans for the device over Bluetooth LE, connects to it and
sends it the SSID and password of the Wi-Fi network to join.

## Requirements

| Platform | Minimum | Notes |
| --- | --- | --- |
| Android | API 21 (Android 5.0) | Built with compileSdk 36 (Android 16), AGP 9.1, Java 17 |
| iOS | iOS 13 | Swift Package Manager or CocoaPods. No prebuilt binaries, so it builds for devices and for arm64 simulators |
| Flutter | 3.27 | Developed with Flutter 3.47 |

Bluetooth is not available in the iOS simulator or the Android emulator, so provisioning has to
be tested on a real phone.

## Installation

```yaml
dependencies:
  esp_blufi: ^0.2.0
```

## Platform setup

### Android

The plugin declares the Bluetooth permissions it needs in its own manifest, so nothing has to be
added to your app manifest.

* **Android 12 and later:** `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` are requested at runtime the
  first time `scanDeviceInfo()` is called. `BLUETOOTH_SCAN` is declared with `neverForLocation`,
  so no location permission is needed. If your app derives location from BLE scans, remove the
  flag in your app manifest:

  ```xml
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
      tools:remove="android:usesPermissionFlags" />
  ```

* **Android 11 and earlier:** `ACCESS_FINE_LOCATION` is requested at runtime, and location
  services must be turned on for BLE scans to return results.

If the user denies the permissions, a `permission_denied` message is sent and no scan starts.

### iOS

Add a Bluetooth usage description to `ios/Runner/Info.plist`. iOS terminates the app on the first
Bluetooth access without it:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Bluetooth is used to find nearby devices and send them Wi-Fi settings.</string>
```

The Bluetooth permission prompt appears the first time `scanDeviceInfo()` is called, not at app
launch. The plugin ships a privacy manifest (`PrivacyInfo.xcprivacy`).

Both Swift Package Manager and CocoaPods are supported. With Swift Package Manager enabled
(`flutter config --enable-swift-package-manager`), Flutter uses `ios/esp_blufi/Package.swift`;
otherwise it uses the podspec.

## Usage

Methods return once the request has been sent. Results arrive asynchronously as JSON strings in
the `onMessageReceived` callback, so register it first.

```dart
import 'dart:convert';

import 'package:esp_blufi/esp_blufi.dart';

final blufi = EspBlufi();

void listen() {
  blufi.onMessageReceived(successCallback: (String? message) {
    if (message == null) return;
    final json = jsonDecode(message) as Map<String, dynamic>;
    final key = json['key'];
    final value = json['value'];

    switch (key) {
      case 'ble_scan_result':
        // value: {"address": "...", "name": "BLUFI_DEVICE", "rssi": "-50"}
        break;
      case 'GATT_SUCCESS':
        // Connected and ready: negotiate encryption before sending the password.
        blufi.negotiateSecurity();
        break;
      case 'negotiate_security':
        // "1": everything sent from now on is encrypted. "0": negotiation failed;
        // do not send the password unencrypted, disconnect and retry instead.
        break;
      case 'wifi_info':
        // value: {"ssid": "...", "rssi": "-60", "address": "..."}
        break;
      case 'configure_params':
        // "1": settings sent, "0": failed
        break;
      case 'device_wifi_connect':
        // "1": the device joined the Wi-Fi network, "0": it did not
        break;
    }
  });
}

Future<void> provision(String address, String ssid, String password) async {
  await blufi.scanDeviceInfo(filterString: 'BLUFI'); // only devices whose name contains BLUFI
  // ... pick a device from the ble_scan_result messages ...
  await blufi.stopScan();
  await blufi.connectPeripheral(peripheralAddress: address);
  // ... wait for GATT_SUCCESS, call negotiateSecurity() and wait for negotiate_security "1" ...
  await blufi.configProvision(username: ssid, password: password);
  await blufi.requestDeviceStatus();
  await blufi.requestCloseConnection();
}
```

## API

| Method | Description |
| --- | --- |
| `onMessageReceived({successCallback, errorCallback})` | Registers the callback that receives every message below. |
| `scanDeviceInfo({String? filterString})` | Starts a BLE scan, requesting permissions first when needed. Only devices whose name contains `filterString` (case-insensitive) are reported. Without a filter, Android reports every named device and iOS uses `BLUFI`. |
| `stopScan()` | Stops the BLE scan. |
| `connectPeripheral({String? peripheralAddress})` | Connects to a device from `ble_scan_result`. On Android the address is the MAC address; on iOS it is the peripheral UUID. |
| `negotiateSecurity()` | Negotiates an encryption key with the device (DH key exchange). Once `negotiate_security` reports `"1"`, all data sent afterwards is AES-encrypted and checksummed. Call it after `GATT_SUCCESS` and before `configProvision()`. |
| `requestDeviceVersion()` | Asks the device for its BLUFI version, reported as `device_version`. |
| `requestDeviceWifiScan()` | Asks the device to scan for Wi-Fi networks. Each network arrives as a `wifi_info` message. |
| `configProvision({String? username, String? password})` | Sends the SSID (`username`) and password in station mode. |
| `requestDeviceStatus()` | Asks the device whether it is connected to Wi-Fi. |
| `sendCustomData({String? data})` | Sends custom data to the device firmware. |
| `requestCloseConnection()` | Closes the BLE connection. |
| `getAllPairedDevice()` | Android only: reports bonded devices as `each_connected_device` messages. |
| `getPlatformVersion()` | Returns the OS name and version. |

Without `negotiateSecurity()`, BLUFI data, including the Wi-Fi password, is sent over Bluetooth
unencrypted.

## Messages

Every message is a JSON string with a `key` and a `value`. Most messages also carry the `address`
of the connected device.

| `key` | `value` | Platform |
| --- | --- | --- |
| `ble_scan_result` | `{"address", "name", "rssi"}` | Both |
| `stop_scan_ble` | `"1"` | Both |
| `permission_denied` | `"1"` | Android |
| `peripheral_connect` | `"1"` connected, `"0"` failed or disconnected | Both |
| `peripheral_disconnect` | `"1"` | Both |
| `GATT_SUCCESS` | `"1"`: services discovered, ready to send requests | Both |
| `blufi_connect_prepared` | `"1"` ready; `"2"` / `"3"` / `"4"` service / write / notify characteristic missing | iOS |
| `discover_service` | `"1"` found, `"0"` BLUFI service missing | Android |
| `wifi_info` | `{"ssid", "rssi", "address"}`, or `"0"` when the Wi-Fi scan failed | Both |
| `configure_params` | `"1"` sent, `"0"` failed | Both |
| `device_status` | `"1"` received (iOS), `"0"` request failed | Both |
| `device_wifi_connect` | `"1"` device joined the Wi-Fi network, `"0"` not joined | Both |
| `negotiate_security` | `"1"` encryption enabled, `"0"` failed or not connected | Both |
| `device_version` | BLUFI version of the device, `"0"` on failure or when not connected | Both |
| `post_custom_data` | `"1"` sent, `"0"` failed | Both |
| `receive_device_custom_data` | Custom data sent by the device, `"0"` on failure | Both |
| `receive_error_code` | BLUFI error code | Android |
| `gatt_write_timeout` | A write timed out and the connection was closed | Android |
| `gatt_disconnected` | `"1"` | Android |

Android also sends some debug messages (for example `request_mtu` and `onCharacteristicWrite`);
ignore keys you do not handle.

## Upgrading from 0.1.x

* iOS: the minimum version is now iOS 13, and the bundled OpenSSL libraries are gone. Run
  `pod install` again, or let Flutter migrate the project to Swift Package Manager.
* Android: the app no longer needs to request Bluetooth or location permissions itself before
  calling `scanDeviceInfo()`.
* Every method now completes its `Future`, so awaiting it no longer hangs.
* New `negotiateSecurity()` and `requestDeviceVersion()`. Call `negotiateSecurity()` before
  `configProvision()` so the Wi-Fi password is not sent in plain text.
* Every Android message is now JSON; the plain-text negotiation messages were removed.

## License

Apache License 2.0, see [LICENSE](LICENSE).
