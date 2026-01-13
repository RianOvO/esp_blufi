# ESP BLUFI Simulator (ARM64)

A Flutter plugin project that provides a simulation environment for the **ESP BLUFI** (Bluetooth-based Wi-Fi Provisioning) protocol, optimized for **ARM64** architecture devices.

> **Note**: This repository is currently a boilerplate/starter template for a Flutter plugin with platform-specific (Android/iOS) implementation code. It does not yet contain functional BLUFI logic but serves as the foundation for future development.

## 📦 What is BLUFI?

BLUFI (Bluetooth-based Wi-Fi Provisioning) is a protocol developed by Espressif Systems that allows users to configure Wi-Fi credentials on an ESP32 or ESP32-S series device via Bluetooth Low Energy (BLE), without needing to connect to a network first.

This simulator aims to replicate BLUFI behavior on ARM64 mobile platforms (e.g., modern Android phones or iOS devices with Apple Silicon) for testing and development purposes.

## 🚀 Features (Planned)

- Simulate BLE communication flow of ESP BLUFI
- Support Wi-Fi credential encoding/decoding as per Espressif’s BLUFI spec
- Cross-platform support via Flutter (Android & iOS)
- ARM64-optimized native bindings

## 🛠️ Getting Started

### Prerequisites

- Flutter SDK (latest stable version)
- Dart SDK
- Android Studio / Xcode (for platform-specific builds)
- Basic understanding of BLE and ESP32 provisioning

### Installation

Add this plugin to your Flutter project:

```yaml
dependencies:
  esp_blufi_simulator_arm6: 
    git:
      url: https://github.com/pengyu0o0/esp_blufi_simulator_arm64.git
```

Then run:

```bash
flutter pub get
```

> ⚠️ **Warning**: This package is currently a skeleton and does not provide usable APIs. Please check back for updates.

## 🧪 Development Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/pengyu0o0/esp_blufi_simulator_arm64.git
   cd esp_blufi_simulator_arm64
   ```

2. Open in your IDE (VS Code / Android Studio).

3. Implement platform-specific BLUFI logic in:
    - `android/src/main/...` (Kotlin/Java)
    - `ios/Classes/...` (Swift/Objective-C)

4. Use `lib/esp_blufi_simulator_arm64.dart` to expose Dart APIs.

## 📄 License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

## 🙌 Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your enhancements, especially around:
- BLUFI frame parsing
- Security handshake simulation
- Real-device BLE integration

## 📬 Contact

Maintainer: Peng Yu  
GitHub: [@pengyu0o0](https://github.com/pengyu0o0)