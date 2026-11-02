package ar.edu.itba.crypto;

// The -m values and the JCE mode and padding for each.
public enum CipherMode {
    ECB("ecb", "ECB/PKCS5Padding", false),
    CBC("cbc", "CBC/PKCS5Padding", true),
    CFB("cfb", "CFB8/NoPadding", true),
    OFB("ofb", "OFB/NoPadding", true);  // full-block feedback: 128 bits for AES, 64 for 3DES

    private final String option;
    private final String transformation;
    private final boolean usesIv;

    CipherMode(String option, String transformation, boolean usesIv) {
        this.option = option;
        this.transformation = transformation;
        this.usesIv = usesIv;
    }

    public static CipherMode fromOption(String option) {
        for (CipherMode mode : values()) {
            if (mode.option.equals(option)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid encryption mode: " + option);
    }

    public String transformation() {
        return transformation;
    }

    public boolean usesIv() {
        return usesIv;
    }
}
