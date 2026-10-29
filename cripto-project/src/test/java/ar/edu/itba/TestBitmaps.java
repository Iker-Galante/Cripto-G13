package ar.edu.itba;

import ar.edu.itba.bmp.BmpImage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/** Genera archivos BMP V3 para los tests. */
public final class TestBitmaps {

    private TestBitmaps() {
    }

    /** BMP de 24 bits sin compresión con píxeles aleatorios. */
    public static byte[] random(int width, int height, long seed) {
        return bmp(width, height, 24, 0, 0, seed);
    }

    /** BMP con el encabezado y la cantidad de bytes extra al final indicados, y píxeles aleatorios. */
    public static byte[] bmp(int width, int height, int bitsPerPixel, int compression, int trailing, long seed) {
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
        byte[] file = buffer.array();
        byte[] body = new byte[file.length - BmpImage.HEADER_SIZE];
        new Random(seed).nextBytes(body);
        System.arraycopy(body, 0, file, BmpImage.HEADER_SIZE, body.length);
        return file;
    }
}
