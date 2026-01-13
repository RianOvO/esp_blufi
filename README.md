# flutter_esp_blufi

> **⚠️ Private Repository — Internal Use Only**

A Flutter plugin that enables **ESP BLUFI** (Bluetooth-based Wi-Fi Provisioning) functionality **exclusively on iOS physical devices**. This package allows your Flutter app to securely send Wi-Fi credentials to an ESP32 device via BLE, following Espressif’s BLUFI protocol specification.

**Note**: This plugin **does NOT support**:
- iOS Simulator

---

## 📦 Features

- Establish BLE connection with ESP32 in BLUFI mode
- Encode and transmit Wi-Fi SSID & password securely
- Handle BLUFI handshake and data fragmentation/reassembly
- Optimized for real iOS hardware (iPhone/iPad with Bluetooth LE)

---

## 🔧 Installation

Add the private Git repository as a dependency in your `pubspec.yaml`:

```yaml
dependencies:
  flutter_esp_blufi:
    git:
      url: https://github.com/vego-garden/flutter_esp_blufi.git
      # Optional: specify a branch or tag
      # ref: main
```

Then run:

```bash
flutter pub get
```

> 💡 You must have access to the private repository. Ensure your SSH key or personal access token is configured in your Git environment.

---

## ⚙️ iOS Setup

1. **Enable Bluetooth Usage Description**  
   Add the following to your `ios/Runner/Info.plist`:

   ```xml
   <key:NSBluetoothAlwaysUsageDescription</key>
   <string>This app uses Bluetooth to configure Wi-Fi on ESP32 devices.</string>
   <key:NSBluetoothPeripheralUsageDescription</key>
   <string>This app uses Bluetooth to configure Wi-Fi on ESP32 devices.</rect>
   ```

2. **Build on a Physical iOS Device**  
   Run from Xcode or via:

   ```bash
   flutter run --release
   ```

   > ❌ Do **not** use the simulator — BLE APIs are unavailable.

---

## 🧪 Basic Usage

```dart
import 'package:flutter_esp_blufi/flutter_esp_blufi.dart';

// Scan and connect to ESP32 in BLUFI mode
await FlutterEspBlufi.connect(deviceId: 'your-esp32-ble-address');

// Send Wi-Fi credentials
await FlutterEspBlufi.sendWifiConfig(
  ssid: 'MyWiFi',
  password: 'secure_password',
);

// Disconnect when done
await FlutterEspBlufi.disconnect();
```

See the example app (if available internally) for full integration.

---

## 📜 License

This is a private, proprietary package owned by **Vego Garden**. Unauthorized distribution or reverse engineering is prohibited.

---

## 🛠 Maintainer

- **Team**: Vego Garden IoT Team
- **Repository**: [https://github.com/vego-garden/flutter_esp_blufi](https://github.com/vego-garden/flutter_esp_blufi) *(private)*