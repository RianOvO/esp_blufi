import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:esp_blufi/esp_blufi_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelEspBlufi platform = MethodChannelEspBlufi.instance;
  const MethodChannel channel = MethodChannel('esp_blufi');
  final List<MethodCall> calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        calls.add(methodCall);
        return methodCall.method == 'getPlatformVersion' ? '42' : null;
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });

  test('negotiateSecurity invokes the native method', () async {
    await platform.negotiateSecurity();
    expect(calls.single.method, 'negotiateSecurity');
    expect(calls.single.arguments, isNull);
  });

  test('requestDeviceVersion invokes the native method', () async {
    await platform.requestDeviceVersion();
    expect(calls.single.method, 'requestDeviceVersion');
  });
}
