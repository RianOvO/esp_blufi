import 'package:esp_blufi/esp_blufi_method_channel.dart';
import 'package:esp_blufi/esp_blufi_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class MockEspBlufiPlatform
    with MockPlatformInterfaceMixin
    implements EspBlufiPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> configProvision({String? username, String? password}) {
    throw UnimplementedError();
  }

  @override
  Future<void> connectPeripheral({String? peripheralAddress}) {
    throw UnimplementedError();
  }

  @override
  Future<void> getAllPairedDevice() {
    throw UnimplementedError();
  }

  // @override
  // void onMessageReceived({ResultCallback? successCallback, ResultCallback? errorCallback}) {
  // }

  @override
  Future requestCloseConnection() {
    throw UnimplementedError();
  }

  @override
  Future<void> requestDeviceStatus() {
    throw UnimplementedError();
  }

  @override
  Future<void> scanDeviceInfo({String? filterString}) {
    throw UnimplementedError();
  }

  @override
  Future<void> sendCustomData({String? data}) {
    throw UnimplementedError();
  }

  @override
  Future<void> stopScan() {
    throw UnimplementedError();
  }

  @override
  Future<void> testFunction() {
    throw UnimplementedError();
  }
}

void main() {
  final EspBlufiPlatform initialPlatform = EspBlufiPlatform.instance;

  test('$MethodChannelEspBlufi is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelEspBlufi>());
  });

  // test('getPlatformVersion', () async {
  //   EspBlufi espBlufiPlugin = EspBlufi();
  //   MockEspBlufiPlatform fakePlatform = MockEspBlufiPlatform();
  //   EspBlufiPlatform.instance = fakePlatform;
  //
  //   expect(await espBlufiPlugin.getPlatformVersion(), '42');
  // });
}
