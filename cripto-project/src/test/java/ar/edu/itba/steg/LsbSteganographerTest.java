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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LsbSteganographerTest {

    private final Steganographer lsb1 = new LsbSteganographer(1);
    private final Steganographer lsb4 = new LsbSteganographer(4);

    @Test
    void lsb1EmbedsA5OverOnes() {
        byte[] pixels = filled(8, 0xFF);

        lsb1.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0xFF, 0xFE, 0xFF, 0xFE, 0xFE, 0xFF, 0xFE, 0xFF), pixels);
    }

    @Test
    void lsb1EmbedsA5OverZeros() {
        byte[] pixels = new byte[8];

        lsb1.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0x01, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01), pixels);
    }

    @Test
    void lsb1PutsFirstPayloadBitInFirstPixelByte() {
        byte[] pixels = new byte[8];

        lsb1.embed(pixels, new byte[] {(byte) 0x80});

        assertArrayEquals(bytes(1, 0, 0, 0, 0, 0, 0, 0), pixels);
    }

    @Test
    void lsb4EmbedsA5OverOnes() {
        byte[] pixels = filled(2, 0xFF);

        lsb4.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0xFA, 0xF5), pixels);
    }

    @Test
    void lsb4EmbedsA5OverZeros() {
        byte[] pixels = new byte[2];

        lsb4.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0x0A, 0x05), pixels);
    }

    @Test
    void lsb4PutsHighNibbleFirst() {
        byte[] pixels = new byte[2];

        lsb4.embed(pixels, new byte[] {(byte) 0x80});

        assertArrayEquals(bytes(0x08, 0x00), pixels);
    }

    @Test
    void sixByFiveImageHoldsTwelveBytesWithLsb1And50WithLsb4() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        assertEquals(12, lsb1.capacity(pixels));
        assertEquals(50, lsb4.capacity(pixels));
    }

    @Test
    void lsb1RejectsPayloadLargerThanCapacity() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> lsb1.embed(pixels, new byte[13]));

        assertTrue(e.getMessage().contains("maximum capacity with LSB1 is 12 bytes"), e.getMessage());
    }

    @Test
    void lsb4RejectsPayloadLargerThanCapacity() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> lsb4.embed(pixels, new byte[51]));

        assertTrue(e.getMessage().contains("maximum capacity with LSB4 is 50 bytes"), e.getMessage());
    }

    @Test
    void lsb1RandomPayloadRoundTrips() {
        assertRandomRoundTrip(lsb1, 125);
    }

    @Test
    void lsb4RandomPayloadRoundTrips() {
        assertRandomRoundTrip(lsb4, 500);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3, 8})
    void rejectsUnsupportedBitsPerByte(int bitsPerByte) {
        assertThrows(IllegalArgumentException.class, () -> new LsbSteganographer(bitsPerByte));
    }

    // 1000 random pixel bytes carrying a 100-byte random payload.
    private static void assertRandomRoundTrip(Steganographer steganographer, int expectedCapacity) {
        Random random = new Random(7);
        byte[] pixels = new byte[1000];
        random.nextBytes(pixels);
        byte[] payload = new byte[100];
        random.nextBytes(payload);

        steganographer.embed(pixels, payload);
        byte[] extracted = steganographer.extract(pixels);

        assertEquals(expectedCapacity, extracted.length);
        assertArrayEquals(payload, Arrays.copyOf(extracted, payload.length));
    }

    private static byte[] filled(int length, int value) {
        byte[] result = new byte[length];
        Arrays.fill(result, (byte) value);
        return result;
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
