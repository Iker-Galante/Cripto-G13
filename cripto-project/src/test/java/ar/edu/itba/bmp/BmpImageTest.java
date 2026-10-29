package ar.edu.itba.bmp;

import ar.edu.itba.StegoException;
import ar.edu.itba.TestBitmaps;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BmpImageTest {

    private static byte[] bmp(int width, int height, int bitsPerPixel, int compression, int trailing) {
        return TestBitmaps.bmp(width, height, bitsPerPixel, compression, trailing, 1);
    }

    @Test
    void pixelBlockIncludesRowPadding() {
        // 5 px * 3 bytes = 15 bytes por fila -> 16 con padding.
        BmpImage image = BmpImage.parse(bmp(5, 3, 24, 0, 0));

        assertEquals(5, image.width());
        assertEquals(3, image.height());
        assertEquals(48, image.pixels().length);
    }

    @Test
    void topDownImagesAreAccepted() {
        assertEquals(4, BmpImage.parse(bmp(4, -4, 24, 0, 0)).height());
    }

    @Test
    void withPixelsKeepsHeaderAndTrailingBytes() {
        byte[] file = bmp(4, 2, 24, 0, 7);
        BmpImage image = BmpImage.parse(file);
        byte[] pixels = new byte[image.pixels().length];

        byte[] result = image.withPixels(pixels);

        assertEquals(file.length, result.length);
        assertArrayEquals(Arrays.copyOf(file, BmpImage.HEADER_SIZE),
                Arrays.copyOf(result, BmpImage.HEADER_SIZE));
        assertArrayEquals(image.trailingBytes(), BmpImage.parse(result).trailingBytes());
        assertArrayEquals(pixels, BmpImage.parse(result).pixels());
    }

    @Test
    void rejectsNonBmp() {
        byte[] file = bmp(4, 4, 24, 0, 0);
        file[0] = 'X';
        assertThrows(StegoException.class, () -> BmpImage.parse(file));
    }

    @Test
    void rejectsOtherBitDepths() {
        assertThrows(StegoException.class, () -> BmpImage.parse(bmp(4, 4, 32, 0, 0)));
    }

    @Test
    void rejectsCompressedImages() {
        assertThrows(StegoException.class, () -> BmpImage.parse(bmp(4, 4, 24, 1, 0)));
    }

    @Test
    void rejectsTruncatedImages() {
        byte[] file = bmp(4, 4, 24, 0, 0);
        byte[] truncated = Arrays.copyOf(file, file.length - 1);
        assertThrows(StegoException.class, () -> BmpImage.parse(truncated));
    }
}
