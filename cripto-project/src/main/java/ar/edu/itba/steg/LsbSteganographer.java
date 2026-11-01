package ar.edu.itba.steg;

// Hides the payload in the lowest bitsPerByte bits of each pixel byte, most significant bits first.
// LSB1 uses 1 bit per pixel byte, LSB4 uses 4.
public final class LsbSteganographer implements Steganographer {

    private final int bitsPerByte;
    private final int mask;           // the low bits we overwrite, e.g. 0x0F for LSB4
    private final int chunksPerByte;  // pixel bytes needed for one payload byte

    public LsbSteganographer(int bitsPerByte) {
        if (bitsPerByte != 1 && bitsPerByte != 4) {
            throw new IllegalArgumentException("Bits per byte must be 1 or 4, got " + bitsPerByte);
        }
        this.bitsPerByte = bitsPerByte;
        this.mask = (1 << bitsPerByte) - 1;
        this.chunksPerByte = 8 / bitsPerByte;
    }

    @Override
    public int capacity(byte[] pixels) {
        return pixels.length / chunksPerByte;
    }

    @Override
    public void embed(byte[] pixels, byte[] payload) {
        int capacity = capacity(pixels);
        if (payload.length > capacity) {
            throw new IllegalArgumentException("File does not fit in the carrier: needs " + payload.length
                    + " bytes, maximum capacity with LSB" + bitsPerByte + " is " + capacity + " bytes");
        }

        int i = 0;
        for (byte b : payload) {
            for (int shift = 8 - bitsPerByte; shift >= 0; shift -= bitsPerByte) {
                pixels[i] = (byte) ((pixels[i] & ~mask) | ((b >> shift) & mask));
                i++;
            }
        }
    }

    @Override
    public byte[] extract(byte[] pixels) {
        byte[] hidden = new byte[capacity(pixels)];
        int i = 0;
        for (int n = 0; n < hidden.length; n++) {
            int value = 0;
            for (int chunk = 0; chunk < chunksPerByte; chunk++) {
                value = (value << bitsPerByte) | (pixels[i] & mask);
                i++;
            }
            hidden[n] = (byte) value;
        }
        return hidden;
    }
}
