## 0.2.0

* Support the latest Android and iOS releases.
* iOS: support Swift Package Manager (the CocoaPods podspec is kept).
* iOS: remove the bundled OpenSSL static libraries; Diffie-Hellman now uses a small built-in
  implementation, so there is nothing prebuilt to update for new Xcode / simulator architectures.
  The shared secret keeps the 0.1.2 behaviour of dropping leading zero bytes, as the device does.
* iOS: minimum deployment target 13.0, add a privacy manifest.
* iOS: request Bluetooth access on the first scan instead of at app launch, connect directly to
  known peripherals, and do not block on writes when the peripheral is gone.
* Android: request `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` at runtime on Android 12+ (location on
  Android 11 and earlier) and start the scan once they are granted.
* Android: use the Android 13+ GATT APIs (`writeCharacteristic` / `writeDescriptor` with values,
  `onCharacteristicChanged` with value).
* Android: build with AGP 9, compileSdk 36 and Java 17; remove the `package` manifest attribute,
  which newer AGP versions reject.
* Android: drop leading zero bytes from the DH shared secret, matching the device and iOS.
* Android: keep BLUFI packets within the negotiated MTU.
* Android: fix crashes when methods are called before connecting or without an attached Activity.
* Both: every method call now completes its `Future`.

## 0.1.8

* update README.md

## 0.1.7

* bug fix

## 0.1.6

* bug fix

## 0.1.5

* Update OpenSSL to version 1.1.1w to support iOS simulator arm64 26.2

## 0.1.4

* Update OpenSSL to version 1.1.1w to support iOS simulator arm64 26.2

## 0.1.3

* upgrade gradle

## 0.1.2

* Fix dh secret

## 0.1.1

* Update openssl to 1.1.1w for iPhoneOS 18.2-arm64

## 0.1.0

* Update IPHONEOS_DEPLOYMENT_TARGET from 11.0 to 12.0

## 0.0.9

* Update gradle build tools to 8.5.2

## 0.0.7

* fix bug

## 0.0.8

* Android Wi-Fi connect status result

## 0.0.6

* Scan Wi-Fi from device

## 0.0.5

* Bug fix
## 0.0.4

* Bug fix

## 0.0.3

* Bug fix

## 0.0.2

* iOS open/ssl header files link

## 0.0.1

* Allows Blufi connection to your ESP BLUFI configured devices
* Dynamically pass wifi credentials to your ESP device to connect to WiFi















