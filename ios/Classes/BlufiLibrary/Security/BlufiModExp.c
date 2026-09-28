//
//  BlufiModExp.c
//  EspBlufi
//

#include "BlufiModExp.h"

#include <string.h>

#define LIMBS (BLUFI_DH_KEY_BYTES / 4)

static void bytes_to_limbs(const uint8_t *in, uint32_t *out) {
    for (int i = 0; i < LIMBS; i++) {
        const uint8_t *p = in + BLUFI_DH_KEY_BYTES - 4 * (i + 1);
        out[i] = ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) | ((uint32_t)p[2] << 8) | (uint32_t)p[3];
    }
}

static void limbs_to_bytes(const uint32_t *in, uint8_t *out) {
    for (int i = 0; i < LIMBS; i++) {
        uint8_t *p = out + BLUFI_DH_KEY_BYTES - 4 * (i + 1);
        p[0] = (uint8_t)(in[i] >> 24);
        p[1] = (uint8_t)(in[i] >> 16);
        p[2] = (uint8_t)(in[i] >> 8);
        p[3] = (uint8_t)in[i];
    }
}

// r = a - b, returns the borrow.
static uint32_t sub_limbs(uint32_t *r, const uint32_t *a, const uint32_t *b) {
    uint64_t borrow = 0;
    for (int i = 0; i < LIMBS; i++) {
        uint64_t d = (uint64_t)a[i] - b[i] - borrow;
        r[i] = (uint32_t)d;
        borrow = (d >> 32) & 1;
    }
    return (uint32_t)borrow;
}

// Copies src into dst when mask is all ones, leaves dst untouched when mask is zero.
static void select_limbs(uint32_t *dst, const uint32_t *src, uint32_t mask) {
    for (int i = 0; i < LIMBS; i++) {
        dst[i] = (dst[i] & ~mask) | (src[i] & mask);
    }
}

// r = a * b * R^-1 mod m, with R = 2^1024 (CIOS Montgomery multiplication).
static void mont_mul(uint32_t *r, const uint32_t *a, const uint32_t *b, const uint32_t *m, uint32_t n0) {
    uint32_t t[LIMBS + 2];
    memset(t, 0, sizeof(t));
    for (int i = 0; i < LIMBS; i++) {
        uint64_t c = 0;
        for (int j = 0; j < LIMBS; j++) {
            c += (uint64_t)t[j] + (uint64_t)a[j] * b[i];
            t[j] = (uint32_t)c;
            c >>= 32;
        }
        c += t[LIMBS];
        t[LIMBS] = (uint32_t)c;
        t[LIMBS + 1] = (uint32_t)(c >> 32);

        uint32_t u = t[0] * n0;
        c = ((uint64_t)t[0] + (uint64_t)u * m[0]) >> 32;
        for (int j = 1; j < LIMBS; j++) {
            c += (uint64_t)t[j] + (uint64_t)u * m[j];
            t[j - 1] = (uint32_t)c;
            c >>= 32;
        }
        c += t[LIMBS];
        t[LIMBS - 1] = (uint32_t)c;
        t[LIMBS] = t[LIMBS + 1] + (uint32_t)(c >> 32);
        t[LIMBS + 1] = 0;
    }

    uint32_t s[LIMBS];
    uint32_t borrow = sub_limbs(s, t, m);
    // Use t - m when t >= m, i.e. there is an overflow limb or no borrow.
    uint32_t mask = (uint32_t)0 - ((t[LIMBS] != 0) | (borrow == 0));
    memcpy(r, t, sizeof(uint32_t) * LIMBS);
    select_limbs(r, s, mask);
}

int blufi_modexp_1024(const uint8_t base[BLUFI_DH_KEY_BYTES],
                      const uint8_t exp[BLUFI_DH_KEY_BYTES],
                      const uint8_t mod[BLUFI_DH_KEY_BYTES],
                      uint8_t out[BLUFI_DH_KEY_BYTES]) {
    uint32_t m[LIMBS], b[LIMBS], rr[LIMBS], acc[LIMBS], tmp[LIMBS], one[LIMBS];
    bytes_to_limbs(mod, m);
    if ((m[0] & 1) == 0 || m[LIMBS - 1] == 0) {
        return -1;
    }
    bytes_to_limbs(base, b);
    uint32_t scratch[LIMBS];
    if (sub_limbs(scratch, b, m) == 0) {
        return -1; // base >= mod
    }

    // n0 = -m^-1 mod 2^32 (Newton iteration).
    uint32_t inv = 1;
    for (int i = 0; i < 5; i++) {
        inv *= 2 - m[0] * inv;
    }
    uint32_t n0 = (uint32_t)0 - inv;

    // rr = R^2 mod m, computed by doubling 1 modulo m 2048 times.
    memset(rr, 0, sizeof(rr));
    rr[0] = 1;
    for (int i = 0; i < 2 * 32 * LIMBS; i++) {
        uint32_t carry = rr[LIMBS - 1] >> 31;
        for (int j = LIMBS - 1; j > 0; j--) {
            rr[j] = (rr[j] << 1) | (rr[j - 1] >> 31);
        }
        rr[0] <<= 1;
        uint32_t borrow = sub_limbs(tmp, rr, m);
        select_limbs(rr, tmp, (uint32_t)0 - (carry | (borrow == 0)));
    }

    memset(one, 0, sizeof(one));
    one[0] = 1;

    uint32_t bm[LIMBS];
    mont_mul(bm, b, rr, m, n0);   // base in Montgomery form
    mont_mul(acc, one, rr, m, n0); // 1 in Montgomery form

    // Left-to-right square-and-always-multiply.
    for (int i = 0; i < BLUFI_DH_KEY_BYTES; i++) {
        for (int bit = 7; bit >= 0; bit--) {
            mont_mul(acc, acc, acc, m, n0);
            mont_mul(tmp, acc, bm, m, n0);
            uint32_t mask = (uint32_t)0 - (uint32_t)((exp[i] >> bit) & 1);
            select_limbs(acc, tmp, mask);
        }
    }

    mont_mul(acc, acc, one, m, n0);
    limbs_to_bytes(acc, out);

    memset(acc, 0, sizeof(acc));
    memset(tmp, 0, sizeof(tmp));
    return 0;
}

int blufi_dh_check_public_key(const uint8_t *key, size_t keyLength,
                              const uint8_t mod[BLUFI_DH_KEY_BYTES]) {
    while (keyLength > 0 && key[0] == 0) {
        key++;
        keyLength--;
    }
    if (keyLength > BLUFI_DH_KEY_BYTES) {
        return 0;
    }
    uint8_t padded[BLUFI_DH_KEY_BYTES];
    memset(padded, 0, sizeof(padded));
    memcpy(padded + BLUFI_DH_KEY_BYTES - keyLength, key, keyLength);

    uint32_t k[LIMBS], m[LIMBS], tmp[LIMBS];
    bytes_to_limbs(padded, k);
    bytes_to_limbs(mod, m);

    // key > 1
    int greaterThanOne = k[0] > 1;
    for (int i = 1; i < LIMBS; i++) {
        greaterThanOne |= k[i] != 0;
    }
    // key < mod - 1  <=>  key + 1 < mod
    uint64_t c = 1;
    for (int i = 0; i < LIMBS; i++) {
        c += k[i];
        k[i] = (uint32_t)c;
        c >>= 32;
    }
    int lessThanModMinusOne = c == 0 && sub_limbs(tmp, k, m) == 1;
    return greaterThanOne && lessThanModMinusOne;
}
