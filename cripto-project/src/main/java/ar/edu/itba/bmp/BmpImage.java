package ar.edu.itba.bmp;

import ar.edu.itba.StegoException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * BMP (V3) de 24 bits sin compresión. Expone el bloque de píxeles completo, incluyendo el padding
 * de cada fila, que es lo que se usa como portador.
 */
public final class BmpImage {

    public static final int HEADER_SIZE = 54;

    private static final int OFFSET_PIXELS = 10;
    private static final int OFFSET_WIDTH = 18;
    private static final int OFFSET_HEIGHT = 22;
    private static final int OFFSET_BITS_PER_PIXEL = 28;
    private static final int OFFSET_COMPRESSION = 30;
    private static final int BITS_PER_PIXEL = 24;
    private static final int NO_COMPRESSION = 0;

    private final byte[] file;
    private final int width;
    private final int height;
    private final int pixelOffset;
    private final int pixelLength;

    private BmpImage(byte[] file, int width, int height, int pixelOffset, int pixelLength) {
        this.file = file;
        this.width = width;
        this.height = height;
        this.pixelOffset = pixelOffset;
        this.pixelLength = pixelLength;
    }

    public static BmpImage parse(byte[] file) {
        if (file.length < HEADER_SIZE || file[0] != 'B' || file[1] != 'M') {
            throw new StegoException("no es un archivo BMP válido");
        }
        ByteBuffer header = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        int bitsPerPixel = Short.toUnsignedInt(header.getShort(OFFSET_BITS_PER_PIXEL));
        if (bitsPerPixel != BITS_PER_PIXEL) {
            throw new StegoException("el BMP debe ser de 24 bits por pixel (tiene " + bitsPerPixel + ")");
        }
        int compression = header.getInt(OFFSET_COMPRESSION);
        if (compression != NO_COMPRESSION) {
            throw new StegoException("el BMP no debe tener compresión (tipo de compresión " + compression + ")");
        }
        int width = header.getInt(OFFSET_WIDTH);
        int height = Math.abs(header.getInt(OFFSET_HEIGHT));
        if (width <= 0 || height == 0) {
            throw new StegoException("dimensiones inválidas en el BMP (" + width + "x" + height + ")");
        }
        long pixelOffset = Integer.toUnsignedLong(header.getInt(OFFSET_PIXELS));
        if (pixelOffset < HEADER_SIZE || pixelOffset > file.length) {
            throw new StegoException("offset de píxeles inválido en el BMP (" + pixelOffset + ")");
        }
        // Cada fila se completa con padding hasta un múltiplo de 4 bytes.
        long rowSize = ((long) width * 3 + 3) / 4 * 4;
        long pixelLength = rowSize * height;
        if (pixelOffset + pixelLength > file.length) {
            throw new StegoException("el BMP está truncado: faltan datos de píxeles");
        }
        return new BmpImage(file, width, height, (int) pixelOffset, (int) pixelLength);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Copia del bloque de píxeles (con el padding de las filas). */
    public byte[] pixels() {
        return Arrays.copyOfRange(file, pixelOffset, pixelOffset + pixelLength);
    }

    /** Bytes que aparecen en el archivo después del bloque de píxeles. */
    public byte[] trailingBytes() {
        return Arrays.copyOfRange(file, pixelOffset + pixelLength, file.length);
    }

    /** Devuelve el archivo completo con el bloque de píxeles reemplazado; el resto queda intacto. */
    public byte[] withPixels(byte[] pixels) {
        if (pixels.length != pixelLength) {
            throw new IllegalArgumentException("pixel block must be " + pixelLength + " bytes, got " + pixels.length);
        }
        byte[] result = file.clone();
        System.arraycopy(pixels, 0, result, pixelOffset, pixelLength);
        return result;
    }
}
