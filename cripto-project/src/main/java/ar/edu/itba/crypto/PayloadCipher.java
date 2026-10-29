package ar.edu.itba.crypto;

import ar.edu.itba.StegoException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Encripta y desencripta con una clave e IV derivados de una password con PBKDF2 (HMAC-SHA256,
 * 10000 iteraciones, salt de 8 bytes en cero). Se derivan {@code keyLength + ivLength} bytes:
 * los primeros son la clave y los siguientes el IV.
 */
public final class PayloadCipher {

    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";
    private static final byte[] SALT = new byte[8];
    private static final int ITERATIONS = 10_000;

    private final CipherAlgorithm algorithm;
    private final CipherMode mode;
    private final byte[] key;
    private final byte[] iv;

    public PayloadCipher(CipherAlgorithm algorithm, CipherMode mode, String password) {
        this.algorithm = algorithm;
        this.mode = mode;
        byte[] material = deriveKeyMaterial(password, algorithm.keyLength() + algorithm.ivLength());
        this.key = Arrays.copyOfRange(material, 0, algorithm.keyLength());
        this.iv = Arrays.copyOfRange(material, algorithm.keyLength(), material.length);
    }

    public CipherAlgorithm algorithm() {
        return algorithm;
    }

    public CipherMode mode() {
        return mode;
    }

    byte[] key() {
        return key.clone();
    }

    byte[] iv() {
        return iv.clone();
    }

    public byte[] encrypt(byte[] plainText) {
        try {
            return cipher(Cipher.ENCRYPT_MODE).doFinal(plainText);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] cipherText) {
        try {
            return cipher(Cipher.DECRYPT_MODE).doFinal(cipherText);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new StegoException("no se pudo desencriptar: la password, el algoritmo o el modo son incorrectos"
                    + " (o la información oculta no está encriptada)", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("decryption failed", e);
        }
    }

    private Cipher cipher(int operation) throws GeneralSecurityException {
        String transformation = algorithm.jceName() + "/" + mode.jceName() + "/" + mode.padding();
        Cipher cipher = Cipher.getInstance(transformation);
        SecretKeySpec keySpec = new SecretKeySpec(key, algorithm.jceName());
        if (mode.usesIv()) {
            cipher.init(operation, keySpec, new IvParameterSpec(iv));
        } else {
            cipher.init(operation, keySpec);
        }
        return cipher;
    }

    private static byte[] deriveKeyMaterial(String password, int length) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, length * 8);
        try {
            return SecretKeyFactory.getInstance(KEY_DERIVATION).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("key derivation failed", e);
        } finally {
            spec.clearPassword();
        }
    }
}
