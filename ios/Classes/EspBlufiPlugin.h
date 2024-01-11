#if TARGET_OS_OSX
#import <FlutterMacOS/FlutterMacOS.h>
#else
#import <Flutter/Flutter.h>
#endif
#import <CoreBluetooth/CoreBluetooth.h>

@interface FlutterBlufiPlugin : NSObject<FlutterPlugin>
@end

@interface FlutterBlufiPluginStreamHandler : NSObject<FlutterStreamHandler>
@property FlutterEventSink sink;
@end
