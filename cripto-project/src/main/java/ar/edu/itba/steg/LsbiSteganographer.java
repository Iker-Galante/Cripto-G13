package ar.edu.itba.steg;

import java.util.stream.IntStream;

// LSBI: LSB1 over the blue and green bytes from index 4 on. Bytes are grouped by their pattern
// (bits 2 and 1), and a group's LSBs are inverted when that leaves fewer bytes changed.
// Bytes 0..3 hold one flag per pattern 00, 01, 10, 11 so extract knows which groups to invert back.
public final class LsbiSteganographer implements Steganographer {

    private static final int FLAG_BYTES = 4;
    private static final int PATTERNS = 4;

    private final Steganographer lsb1 = new LsbSteganographer(1);

    @Override
    public int capacity(byte[] pixels) {
        return usableIndices(pixels).length / 8;
    }

    @Override
    public void embed(byte[] pixels, byte[] payload) {
        int[] indices = usableIndices(pixels);
        int capacity = indices.length / 8;
        if (payload.length > capacity) {
            throw new IllegalArgumentException("File does not fit in the carrier: needs " + payload.length
                    + " bytes, maximum capacity with LSBI is " + capacity + " bytes");
        }

        byte[] cover = gather(pixels, indices);
        byte[] stego = cover.clone();
        lsb1.embed(stego, payload);
        int[] flags = invertGroups(cover, stego, payload.length * 8);
        scatter(pixels, indices, stego);
        for (int k = 0; k < FLAG_BYTES; k++) {
            pixels[k] = (byte) ((pixels[k] & 0xFE) | flags[k]);
        }
    }

    @Override
    public byte[] extract(byte[] pixels) {
        if (pixels.length < FLAG_BYTES) {
            return new byte[0];
        }
        int[] flags = new int[PATTERNS];
        for (int k = 0; k < FLAG_BYTES; k++) {
            flags[k] = pixels[k] & 1;
        }

        byte[] bytes = gather(pixels, usableIndices(pixels));
        for (int j = 0; j < bytes.length; j++) {
            if (flags[pattern(bytes[j])] == 1) {
                bytes[j] ^= 1;
            }
        }
        return lsb1.extract(bytes);
    }

    // Looks at the first `used` bytes (those that got a payload bit). For each pattern, if more
    // bytes changed than stayed the same, flips their LSBs in stego and sets the flag. A tie keeps the group.
    static int[] invertGroups(byte[] cover, byte[] stego, int used) {
        int[] changed = new int[PATTERNS];
        int[] unchanged = new int[PATTERNS];
        for (int j = 0; j < used; j++) {
            if (cover[j] != stego[j]) {
                changed[pattern(stego[j])]++;
            } else {
                unchanged[pattern(stego[j])]++;
            }
        }

        int[] flags = new int[PATTERNS];
        for (int p = 0; p < PATTERNS; p++) {
            flags[p] = changed[p] > unchanged[p] ? 1 : 0;
        }
        for (int j = 0; j < used; j++) {
            if (flags[pattern(stego[j])] == 1) {
                stego[j] ^= 1;
            }
        }
        return flags;
    }

    // Every byte from index 4 on except red. File order is blue, green, red, so red is i % 3 == 2.
    // The array is two thirds the size of the pixels (about 2.4 MB for 640x480), fine here.
    private static int[] usableIndices(byte[] pixels) {
        return IntStream.range(FLAG_BYTES, pixels.length).filter(i -> i % 3 != 2).toArray();
    }

    private static byte[] gather(byte[] pixels, int[] indices) {
        byte[] bytes = new byte[indices.length];
        for (int j = 0; j < indices.length; j++) {
            bytes[j] = pixels[indices[j]];
        }
        return bytes;
    }

    private static void scatter(byte[] pixels, int[] indices, byte[] bytes) {
        for (int j = 0; j < indices.length; j++) {
            pixels[indices[j]] = bytes[j];
        }
    }

    // Bits 2 and 1 of the byte. LSB1 only touches bit 0, so cover and stego share the pattern.
    private static int pattern(byte b) {
        return (b >> 1) & 3;
    }
}
