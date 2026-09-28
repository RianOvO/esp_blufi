# esp_blufi

[English](README.md) | 中文

基于 [BLUFI](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/api-guides/ble/blufi.html)
协议为乐鑫（Espressif）设备配置 Wi-Fi 的 Flutter 插件，支持 Android 和 iOS。App 通过低功耗蓝牙（BLE）
扫描并连接设备，然后把要连接的 Wi-Fi 名称（SSID）和密码发给设备。

## 环境要求

| 平台 | 最低版本 | 说明 |
| --- | --- | --- |
| Android | API 21（Android 5.0） | 使用 compileSdk 36（Android 16）、AGP 9.1、Java 17 构建 |
| iOS | iOS 13 | 支持 Swift Package Manager 和 CocoaPods。不含预编译库，真机和 arm64 模拟器都能编译 |
| Flutter | 3.27 | 开发时使用 Flutter 3.47 |

iOS 模拟器和 Android 模拟器都没有蓝牙，配网必须在真机上测试。

## 安装

```yaml
dependencies:
  esp_blufi: ^0.2.0
```

## 平台配置

### Android

插件已在自己的 manifest 里声明了所需的蓝牙权限，App 的 manifest 不需要额外添加。

* **Android 12 及以上：** 第一次调用 `scanDeviceInfo()` 时，会在运行时申请 `BLUETOOTH_SCAN` 和
  `BLUETOOTH_CONNECT`。`BLUETOOTH_SCAN` 带有 `neverForLocation` 声明，因此不需要定位权限。
  如果你的 App 需要通过 BLE 扫描获取位置，请在 App 的 manifest 里去掉这个声明：

  ```xml
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
      tools:remove="android:usesPermissionFlags" />
  ```

* **Android 11 及以下：** 运行时会申请 `ACCESS_FINE_LOCATION`，而且必须打开系统定位服务，
  否则 BLE 扫描不到设备。

如果用户拒绝授权，插件会发送 `permission_denied` 消息，不会开始扫描。

### iOS

在 `ios/Runner/Info.plist` 里添加蓝牙用途说明。缺少这项时，App 第一次访问蓝牙就会被 iOS 强制退出：

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>使用蓝牙查找附近的设备并发送 Wi-Fi 配置。</string>
```

蓝牙权限弹窗会在第一次调用 `scanDeviceInfo()` 时出现，而不是 App 一启动就弹。插件自带隐私清单
（`PrivacyInfo.xcprivacy`）。

插件同时支持 Swift Package Manager 和 CocoaPods。开启 Swift Package Manager
（`flutter config --enable-swift-package-manager`）后，Flutter 使用 `ios/esp_blufi/Package.swift`；
否则使用 podspec。

## 使用方法

方法在请求发出后就返回，结果以 JSON 字符串的形式异步发到 `onMessageReceived` 回调里，所以要先注册回调。

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
        // 已连接并就绪：先协商加密，再发送密码
        blufi.negotiateSecurity();
        break;
      case 'negotiate_security':
        // "1"：之后发送的所有数据都会加密。"0"：协商失败，
        // 不要以明文发送密码，应断开连接后重试
        break;
      case 'wifi_info':
        // value: {"ssid": "...", "rssi": "-60", "address": "..."}
        break;
      case 'configure_params':
        // "1"：配置已发送，"0"：发送失败
        break;
      case 'device_wifi_connect':
        // "1"：设备已连上 Wi-Fi，"0"：未连上
        break;
    }
  });
}

Future<void> provision(String address, String ssid, String password) async {
  await blufi.scanDeviceInfo(filterString: 'BLUFI'); // 只显示名称包含 BLUFI 的设备
  // ……从 ble_scan_result 消息里选择设备……
  await blufi.stopScan();
  await blufi.connectPeripheral(peripheralAddress: address);
  // ……等待 GATT_SUCCESS，调用 negotiateSecurity()，等待 negotiate_security 为 "1"……
  await blufi.configProvision(username: ssid, password: password);
  await blufi.requestDeviceStatus();
  await blufi.requestCloseConnection();
}
```

## API

| 方法 | 说明 |
| --- | --- |
| `onMessageReceived({successCallback, errorCallback})` | 注册回调，接收下文列出的所有消息。 |
| `scanDeviceInfo({String? filterString})` | 开始 BLE 扫描，需要时会先申请权限。只上报名称包含 `filterString` 的设备（不区分大小写）。不传过滤条件时，Android 上报所有有名称的设备，iOS 默认使用 `BLUFI`。 |
| `stopScan()` | 停止 BLE 扫描。 |
| `connectPeripheral({String? peripheralAddress})` | 连接 `ble_scan_result` 里的设备。Android 上地址是 MAC 地址，iOS 上是外设 UUID。 |
| `negotiateSecurity()` | 与设备协商加密密钥（DH 密钥交换）。`negotiate_security` 返回 `"1"` 后，之后发送的所有数据都会用 AES 加密并附带校验。请在 `GATT_SUCCESS` 之后、`configProvision()` 之前调用。 |
| `requestDeviceVersion()` | 查询设备的 BLUFI 版本，结果通过 `device_version` 返回。 |
| `requestDeviceWifiScan()` | 让设备扫描周围的 Wi-Fi，每个网络通过一条 `wifi_info` 消息返回。 |
| `configProvision({String? username, String? password})` | 以 Station 模式发送 Wi-Fi 名称（`username`）和密码。 |
| `requestDeviceStatus()` | 查询设备是否已连上 Wi-Fi。 |
| `sendCustomData({String? data})` | 向设备固件发送自定义数据。 |
| `requestCloseConnection()` | 断开 BLE 连接。 |
| `getAllPairedDevice()` | 仅 Android：以 `each_connected_device` 消息上报已配对的设备。 |
| `getPlatformVersion()` | 返回系统名称和版本。 |

不调用 `negotiateSecurity()` 时，BLUFI 数据（包括 Wi-Fi 密码）会通过蓝牙明文传输。

## 消息

每条消息都是一个包含 `key` 和 `value` 的 JSON 字符串，大多数消息还带有已连接设备的 `address`。

| `key` | `value` | 平台 |
| --- | --- | --- |
| `ble_scan_result` | `{"address", "name", "rssi"}` | 两端 |
| `stop_scan_ble` | `"1"` | 两端 |
| `permission_denied` | `"1"` | Android |
| `peripheral_connect` | `"1"` 已连接，`"0"` 连接失败或已断开 | 两端 |
| `peripheral_disconnect` | `"1"` | 两端 |
| `GATT_SUCCESS` | `"1"`：服务发现完成，可以发送请求 | 两端 |
| `blufi_connect_prepared` | `"1"` 就绪；`"2"` / `"3"` / `"4"` 分别表示缺少服务 / 写特征 / 通知特征 | iOS |
| `discover_service` | `"1"` 找到，`"0"` 缺少 BLUFI 服务 | Android |
| `wifi_info` | `{"ssid", "rssi", "address"}`，Wi-Fi 扫描失败时为 `"0"` | 两端 |
| `configure_params` | `"1"` 已发送，`"0"` 失败 | 两端 |
| `device_status` | `"1"` 已收到（iOS），`"0"` 请求失败 | 两端 |
| `device_wifi_connect` | `"1"` 设备已连上 Wi-Fi，`"0"` 未连上 | 两端 |
| `negotiate_security` | `"1"` 已启用加密，`"0"` 失败或未连接 | 两端 |
| `device_version` | 设备的 BLUFI 版本，失败或未连接时为 `"0"` | 两端 |
| `post_custom_data` | `"1"` 已发送，`"0"` 失败 | 两端 |
| `receive_device_custom_data` | 设备发来的自定义数据，失败时为 `"0"` | 两端 |
| `receive_error_code` | BLUFI 错误码 | Android |
| `gatt_write_timeout` | 写入超时，连接已关闭 | Android |
| `gatt_disconnected` | `"1"` | Android |

Android 还会发送一些调试消息（例如 `request_mtu`、`onCharacteristicWrite`），不需要处理的 `key` 忽略即可。

## 从 0.1.x 升级

* iOS：最低版本改为 iOS 13，并去掉了内置的 OpenSSL 库。请重新执行 `pod install`，
  或让 Flutter 把工程迁移到 Swift Package Manager。
* Android：调用 `scanDeviceInfo()` 前，App 不再需要自己申请蓝牙或定位权限。
* 所有方法的 `Future` 都会完成，`await` 不会再一直卡住。
* 新增 `negotiateSecurity()` 和 `requestDeviceVersion()`。请在 `configProvision()` 之前调用
  `negotiateSecurity()`，避免以明文发送 Wi-Fi 密码。
* Android 的所有消息现在都是 JSON，去掉了协商时发出的纯文本消息。

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。
