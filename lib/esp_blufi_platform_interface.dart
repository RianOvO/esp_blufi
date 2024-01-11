import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'esp_blufi_method_channel.dart';

abstract class EspBlufiPlatform extends PlatformInterface {
  /// Constructs a EspBlufiPlatform.
  EspBlufiPlatform() : super(token: _token);

  static final Object _token = Object();

  static EspBlufiPlatform _instance = MethodChannelEspBlufi.instance;

  /// The default instance of [EspBlufiPlatform] to use.
  ///
  /// Defaults to [MethodChannelEspBlufi].
  static EspBlufiPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [EspBlufiPlatform] when
  /// they register themselves.
  static set instance(EspBlufiPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<void> testFunction() {
    throw UnimplementedError('testFunction() has not been implemented.');
  }


  Future<void> scanDeviceInfo({String? filterString}) {
    throw UnimplementedError('scanDeviceInfo() has not been implemented.');
  }

  Future<void> stopScan() {
    throw UnimplementedError('stopScan() has not been implemented.');
  }

  Future<void> connectPeripheral({String? peripheralAddress}) {
    throw UnimplementedError('connectPeripheral() has not been implemented');
  }

  Future requestCloseConnection() async {
    throw UnimplementedError('requestCloseConnection() has not been implemented');
  }

  Future<void> configProvision({String? username, String? password}) async {
    throw UnimplementedError('configProvision() has not been implemented');
  }

  void onMessageReceived({ResultCallback? successCallback, ResultCallback? errorCallback}) {
    throw UnimplementedError('onMessageReceived() has not been implemented');
  }

  Future<void> getAllPairedDevice() async {
    throw UnimplementedError('getAllPairedDevice');
  }

  Future<void> requestDeviceStatus() async {
    throw UnimplementedError('requestDeviceStatus');
  }

  Future<void> sendCustomData({String? data}) async {
    throw UnimplementedError('sendCustomData');
  }
}
