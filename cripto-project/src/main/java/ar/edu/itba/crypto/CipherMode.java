package ar.edu.itba.crypto;

import java.util.Arrays;
import java.util.Optional;

/** Modos de encripción disponibles por línea de comandos ({@code -m}). */
public enum CipherMode {
    ECB("ecb", "ECB", "PKCS5Padding"),
    CBC("cbc", "CBC", "PKCS5Padding"),
    /** 8 bits de feedback (cfb8 en openssl). */
    CFB("cfb", "CFB8", "NoPadding"),
    /** Feedback de bloque completo (128 bits en AES). */
    OFB("ofb", "OFB", "NoPadding");

    private final String argument;
    private final String jceName;
    private final String padding;

    CipherMode(String argument, String jceName, String padding) {
        this.argument = argument;
        this.jceName = jceName;
        this.padding = padding;
    }

    public String argument() {
        return argument;
    }

    public String jceName() {
        return jceName;
    }

    public String padding() {
        return padding;
    }

    public boolean usesIv() {
        return this != ECB;
    }

    public static Optional<CipherMode> fromName(String name) {
        return Arrays.stream(values()).filter(mode -> mode.argument.equalsIgnoreCase(name)).findFirst();
    }
}
