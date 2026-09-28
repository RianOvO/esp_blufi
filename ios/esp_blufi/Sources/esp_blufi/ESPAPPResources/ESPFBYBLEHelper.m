//
//  ESPFBYBLEHelper.m
//  EspBlufi
//
//  Created by fanbaoying on 2020/6/11.
//  Copyright © 2020 espressif. All rights reserved.
//

#import "ESPFBYBLEHelper.h"
#import <CoreBluetooth/CoreBluetooth.h>

@interface ESPFBYBLEHelper ()<CBCentralManagerDelegate,CBPeripheralDelegate>

@property (nonatomic, strong) CBCentralManager *centralManager;

@property (nonatomic, strong) NSMutableArray *peripherals;

@property (nonatomic, assign) CBManagerState peripheralState;

// Only scan when the Dart side asked for it, even if Bluetooth is turned on later.
@property (nonatomic, assign) BOOL scanRequested;

@end

@implementation ESPFBYBLEHelper

- (void)ESPFBYBLEHelperInit {
    self.centralManager = [[CBCentralManager alloc]initWithDelegate:self queue:nil];
}

//单例模式
+ (instancetype)share {
    static ESPFBYBLEHelper *share = nil;
    static dispatch_once_t oneToken;
    dispatch_once(&oneToken, ^{
        share = [[ESPFBYBLEHelper alloc]init];
        [share ESPFBYBLEHelperInit];
    });
    return share;
}

- (void)stopScan {
    self.scanRequested = NO;
    [self.centralManager stopScan];
}

- (void)startScan:(FBYBleDeviceBackBlock)device {
    _bleScanSuccessBlock = device;
    self.scanRequested = YES;
    if (self.peripheralState == CBManagerStatePoweredOn) {
        [self.centralManager scanForPeripheralsWithServices:nil options:nil];
    }
}

/**
 扫描到设备
 
 @param central 中心管理者
 @param peripheral 扫描到的设备
 @param advertisementData 广告信息
 @param RSSI 信号强度
 */
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI {
    ESPPeripheral *espPeripheral = [[ESPPeripheral alloc] initWithPeripheral:peripheral];
    espPeripheral.name = advertisementData[CBAdvertisementDataLocalNameKey] ?: peripheral.name;
    espPeripheral.rssi = RSSI.intValue;
    if (self.bleScanSuccessBlock) {
        self.bleScanSuccessBlock(espPeripheral);
    }
}

// 状态更新时调用
- (void)centralManagerDidUpdateState:(CBCentralManager *)central
{
    self.peripheralState = central.state;
    if (central.state == CBManagerStatePoweredOn && self.scanRequested) {
        [self.centralManager scanForPeripheralsWithServices:nil options:nil];
    }
}

@end
