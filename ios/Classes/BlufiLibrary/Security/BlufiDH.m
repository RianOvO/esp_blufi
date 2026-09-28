//
//  BlufiDH.m
//  EspBlufi
//
//  Created by AE on 2020/6/10.
//  Copyright © 2020 espressif. All rights reserved.
//

#import "BlufiDH.h"
#import "BlufiModExp.h"

@implementation BlufiDH {
    NSMutableData *_privateKeyStorage;
}

- (instancetype)initWithP:(NSData *)p G:(NSData *)g PublicKey:(NSData *)publicKey PrivateKey:(NSData *)privateKey {
    self = [super init];
    if (self) {
        _p = p;
        _g = g;
        _publicKey = publicKey;
        _privateKeyStorage = [privateKey mutableCopy];
    }
    return self;
}

- (NSData *)privateKey {
    return _privateKeyStorage;
}

- (NSData *)generateSecret:(NSData *)devicePublicKey {
    if (_privateKeyStorage.length != BLUFI_DH_KEY_BYTES || _p.length != BLUFI_DH_KEY_BYTES) {
        NSLog(@"BlufiDH: DH keys are not available");
        return nil;
    }
    if (!blufi_dh_check_public_key(devicePublicKey.bytes, devicePublicKey.length, _p.bytes)) {
        NSLog(@"BlufiDH: invalid device public key");
        return nil;
    }

    // Left-pad the device key to the modulus size (leading zeros are already stripped by the check).
    const uint8_t *keyBytes = devicePublicKey.bytes;
    NSUInteger keyLength = devicePublicKey.length;
    while (keyLength > BLUFI_DH_KEY_BYTES && keyBytes[0] == 0) {
        keyBytes++;
        keyLength--;
    }
    uint8_t base[BLUFI_DH_KEY_BYTES] = {0};
    memcpy(base + BLUFI_DH_KEY_BYTES - keyLength, keyBytes, keyLength);

    uint8_t secret[BLUFI_DH_KEY_BYTES];
    if (blufi_modexp_1024(base, _privateKeyStorage.bytes, _p.bytes, secret) != 0) {
        NSLog(@"BlufiDH: compute secret failed");
        return nil;
    }
    NSData *result = [NSData dataWithBytes:secret length:BLUFI_DH_KEY_BYTES];
    memset(secret, 0, sizeof(secret));
    return result;
}

- (void)releaseDH {
    if (_privateKeyStorage) {
        memset(_privateKeyStorage.mutableBytes, 0, _privateKeyStorage.length);
        _privateKeyStorage = nil;
    }
}

- (void)dealloc {
    [self releaseDH];
}

@end
