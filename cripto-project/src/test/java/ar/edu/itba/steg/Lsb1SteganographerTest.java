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

class Lsb1SteganographerTest {

    private final Steganographer lsb1 = new Lsb1Steganographer();

    @Test
    void embedsA5OverOnes() {
        byte[] pixels = new byte[8];
        Arrays.fill(pixels, (byte) 0xFF);

        lsb1.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0xFF, 0xFE, 0xFF, 0xFE, 0xFE, 0xFF, 0xFE, 0xFF), pixels);
    }

    @Test
    void embedsA5OverZeros() {
        byte[] pixels = new byte[8];

        lsb1.embed(pixels, new byte[] {(byte) 0xA5});

        assertArrayEquals(bytes(0x01, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01), pixels);
    }

    @Test
    void firstPayloadBitGoesInFirstPixelByte() {
        byte[] pixels = new byte[8];

        lsb1.embed(pixels, new byte[] {(byte) 0x80});

        assertArrayEquals(bytes(1, 0, 0, 0, 0, 0, 0, 0), pixels);
    }

    @Test
    void sixByFiveImageHoldsTwelveBytes() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        assertEquals(12, lsb1.capacity(pixels));
    }

    @Test
    void embedRejectsPayloadLargerThanCapacity() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> lsb1.embed(pixels, new byte[13]));

        assertTrue(e.getMessage().contains("12"), e.getMessage());
    }

    @Test
    void randomPayloadRoundTrips() {
        Random random = new Random(7);
        byte[] pixels = new byte[1000];
        random.nextBytes(pixels);
        byte[] payload = new byte[100];
        random.nextBytes(payload);

        lsb1.embed(pixels, payload);
        byte[] extracted = lsb1.extract(pixels);

        assertEquals(125, extracted.length);
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
