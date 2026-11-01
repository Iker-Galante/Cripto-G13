package ar.edu.itba.steg;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.bmp.BmpImage;
import ar.edu.itba.bmp.TestBmps;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LsbiSteganographerTest {

    private final Steganographer lsbi = new LsbiSteganographer();

    @Test
    void paperExampleFlipsOnlyPattern10() {
        // Majeed and Sulaiman, section 4: cover 10001100 10101101 10101011 10101101, message 1011.
        byte[] cover = bytes(0x8C, 0xAD, 0xAB, 0xAD);
        byte[] stego = bytes(0x8D, 0xAC, 0xAB, 0xAD);

        int[] flags = LsbiSteganographer.invertGroups(cover, stego, 4);

        assertArrayEquals(new int[] {0, 0, 1, 0}, flags);
        assertArrayEquals(bytes(0x8C, 0xAD, 0xAB, 0xAC), stego);
    }

    @Test
    void tieKeepsTheGroup() {
        byte[] cover = bytes(0x8C, 0xAD);
        byte[] stego = bytes(0x8D, 0xAD);

        int[] flags = LsbiSteganographer.invertGroups(cover, stego, 2);

        assertArrayEquals(new int[] {0, 0, 0, 0}, flags);
        assertArrayEquals(bytes(0x8D, 0xAD), stego);
    }

    @Test
    void flagsGoInFirstFourBytesAndUndoTheFlip() {
        // All bytes are 0 (pattern 00). Hiding 0xFF changes all 8 used bytes, so pattern 00 flips back.
        byte[] pixels = new byte[40];

        lsbi.embed(pixels, bytes(0xFF));

        assertArrayEquals(bytes(1, 0, 0, 0), Arrays.copyOf(pixels, 4));
        assertArrayEquals(new byte[36], Arrays.copyOfRange(pixels, 4, 40));
        assertEquals((byte) 0xFF, lsbi.extract(pixels)[0]);
    }

    @Test
    void redBytesNeverCarryPayload() {
        Random random = new Random(11);
        byte[] cover = new byte[3000];
        random.nextBytes(cover);
        byte[] pixels = cover.clone();
        byte[] payload = new byte[lsbi.capacity(pixels)];
        random.nextBytes(payload);

        lsbi.embed(pixels, payload);

        for (int i = 4; i < pixels.length; i++) {
            if (i % 3 == 2) {
                assertEquals(cover[i], pixels[i], "red byte " + i + " changed");
            }
        }
    }

    @Test
    void capacityCountsBlueAndGreenBytesAfterTheFlags() {
        assertEquals(8, lsbi.capacity(BmpImage.parse(TestBmps.sixByFive()).pixels()));
        assertEquals(76799, lsbi.capacity(new byte[921600]));  // a 640x480 image like lado.bmp
        assertEquals(0, lsbi.capacity(new byte[3]));
    }

    @Test
    void embedRejectsPayloadLargerThanCapacity() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> lsbi.embed(pixels, new byte[9]));

        assertTrue(e.getMessage().contains("maximum capacity with LSBI is 8 bytes"), e.getMessage());
    }

    @Test
    void extractFromTinyArrayReturnsNothing() {
        assertEquals(0, lsbi.extract(new byte[3]).length);
    }

    @Test
    void randomPayloadRoundTrips() {
        Random random = new Random(7);
        byte[] payload = new byte[100];
        random.nextBytes(payload);

        assertRoundTrip(payload, random);
    }

    @Test
    void allOnesPayloadRoundTrips() {
        byte[] payload = new byte[100];
        Arrays.fill(payload, (byte) 0xFF);

        assertRoundTrip(payload, new Random(3));
    }

    private void assertRoundTrip(byte[] payload, Random random) {
        byte[] pixels = new byte[3000];
        random.nextBytes(pixels);

        lsbi.embed(pixels, payload);
        byte[] extracted = lsbi.extract(pixels);

        assertEquals(lsbi.capacity(pixels), extracted.length);
        assertArrayEquals(payload, Arrays.copyOf(extracted, payload.length));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
