package ar.edu.itba.steg;

// One payload bit in the least significant bit of each pixel byte, most significant bit first.
public final class Lsb1Steganographer implements Steganographer {

    @Override
    public int capacity(byte[] pixels) {
        return pixels.length / 8;
    }

    @Override
    public void embed(byte[] pixels, byte[] payload) {
        int capacity = capacity(pixels);
        if (payload.length > capacity) {
            throw new IllegalArgumentException("File does not fit in the carrier: needs "
                    + payload.length + " bytes, maximum capacity with LSB1 is " + capacity + " bytes");
        }

        int i = 0;
        for (byte b : payload) {
            for (int bit = 7; bit >= 0; bit--) {
                pixels[i] = (byte) ((pixels[i] & 0xFE) | ((b >> bit) & 1));
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
            for (int bit = 0; bit < 8; bit++) {
                value = (value << 1) | (pixels[i] & 1);
                i++;
            }
            hidden[n] = (byte) value;
        }
        return hidden;
    }
}
