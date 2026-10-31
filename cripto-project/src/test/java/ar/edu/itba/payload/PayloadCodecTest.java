package ar.edu.itba.payload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PayloadCodecTest {

    @Test
    void roundTripsEmptyData() {
        assertRoundTrip(new byte[0], ".txt");
    }

    @Test
    void roundTripsShortText() {
        assertRoundTrip("hola mundo".getBytes(StandardCharsets.US_ASCII), ".txt");
    }

    @Test
    void roundTripsRandomBinaryData() {
        byte[] data = new byte[1000];
        new Random(42).nextBytes(data);

        assertRoundTrip(data, ".bin");
    }

    @Test
    void roundTripsShortestExtension() {
        // Block is exactly 4 + size + 2, the largest size decode accepts.
        assertRoundTrip(new byte[] {1, 2, 3}, ".");
    }

    @Test
    void encodesTheAssignmentExample() {
        // saco.txt from the assignment: 4 + 414 + 5 = 423 bytes.
        byte[] block = PayloadCodec.encode(new byte[414], ".txt");

        assertEquals(423, block.length);
        assertArrayEquals(new byte[] {0x00, 0x00, 0x01, (byte) 0x9E}, Arrays.copyOf(block, 4));
        assertArrayEquals(".txt\0".getBytes(StandardCharsets.US_ASCII), Arrays.copyOfRange(block, 418, 423));
    }

    @Test
    void decodeIgnoresBytesAfterTheNul() {
        byte[] block = PayloadCodec.encode(new byte[] {1, 2, 3}, ".bin");
        byte[] withTrailingBytes = Arrays.copyOf(block, block.length + 20);
        Arrays.fill(withTrailingBytes, block.length, withTrailingBytes.length, (byte) 0x7F);

        Decoded decoded = PayloadCodec.decode(withTrailingBytes);

        assertArrayEquals(new byte[] {1, 2, 3}, decoded.data());
        assertEquals(".bin", decoded.extension());
    }

    @Test
    void decodeRejectsBlockShorterThanSizeField() {
        assertRejected(new byte[] {0, 0, 1}, "Payload too short: 3 bytes");
    }

    @Test
    void decodeRejectsNegativeSize() {
        byte[] block = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, '.', 't', 0};

        assertRejected(block, "Invalid payload size -1 for a block of 7 bytes");
    }

    @Test
    void decodeRejectsSizeLargerThanBlock() {
        byte[] block = {0, 0, 0, 10, 'a', 'b', '.', 't', 0};

        assertRejected(block, "Invalid payload size 10 for a block of 9 bytes");
    }

    @Test
    void decodeRejectsMissingNul() {
        byte[] block = {0, 0, 0, 1, 'a', '.', 't', 'x', 't'};

        assertRejected(block, "Payload extension has no terminating NUL byte");
    }

    @Test
    void decodeRejectsExtensionWithoutDot() {
        byte[] block = {0, 0, 0, 1, 'a', 't', 'x', 't', 0};

        assertRejected(block, "Payload extension does not start with '.'");
    }

    @Test
    void encodeRejectsExtensionWithoutDot() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PayloadCodec.encode(new byte[] {1}, "txt"));

        assertEquals("Extension must start with '.', got \"txt\"", e.getMessage());
    }

    @Test
    void encodeRejectsEmptyExtension() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.encode(new byte[] {1}, ""));
    }

    @Test
    void encodeRejectsNonAsciiExtension() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PayloadCodec.encode(new byte[] {1}, ".jpé"));

        assertEquals("Extension must be ASCII without NUL, got \".jpé\"", e.getMessage());
    }

    @Test
    void encodeRejectsNulInExtension() {
        assertThrows(IllegalArgumentException.class, () -> PayloadCodec.encode(new byte[] {1}, ".t\0t"));
    }

    private static void assertRoundTrip(byte[] data, String extension) {
        Decoded decoded = PayloadCodec.decode(PayloadCodec.encode(data, extension));

        assertArrayEquals(data, decoded.data());
        assertEquals(extension, decoded.extension());
    }

    private static void assertRejected(byte[] block, String expectedMessage) {
        PayloadException e = assertThrows(PayloadException.class, () -> PayloadCodec.decode(block));
        assertEquals(expectedMessage, e.getMessage());
    }
}
