package ar.edu.itba.bmp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BmpImageTest {

    @Test
    void parseThenToBytesGivesTheSameFile() {
        byte[] file = TestBmps.sixByFive();

        assertArrayEquals(file, BmpImage.parse(file).toBytes());
    }

    @Test
    void parseThenToBytesKeepsALargerHeader() {
        byte[] file = TestBmps.withOffset(138);

        BmpImage image = BmpImage.parse(file);

        assertEquals(138, image.header().length);
        assertArrayEquals(file, image.toBytes());
    }

    @Test
    void sixByFiveSplitsIntoHeaderAndPixels() {
        BmpImage image = BmpImage.parse(TestBmps.sixByFive());

        assertEquals(54, image.header().length);
        assertEquals(100, image.pixels().length);
    }

    @Test
    void pixelsIncludeRowPadding() {
        byte[] pixels = BmpImage.parse(TestBmps.sixByFive()).pixels();

        // Top row (green) is the last 20 bytes: 18 bytes of color, then 2 padding bytes.
        // The last pixel is 00 FF 00 (blue, green, red) at indexes 95..97.
        assertEquals((byte) 0xFF, pixels[96]);
        assertEquals(0, pixels[97]);
        assertEquals(0, pixels[98]);
        assertEquals(0, pixels[99]);
    }

    @Test
    void headerAndPixelsAreCopies() {
        BmpImage image = BmpImage.parse(TestBmps.sixByFive());

        image.pixels()[0] = 42;
        image.header()[0] = 42;

        assertArrayEquals(TestBmps.sixByFive(), image.toBytes());
    }

    @Test
    void writtenFileReadsBackIdentical(@TempDir Path dir) throws IOException {
        byte[] file = TestBmps.sixByFive();
        Path copy = dir.resolve("copy.bmp");

        Files.write(copy, BmpImage.parse(file).toBytes());

        assertArrayEquals(file, Files.readAllBytes(copy));
    }

    @Test
    void rejectsMissingSignature() {
        byte[] file = TestBmps.sixByFive();
        file[0] = 'X';

        assertRejected(file, "Not a BMP file");
    }

    @Test
    void rejectsFileShorterThanHeader() {
        assertRejected(new byte[] {'B', 'M'}, "Not a BMP file");
    }

    @Test
    void rejects32BitsPerPixel() {
        assertRejected(TestBmps.withBitsPerPixel(32), "Only 24-bit BMPs are supported, got 32");
    }

    @Test
    void rejectsCompression() {
        assertRejected(TestBmps.withCompression(3), "Compressed BMPs are not supported");
    }

    @Test
    void rejectsOffsetBeyondFileLength() {
        byte[] file = TestBmps.sixByFive();
        ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).putInt(0x0A, 155);

        assertRejected(file, "Corrupt header: invalid pixel data offset 155");
    }

    private static void assertRejected(byte[] file, String expectedMessage) {
        BmpException e = assertThrows(BmpException.class, () -> BmpImage.parse(file));
        assertEquals(expectedMessage, e.getMessage());
    }
}
