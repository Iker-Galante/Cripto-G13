package ar.edu.itba.crypto;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// Encrypts and decrypts the hidden block with a key and IV derived from the password.
public final class PayloadCipher {

    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTES = 8;  // eight zero bytes (not new byte[]{0})

    private final CipherSpec spec;
    private final SecretKeySpec key;
    private final byte[] iv;

    public PayloadCipher(CipherSpec spec) {
        this.spec = spec;
        CipherAlgorithm algorithm = spec.algorithm();
        byte[] keyAndIv = deriveKeyAndIv(spec);
        this.key = new SecretKeySpec(Arrays.copyOf(keyAndIv, algorithm.keyBytes()), algorithm.jceName());
        this.iv = Arrays.copyOfRange(keyAndIv, algorithm.keyBytes(), algorithm.keyBytes() + algorithm.ivBytes());
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            return run(Cipher.ENCRYPT_MODE, plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] ciphertext) {
        try {
            return run(Cipher.DECRYPT_MODE, ciphertext);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Could not decrypt: wrong password, algorithm or mode", e);
        }
    }

    // One PBKDF2-HMAC-SHA256 output of key + IV bytes, key first. Sizing it per algorithm matters:
    // a fixed 256 bits would leave no IV for aes192, aes256 or 3des.
    static byte[] deriveKeyAndIv(CipherSpec spec) {
        CipherAlgorithm algorithm = spec.algorithm();
        int bits = (algorithm.keyBytes() + algorithm.ivBytes()) * 8;
        PBEKeySpec keySpec = new PBEKeySpec(spec.password().toCharArray(), new byte[SALT_BYTES], ITERATIONS, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is not available", e);
        } finally {
            keySpec.clearPassword();
        }
    }

    private byte[] run(int opmode, byte[] input) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(spec.algorithm().jceName() + "/" + spec.mode().transformation());
        if (spec.mode().usesIv()) {
            cipher.init(opmode, key, new IvParameterSpec(iv));
        } else {
            cipher.init(opmode, key);  // ECB takes no IV; passing one throws
        }
        return cipher.doFinal(input);
    }
}
