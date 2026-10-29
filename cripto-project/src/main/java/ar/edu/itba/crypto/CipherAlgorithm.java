package ar.edu.itba.crypto;

import java.util.Arrays;
import java.util.Optional;

/** Algoritmos de encripción disponibles por línea de comandos ({@code -a}). */
public enum CipherAlgorithm {
    AES128("aes128", "AES", 16, 16),
    AES192("aes192", "AES", 24, 16),
    AES256("aes256", "AES", 32, 16),
    /** Triple DES con tres claves distintas (des_ede3 en openssl). */
    DES3("3des", "DESede", 24, 8);

    private final String argument;
    private final String jceName;
    private final int keyLength;
    private final int ivLength;

    CipherAlgorithm(String argument, String jceName, int keyLength, int ivLength) {
        this.argument = argument;
        this.jceName = jceName;
        this.keyLength = keyLength;
        this.ivLength = ivLength;
    }

    public String argument() {
        return argument;
    }

    public String jceName() {
        return jceName;
    }

    public int keyLength() {
        return keyLength;
    }

    public int ivLength() {
        return ivLength;
    }

    public static Optional<CipherAlgorithm> fromName(String name) {
        return Arrays.stream(values()).filter(algorithm -> algorithm.argument.equalsIgnoreCase(name)).findFirst();
    }
}
