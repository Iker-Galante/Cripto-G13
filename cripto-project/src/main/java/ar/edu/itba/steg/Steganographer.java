package ar.edu.itba.steg;

// Hides raw bytes in pixel bytes. It never writes or reads a length: PayloadCodec owns that.
public interface Steganographer {

    // How many payload bytes fit in these pixels.
    int capacity(byte[] pixels);

    // Writes the payload bits into pixels, in place.
    void embed(byte[] pixels, byte[] payload);

    // Returns capacity(pixels) bytes read back, hidden payload first and leftover bits after it.
    byte[] extract(byte[] pixels);
}
