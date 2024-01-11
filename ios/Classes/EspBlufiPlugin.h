#if TARGET_OS_OSX
#import <FlutterMacOS/FlutterMacOS.h>
#else
#import <Flutter/Flutter.h>
#endif
#import <CoreBluetooth/CoreBluetooth.h>

@interface EspBlufiPlugin : NSObject<FlutterPlugin>
@end

@interface EspBlufiPluginStreamHandler : NSObject<FlutterStreamHandler>
@property FlutterEventSink sink;
@end
