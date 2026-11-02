package ar.edu.itba.crypto;

// The -a values, with the JCE algorithm name and the key and IV sizes in bytes.
public enum CipherAlgorithm {
    AES128("aes128", "AES", 16, 16),
    AES192("aes192", "AES", 24, 16),
    AES256("aes256", "AES", 32, 16),
    TRIPLE_DES("3des", "DESede", 24, 8);  // three-key 3DES

    private final String option;
    private final String jceName;
    private final int keyBytes;
    private final int ivBytes;

    CipherAlgorithm(String option, String jceName, int keyBytes, int ivBytes) {
        this.option = option;
        this.jceName = jceName;
        this.keyBytes = keyBytes;
        this.ivBytes = ivBytes;
    }

    // Not valueOf: "3des" cannot be a constant name, and the options are lowercase.
    public static CipherAlgorithm fromOption(String option) {
        for (CipherAlgorithm algorithm : values()) {
            if (algorithm.option.equals(option)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("Invalid encryption algorithm: " + option);
    }

    public String jceName() {
        return jceName;
    }

    public int keyBytes() {
        return keyBytes;
    }

    public int ivBytes() {
        return ivBytes;
    }
}
