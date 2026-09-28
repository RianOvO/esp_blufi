## 0.0.1

* Allows Blufi connection to your ESP BLUFI configured devices
* Dynamically pass wifi credentials to your ESP device to connect to WiFi

## 0.0.2

* iOS open/ssl header files link

## 0.0.3

* Bug fix

## 0.0.4

* Bug fix

## 0.0.5

* Bug fix

## 0.0.6

* Scan Wi-Fi from device

## 0.0.7

* fix bug

## 0.1.0

* Support the latest Android and iOS releases.
* Android: request `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` at runtime on Android 12+ (location on
  Android 11 and earlier) and start the scan once they are granted.
* Android: use the Android 13+ GATT APIs (`writeCharacteristic` / `writeDescriptor` with values,
  `onCharacteristicChanged` with value).
* Android: build with AGP 9, compileSdk 36 and Java 17; remove the `package` manifest attribute
  and the `BuildConfig` dependency, which newer AGP versions reject.
* Android: keep BLUFI packets within the negotiated MTU.
* Android: fix crashes when methods are called before connecting or without an attached Activity.
* iOS: remove the bundled OpenSSL 1.0 static libraries, which had no arm64 simulator slice and
  failed to link with current Xcode; Diffie-Hellman now uses a built-in implementation.
* iOS: minimum deployment target 13.0, add a privacy manifest.
* iOS: request Bluetooth access on the first scan instead of at app launch, and connect directly
  to known peripherals.
* Both: every method call now completes its `Future`.
