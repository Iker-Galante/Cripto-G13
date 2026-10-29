package ar.edu.itba.bmp;

import ar.edu.itba.StegoException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BmpImageTest {

    /** Arma un BMP V3 con los parámetros dados y los píxeles llenos con un patrón. */
    static byte[] bmp(int width, int height, int bitsPerPixel, int compression, int trailing) {
        int rowSize = (width * 3 + 3) / 4 * 4;
        int pixelLength = rowSize * Math.abs(height);
        ByteBuffer buffer = ByteBuffer.allocate(BmpImage.HEADER_SIZE + pixelLength + trailing)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 'B').put((byte) 'M');
        buffer.putInt(buffer.capacity());
        buffer.putInt(0);
        buffer.putInt(BmpImage.HEADER_SIZE);
        buffer.putInt(40);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.putShort((short) 1);
        buffer.putShort((short) bitsPerPixel);
        buffer.putInt(compression);
        buffer.putInt(pixelLength);
        for (int i = buffer.position(); i < buffer.capacity(); i++) {
            buffer.put(i, (byte) (i * 31));
        }
        return buffer.array();
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
