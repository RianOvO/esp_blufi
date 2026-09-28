//
//  BlufiSecurity.h
//  EspBlufi
//
//  Created by AE on 2020/6/9.
//  Copyright © 2020 espressif. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "BlufiDH.h"

NS_ASSUME_NONNULL_BEGIN

@interface BlufiSecurity : NSObject

+ (NSInteger)crc:(NSInteger)crc data:(NSData *)data;

+ (NSInteger)crc:(NSInteger)crc buf:(Byte *)buf length:(NSInteger)length;

+ (NSData *)md5:(NSData *)data;

+ (nullable NSData *)aesEncrypt:(NSData *)data key:(NSData *)key iv:(NSData *)iv;

+ (nullable NSData *)aesDecrypt:(NSData *)data key:(NSData *)key iv:(NSData *)iv;

+ (nullable BlufiDH *)dhGenerateKeys;

@end

NS_ASSUME_NONNULL_END
