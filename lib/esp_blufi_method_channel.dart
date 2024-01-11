import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'esp_blufi_platform_interface.dart';

typedef ResultCallback = void Function(String? data);


/// An implementation of [EspBlufiPlatform] that uses method channels.
class MethodChannelEspBlufi extends EspBlufiPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('esp_blufi');
  final EventChannel _eventChannel = const EventChannel('esp_blufi/state');

  ResultCallback? _resultSuccessCallback;
  ResultCallback? _resultErrorCallback;

  static  final MethodChannelEspBlufi _instance = MethodChannelEspBlufi._();
  static MethodChannelEspBlufi get instance => _instance;

  MethodChannelEspBlufi._() {
    methodChannel.setMethodCallHandler(null);

    _eventChannel.receiveBroadcastStream().listen(speechResultsHandler, onError: speechResultErrorHandler);
  }

  @override
  void onMessageReceived({ResultCallback? successCallback, ResultCallback? errorCallback}) {
    _resultSuccessCallback = successCallback;
    _resultErrorCallback = errorCallback;
  }

  @override
  Future<void> testFunction() async {
    await methodChannel.invokeMethod<String>('testFunction');
  }

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<bool?> scanDeviceInfo({String? filterString}) async {
    final bool? isEnable =
    await methodChannel.invokeMethod('scanDeviceInfo', <String, dynamic>{'filter': filterString});
    return isEnable;
  }

  @override
  Future stopScan() async {
    await methodChannel.invokeMethod('stopScan');
  }

  @override
  Future connectPeripheral({String? peripheralAddress}) async {
    await methodChannel.invokeMethod('connectPeripheral', <String, dynamic>{'peripheral': peripheralAddress});
  }

  @override
  Future requestCloseConnection() async {
    await methodChannel.invokeMethod('requestCloseConnection');
  }

  @override
  Future<void> configProvision({String? username, String? password}) async {
    await methodChannel.invokeMethod('configProvision', <String, dynamic>{'username': username, 'password': password});
  }

  @override
  Future<void> getAllPairedDevice() async {
    await methodChannel.invokeMethod('getAllPairedDevice');
  }

  @override
  Future<void> requestDeviceStatus() async {
    await methodChannel.invokeMethod('requestDeviceStatus');
  }

  @override
  Future<void> sendCustomData({String? data}) async {
    await methodChannel.invokeMethod('sendCustomData', <String, dynamic>{'data': data});
  }

  speechResultsHandler(dynamic event) {
    if (_resultSuccessCallback != null) _resultSuccessCallback!(event);
  }

  speechResultErrorHandler(dynamic error) {
    if (_resultErrorCallback != null) _resultErrorCallback!(error);
  }
}
