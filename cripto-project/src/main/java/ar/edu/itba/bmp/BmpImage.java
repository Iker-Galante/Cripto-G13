package ar.edu.itba.bmp;

import java.util.Arrays;

// A 24-bit uncompressed BMP split into header and pixels (row padding included).
public final class BmpImage {

    private static final int MIN_HEADER_SIZE = 54;  // 14-byte file header + 40-byte info header

    private static final int PIXEL_OFFSET_POSITION = 0x0A;
    private static final int WIDTH_POSITION = 0x12;
    private static final int HEIGHT_POSITION = 0x16;
    private static final int BITS_PER_PIXEL_POSITION = 0x1C;
    private static final int COMPRESSION_POSITION = 0x1E;

    private final byte[] header;
    private final byte[] pixels;
    private final int width;
    private final int height;

    private BmpImage(byte[] header, byte[] pixels, int width, int height) {
        this.header = header;
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public static BmpImage parse(byte[] file) {
        if (file.length < MIN_HEADER_SIZE || file[0] != 'B' || file[1] != 'M') {
            throw new BmpException("Not a BMP file");
        }

        int offset = readIntLE(file, PIXEL_OFFSET_POSITION);
        int bitsPerPixel = readShortLE(file, BITS_PER_PIXEL_POSITION);
        int compression = readIntLE(file, COMPRESSION_POSITION);

        if (bitsPerPixel != 24) {
            throw new BmpException("Only 24-bit BMPs are supported, got " + bitsPerPixel);
        }
        if (compression != 0) {
            throw new BmpException("Compressed BMPs are not supported");
        }
        // Read the offset instead of assuming 54: GIMP's V5 export puts pixels at 138.
        if (offset < MIN_HEADER_SIZE || offset > file.length) {
            throw new BmpException("Corrupt header: invalid pixel data offset " + offset);
        }

        // A negative height means the rows are stored top-down; the size is the same.
        return new BmpImage(Arrays.copyOfRange(file, 0, offset),
                            Arrays.copyOfRange(file, offset, file.length),
                            readIntLE(file, WIDTH_POSITION),
                            Math.abs(readIntLE(file, HEIGHT_POSITION)));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    // Bytes after the rows the header declares. pixels() includes them; a normal file has none.
    public byte[] trailingBytes() {
        long imageSize = (width * 3L + 3) / 4 * 4 * height;  // rows padded to a multiple of 4
        if (width <= 0 || imageSize >= pixels.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(pixels, (int) imageSize, pixels.length);
    }

    public byte[] header() {
        return header.clone();
    }

    public byte[] pixels() {
        return pixels.clone();
    }

    public byte[] toBytes() {
        byte[] file = Arrays.copyOf(header, header.length + pixels.length);
        System.arraycopy(pixels, 0, file, header.length, pixels.length);
        return file;
    }

    // Header numbers are little-endian. "& 0xFF" reads each signed byte as 0..255.
    private static int readIntLE(byte[] bytes, int position) {
        return (bytes[position] & 0xFF)
                | (bytes[position + 1] & 0xFF) << 8
                | (bytes[position + 2] & 0xFF) << 16
                | (bytes[position + 3] & 0xFF) << 24;
    }

    private static int readShortLE(byte[] bytes, int position) {
        return (bytes[position] & 0xFF)
                | (bytes[position + 1] & 0xFF) << 8;
    }
}
