package ar.edu.itba.steg;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LsbNTest {

    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    @Test
    void capacityUsesOneBitPerCarrierByte() {
        assertEquals(10, new LsbN(1).capacity(87));
    }

    @Test
    void capacityUsesFourBitsPerCarrierByte() {
        assertEquals(43, new LsbN(4).capacity(87));
    }

    @Test
    void lsb1WritesMostSignificantBitFirst() {
        byte[] carrier = new byte[8];
        Arrays.fill(carrier, (byte) 0xF0);

        new LsbN(1).embed(carrier, new byte[]{(byte) 0b1011_0001});

        byte[] expected = {(byte) 0xF1, (byte) 0xF0, (byte) 0xF1, (byte) 0xF1,
                (byte) 0xF0, (byte) 0xF0, (byte) 0xF0, (byte) 0xF1};
        assertArrayEquals(expected, carrier);
    }

    @Test
    void lsb4WritesHighNibbleFirst() {
        byte[] carrier = {(byte) 0xAB, (byte) 0xCD};

        new LsbN(4).embed(carrier, new byte[]{(byte) 0x5E});

        assertArrayEquals(new byte[]{(byte) 0xA5, (byte) 0xCE}, carrier);
    }

    @Test
    void lsb1RoundTrip() {
        assertRoundTrip(new LsbN(1));
    }

    @Test
    void lsb4RoundTrip() {
        assertRoundTrip(new LsbN(4));
    }

    @Test
    void rejectsDataLargerThanCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LsbN(1).embed(new byte[15], new byte[2]));
    }

    private static void assertRoundTrip(StegAlgorithm algorithm) {
        byte[] carrier = randomBytes(1000, 1);
        byte[] data = randomBytes(algorithm.capacity(carrier.length), 2);

        algorithm.embed(carrier, data);

        assertArrayEquals(data, algorithm.extract(carrier));
    }
}
