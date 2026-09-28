//
//  BlufiModExp.h
//  EspBlufi
//
//  Minimal fixed-size (1024-bit) modular exponentiation used by the BLUFI
//  Diffie-Hellman key exchange. Replaces the previously vendored OpenSSL
//  static libraries, which had no arm64 simulator slice and could not be
//  linked by current Xcode versions.
//

#ifndef BlufiModExp_h
#define BlufiModExp_h

#include <stddef.h>
#include <stdint.h>

#define BLUFI_DH_KEY_BYTES 128

#ifdef __cplusplus
extern "C" {
#endif

/// out = base ^ exp mod mod. All values are 128-byte big-endian integers,
/// `mod` must be odd and base must be < mod. Returns 0 on success.
int blufi_modexp_1024(const uint8_t base[BLUFI_DH_KEY_BYTES],
                      const uint8_t exp[BLUFI_DH_KEY_BYTES],
                      const uint8_t mod[BLUFI_DH_KEY_BYTES],
                      uint8_t out[BLUFI_DH_KEY_BYTES]);

/// Returns 1 when 1 < key < mod - 1 (a valid DH public key), 0 otherwise.
/// `key` is a big-endian integer of `keyLength` bytes (at most 128 significant bytes).
int blufi_dh_check_public_key(const uint8_t *key, size_t keyLength,
                              const uint8_t mod[BLUFI_DH_KEY_BYTES]);

#ifdef __cplusplus
}
#endif

#endif /* BlufiModExp_h */
