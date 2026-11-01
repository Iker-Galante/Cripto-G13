package ar.edu.itba.steg;

// The -steg values. valueOf is case-sensitive.
public enum StegMethod {
    LSB1, LSB4, LSBI;

    public Steganographer create() {
        return switch (this) {
            case LSB1 -> new LsbSteganographer(1);
            case LSB4 -> new LsbSteganographer(4);
            case LSBI -> new LsbiSteganographer();
        };
    }
}
