import 'esp_blufi_platform_interface.dart';

typedef ResultCallback = void Function(String? data);

class EspBlufi {
  static EspBlufiPlatform get instance => EspBlufiPlatform.instance;

  Future<String?> getPlatformVersion() {
    return EspBlufi.instance.getPlatformVersion();
  }

  Future<void> testFunction() {
    return EspBlufi.instance.testFunction();
  }

  Future<void> scanDeviceInfo({String? filterString}) {
    return EspBlufi.instance.scanDeviceInfo(filterString: filterString);
  }

  Future stopScan() async {
    return EspBlufi.instance.stopScan();
  }

  Future connectPeripheral({String? peripheralAddress}) async {
    return EspBlufi.instance
        .connectPeripheral(peripheralAddress: peripheralAddress);
  }

  Future requestCloseConnection() async {
    return EspBlufi.instance.requestCloseConnection();
  }

  Future configProvision({String? username, String? password}) async {
    return EspBlufi.instance
        .configProvision(username: username, password: password);
  }

  void onMessageReceived(
      {ResultCallback? successCallback, ResultCallback? errorCallback}) {
    EspBlufi.instance.onMessageReceived(
        successCallback: successCallback, errorCallback: errorCallback);
  }

  Future getAllPairedDevice() async {
    return EspBlufi.instance.getAllPairedDevice();
  }

  Future requestDeviceStatus() async {
    return EspBlufi.instance.requestDeviceStatus();
  }

  Future sendCustomData({String? data}) async {
    return EspBlufi.instance.sendCustomData(data: data);
  }
}
