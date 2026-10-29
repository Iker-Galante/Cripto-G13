package ar.edu.itba.steg;

/**
 * Inserción en los {@code n} bits menos significativos de cada byte del portador (LSB1, LSB4).
 */
public final class LsbN implements StegAlgorithm {

    private final int bits;
    private final int mask;

    public LsbN(int bits) {
        if (bits != 1 && bits != 2 && bits != 4) {
            throw new IllegalArgumentException("bits must divide 8: " + bits);
        }
        this.bits = bits;
        this.mask = (1 << bits) - 1;
    }

    @Override
    public int capacity(int carrierLength) {
        return (int) ((long) carrierLength * bits / 8);
    }

    @Override
    public void embed(byte[] carrier, byte[] data) {
        if (data.length > capacity(carrier.length)) {
            throw new IllegalArgumentException("data does not fit in carrier");
        }
        int position = 0;
        for (byte value : data) {
            for (int shift = 8 - bits; shift >= 0; shift -= bits) {
                int chunk = (value >> shift) & mask;
                carrier[position] = (byte) ((carrier[position] & ~mask) | chunk);
                position++;
            }
        }
    }

    @Override
    public byte[] extract(byte[] carrier) {
        byte[] data = new byte[capacity(carrier.length)];
        int position = 0;
        for (int i = 0; i < data.length; i++) {
            int value = 0;
            for (int read = 0; read < 8; read += bits) {
                value = (value << bits) | (carrier[position] & mask);
                position++;
            }
            data[i] = (byte) value;
        }
        return data;
    }
}
