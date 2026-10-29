package ar.edu.itba.steg;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LsbiTest {

    private final Lsbi lsbi = new Lsbi();

    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    @Test
    void capacitySkipsInversionMapAndRedBytes() {
        // Bytes útiles desde el 4: 4,6,7,9,10,12,13,15 -> 8 bits.
        assertEquals(1, lsbi.capacity(16));
        assertEquals(0, lsbi.capacity(15));
        assertEquals(0, lsbi.capacity(3));
        // 640x480: (921600 - 307200 - 3) / 8
        assertEquals(76799, lsbi.capacity(921600));
    }

    @Test
    void roundTrip() {
        byte[] carrier = randomBytes(3000, 1);
        byte[] data = randomBytes(lsbi.capacity(carrier.length), 2);

        lsbi.embed(carrier, data);

        assertArrayEquals(data, lsbi.extract(carrier));
    }

    @Test
    void neverTouchesRedBytes() {
        byte[] original = randomBytes(3000, 3);
        byte[] carrier = original.clone();

        lsbi.embed(carrier, randomBytes(lsbi.capacity(carrier.length), 4));

        for (int i = 3; i < carrier.length; i++) {
            if (i % 3 == 2) {
                assertEquals(original[i], carrier[i], "red byte " + i);
            }
        }
    }

    @Test
    void invertsPatternWithMoreChangesAndStoresMap() {
        // 0x04 = 0000 0100: patrón "10" con LSB en 0. Ocultar 0xFF cambia los 8 bytes usados,
        // así que se invierte el patrón "10" y los bytes vuelven a su valor original.
        byte[] carrier = new byte[16];
        Arrays.fill(carrier, (byte) 0x04);

        lsbi.embed(carrier, new byte[]{(byte) 0xFF});

        byte[] expected = new byte[16];
        Arrays.fill(expected, (byte) 0x04);
        expected[2] = 0x05; // flag del patrón "10"
        assertArrayEquals(expected, carrier);
        assertArrayEquals(new byte[]{(byte) 0xFF}, lsbi.extract(carrier));
    }

    @Test
    void changesNoMoreBytesThanPlainLsb() {
        byte[] original = randomBytes(30000, 5);
        byte[] data = randomBytes(lsbi.capacity(original.length), 6);
        byte[] carrier = original.clone();

        lsbi.embed(carrier, data);

        // LSB1 sobre los mismos bytes (sin mapa ni inversión).
        byte[] plain = original.clone();
        int position = 4;
        for (byte value : data) {
            for (int shift = 7; shift >= 0; shift--) {
                if (position % 3 == 2) {
                    position++;
                }
                plain[position] = (byte) ((plain[position] & ~1) | ((value >> shift) & 1));
                position++;
            }
        }
        assertTrue(differences(original, carrier) <= differences(original, plain) + 4);
    }

    private static int differences(byte[] a, byte[] b) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                count++;
            }
        }
        return count;
    }
}
